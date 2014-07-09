package p2pp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.klomp.snark.BitField;
import org.klomp.snark.CoordinatorListener;
import org.klomp.snark.MetaInfo;
import org.klomp.snark.Peer;
import org.klomp.snark.PeerCoordinator;
import org.klomp.snark.Storage;
import org.klomp.snark.TrackerClient;

public class VoDBTPeerCoordinator extends PeerCoordinator {
	private final int DELAY = 30;
	private final int w2 = 100;
	
	private int w1;
	private ArrayList<Integer> inOrderWindow = new ArrayList<Integer>();
	private int positionInOrder = 0;
	private int positionRarestFirst;
	private ArrayList<Integer> requestedPieces = new ArrayList<Integer>();
	
	private WebSeed webSeed;
	
	//Constructor
	public VoDBTPeerCoordinator(byte[] id, MetaInfo metainfo, int bitrate, 
			Storage storage, CoordinatorListener listener, 
			String serverUrl, boolean useName) throws MalformedURLException {		
		//Call super constructor
		super(id, metainfo, storage, listener);
		
		this.w1 = (DELAY * bitrate) / (8 * metainfo.getPieceLength(0));
		System.out.println("In order window length " + w1);
		this.positionRarestFirst = positionInOrder + w1;
		updateInOrderWindow(positionInOrder);
		this.webSeed = new WebSeed(serverUrl, metainfo, useName);
	}
	
	@Override
	public boolean gotPiece(Peer peer, int piece, byte[] data) 
			throws IOException {
		synchronized(wantedPieces) {
			boolean valid = super.gotPiece(peer, piece, data);
		
			if(valid) {
				inOrderWindow.remove(new Integer(piece));
			
				if(piece == positionRarestFirst)
					positionRarestFirst++;
			}
			
			return valid;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public int wantPiece(Peer peer, BitField have) {
		synchronized(wantedPieces) {
			if(inOrderWindow.isEmpty()) {
				for(int piece : wantedPieces)
					if(have.get(piece) && positionRarestFirst <= piece && 
							piece < positionRarestFirst + w2) {
						if(downloadProgress != null)
							downloadProgress.pieceRequested(peer, piece);
							
						return piece;
					}
			} 
			else {
				for(int piece : inOrderWindow) {
					if(have.get(piece) && !requestedPieces.contains(piece)) {
						requestedPieces.add(piece);
						if(downloadProgress != null)
							downloadProgress.pieceRequested(peer, piece);
						
						if(requestedPieces.containsAll(inOrderWindow)) {
							Thread server = new Thread(
								serverDownload((List<Integer>) 
									inOrderWindow.clone(), positionInOrder));
							
							inOrderWindow.clear();
							requestedPieces.clear();
							
							server.start();
						}
							
							
						return piece;
					} 
				}
			}
			
			return -1;
		}
	}
	
	@Override
	public void setTracker(TrackerClient tracker) {
		super.setTracker(tracker);
		
		Thread order = new Thread(new Runnable() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				while(!VoDBTPeerCoordinator.this.completed()) {
					try {
						Thread.sleep(DELAY * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					synchronized(wantedPieces) {
						requestedPieces.clear();
						
						if(!inOrderWindow.isEmpty()) {
							Thread server = new Thread(
									serverDownload((List<Integer>) 
										inOrderWindow.clone(), positionInOrder));
								
							server.start();
						}
						
						updateWindows();
					}
				}
			}
		});
		
		order.start();
	}
	
	private void updateInOrderWindow(int start) {
		synchronized(wantedPieces) {
			inOrderWindow.clear();
			for(int piece : wantedPieces) { 
				if(start <= piece && piece < start + w1)
					inOrderWindow.add(piece);
			}
			
			Collections.sort(inOrderWindow);
		}
	}
	
	private void updateWindows() {
		int calc = w1 - (positionRarestFirst - (positionInOrder + w1));
		if(calc > 0) 
			positionRarestFirst += calc;
		positionInOrder += w1;
		updateInOrderWindow(positionInOrder);
	}
	
	/**
	 * Starts downloading pieces from the server. The pieces are downloaded
	 * in the order they apear in the list.
	 * @param pieces List of pieces to download.
	 * @param window Which window position. Used for logging.
	 * @return A runnable object whos run method starts the download
	 * of pieces from server.
	 */
	private Runnable serverDownload(final List<Integer> pieces, 
			final int window) {
		return new Runnable() {
			@Override
			public void run() {
				log.log(Level.INFO, "Server download session started [window=" + 
						window + "]");
				
				Iterator<Integer> it = pieces.iterator();
				while(it.hasNext()) {
					int piece = it.next();
					
					// Check if we still need this piece.
					boolean stillWanted = wantedPieces.contains(piece);
					
					if(stillWanted) {
						// Download data from server.
						byte[] data = getServerPiece(piece);
						
						// If data is null error occurred. What to do? Panic?
						if(data != null)
							try {
								// if gotPiece returns false the downloaded
								// piece does not correspond to the hash in
								// the torrent. This should not happen!
								if(!gotPiece(null, piece, data))
									log.log(Level.SEVERE, "Downloaded bad piece " 
										+ piece + " from web seed " + webSeed);
							} catch (IOException e) {
								// Ignore. Thrown when trying to abort Snark
								// if an error during writing to disk occurred.
							} 
						else
							log.log(Level.SEVERE, "Data is null. " +
									"Bad downloaded bad block form server!");
								
					}
				}
				
				log.log(Level.INFO, "Server download session completed [window=" 
						+ window + "]");
			}
		};
	}
	
	/**
	 * Retrieves a whole piece from the server.
	 * @param piece Piece to download.
	 * @return Returns the raw byte of that piece.
	 */
	private byte[] getServerPiece(int piece) {
		MetaInfo info = getMetaInfo();
		
		// Calculate start index.
		long index = piece * info.getPieceLength(0);
		
		try {
			// Notify the download listener that we are requesting
			// a piece from the server.
			synchronized(downloadProgress) {
				if(this.downloadProgress != null)
					downloadProgress.pieceRequested(null, piece);
			}
				
			// Return the bytes.
			return webSeed.getBlock(index, info.getPieceLength(piece));
		} 
		catch (IOException e) {
			log.log(Level.SEVERE, "Error while reading block from server.", e);
		}
		
		// Return null if not able to download block.
		return null;
	}
}
