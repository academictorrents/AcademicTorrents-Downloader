/*
 * PeerCoordinator which uses BASS strategy to download pieces. The file is 
 * split up in windows (each consising of a number of pieces depending on ALFA
 * and the bitrate), the pieces from the current window are downloaded 
 * from the server.
 */

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

public class BassPeerCoordinator extends PeerCoordinator {

	// Window in seconds.
	private final int ALFA = 20;
	
	// Window in pieces.
	private int windowSize;
	
	// Current piece position of window.
	private int windowPosition = 0;
	
	// Missing pieces in the current window, which are being
	// downloaded from the server.
	private ArrayList<Integer> missingWindowPieces;
	
	// Current requested pieces.
	private List<Integer> requestedPieces = new ArrayList<Integer>();
	
	private WebSeed webSeed;

	/**
	 * Creates new BASS coordinator.
	 * @param bitrate The bitrate of the media file in bits per second (bit/s).
	 * @param serverUrl The full to the media server. E.g. 'http://ge.tt/#6aYJ5'.
	 * @param useName True if the web seed should use the name attribute from
	 * the torrent file to construct the url.
	 * @throws MalformedURLException If the url is invalid.
	 */
	public BassPeerCoordinator(byte[] id, MetaInfo metainfo, int bitrate, 
			Storage storage, CoordinatorListener listener, 
			String serverUrl, boolean useName) throws MalformedURLException {
		// Call the super constructor.
		super(id, metainfo, storage, listener);

		// Create a new WebSeed object. If the server does not support
		// range requests throw exception.
		this.webSeed = new WebSeed(serverUrl, metainfo, useName);
		if(!webSeed.supportsRange())
			throw new UnsupportedOperationException("The server does not support range requests!");
		
		this.windowSize = ((bitrate * ALFA) >> 3) / metainfo.getPieceLength(0) + 1;
		missingPieces(windowPosition, windowSize);
		
		log.log(Level.INFO, "BASS started with window size " + windowSize);

		// Sort the wantedPieces list.
		Collections.sort(wantedPieces);
	}
	
	/**
	 * Called when a piece has been finished downloading. Return false is the
	 * piece is not valid according to the hash in the torrent.
	 * @param peer The peer which uploaded us the piece.
	 * @param piece The downloaded piece.
	 * @param data The raw piece data.
	 * @return Returns if the piece is valid.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean gotPiece(Peer peer, int piece, byte[] data) 
			throws IOException {
		// Always call the super.gotPiece method. It stores the piece
		// and checks if it's valid.
		boolean valid = super.gotPiece(peer, piece, data);
		
		// 
		synchronized(missingWindowPieces) {
			// If the server uploaded the piece it will be present in the
			// missing window list. Remove it from the list.
			missingWindowPieces.remove(new Integer(piece));
			
			// Completed download of a window. Prepare the next window and
			// start downloading from the server.
			if(missingWindowPieces.isEmpty()) {
				do {
					windowPosition += windowSize;
					missingPieces(windowPosition, windowSize);
				} while(missingWindowPieces.isEmpty() && !wantedPieces.isEmpty());
				
				// Start downloading of pieces from server.
				Thread webDownload = new Thread(serverDownload(
					(List<Integer>) missingWindowPieces.clone(), windowPosition));
				webDownload.start();
			}
		}

		return valid;
	}
	
	/**
	 * Override the default wantPiece strategy (the default is to use random 
	 * piece selection). The method is called when a peer is willing to upload
	 * to us and we haven't started a download from that peer.
	 * @param peer The ready peer.
	 * @param have A bitfield with all the pieces the peers has.
	 * @return Returns the wanted piece or -1 if we don't want a piece from
	 * this peer.
	 */
	@Override
	public int wantPiece(Peer peer, BitField have) {
		// Only request pieces which are in the newt window and beyond.
		// We are already downloading the pieces from this window from the server.
		int fromPiece = windowPosition + windowSize;

		synchronized(wantedPieces) {
			// Prioritize closest pieces.
			for(int i = fromPiece; i < getMetaInfo().getPieces(); i++)
				// If the peer has the piece and the piece is not already
				// requested we request the piece.
				if(have.get(i) && !requestedPieces.contains(i)) {
					requestedPieces.add(i);
					
					// Move the wanted piece to end of the list.
					wantedPieces.remove(new Integer(i));
					wantedPieces.add(i);
					
					// Notify the download listener that piece is requsted.
					synchronized(downloadProgress) {
						if(this.downloadProgress != null)
							downloadProgress.pieceRequested(peer, i);
					}
					
					return i;
				}
			
			// Return -1 if we the peers doesn't have any interesting pieces.
			return -1;
		}
	}
	
	/**
	 * Sets tracker to be used for peer list updated.
	 * Starts the server download of the first window.
	 */
	@Override
	public void setTracker(TrackerClient client) {
		super.setTracker(client);
		
		@SuppressWarnings("unchecked")
		Thread webDownload = new Thread(serverDownload(
				(List<Integer>) missingWindowPieces.clone(), windowPosition));
		webDownload.start();
	}
	
	/**
	 * Starts downloading pieces from the server. The pieces are downloaded
	 * in the order they apear in the list.
	 * @param pieces List of pieces to download.
	 * @param window Which window position. Used for logging.
	 * @return A runnable object whos run method starts the download
	 * of pieces from server.
	 */
	private Runnable serverDownload(final List<Integer> pieces, final int window) {
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
									log.log(Level.SEVERE, "Downloaded bad piece " +
											piece + " from web seed " + webSeed);
							} catch (IOException e) {
								// Ignore. Thrown when trying to abort Snark
								// if an error during writing to disk occurred.
							} 
					}
				}
				
				log.log(Level.INFO, "Server download session completed [window=" + 
						window + "]");
			}
		};
	}
	
	/**
	 * Finds the missing pieces in window. It simply runs through wantedPieces
	 * and when ever it runs into a piece s.t. pos <= piece < pos + size, it 
	 * puts that piece in the missing window list.
	 * @param pos The window position.
	 * @param size The window size.
	 */
	private void missingPieces(int pos, int size) {
		if(missingWindowPieces == null)
			missingWindowPieces = new ArrayList<Integer>();
		else
			missingWindowPieces.clear();
		
		synchronized(wantedPieces) {
			Iterator<Integer> it = wantedPieces.iterator();
			while(it.hasNext()) {
				int piece = it.next();
				if(pos <= piece && piece < pos + size) {
					missingWindowPieces.add(piece);
				}
			}
		}
		
		Collections.sort(missingWindowPieces);
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
