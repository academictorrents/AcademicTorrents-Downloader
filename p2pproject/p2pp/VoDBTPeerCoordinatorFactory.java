package p2pp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Level;

import org.klomp.snark.CoordinatorListener;
import org.klomp.snark.MetaInfo;
import org.klomp.snark.PeerCoordinator;
import org.klomp.snark.Storage;

public class VoDBTPeerCoordinatorFactory implements PeerCoordinatorFactory {

	public static void main(String[] args) throws InterruptedException {
		// This torrent can be used for testing (50 mb).
		//String torrent = "trusted-computing.torrent";
		String torrent = "eclipse.torrent";
		
		// The estimated bitrate in kiloBits/second.
		int bitrate = 1911000;
		
		String serverUrl = "http://daimi.au.dk/~bubbi/p2p";
		//String serverUrl = "http://ge.tt/#554b7LQ";
		
		boolean useName = true;
		
		Thread vod = new Thread(new Driver(Level.INFO, torrent,
			new VoDBTPeerCoordinatorFactory(bitrate, serverUrl, useName)));
		vod.start();
		
		vod.join();
	}
	
	private int bitrate;
	private String url;
	private boolean useName;
	
	// The one instance of the coordinator.
	// This is not necessary, as the method getPeerCoordinator only
	// is called once from within the Snark class.
	private PeerCoordinator coord;
	
	public VoDBTPeerCoordinatorFactory(int bitrate, String url, boolean useName) {
		this.bitrate = bitrate;
		this.url = url;
		this.useName = useName;
	}

	@Override
	public PeerCoordinator getPeerCoordinator(byte[] id, MetaInfo metainfo,
			Storage storage, CoordinatorListener listener) {
		try {
			// Create new coordinator.
			if(coord == null) {
				coord = new VoDBTPeerCoordinator(id, metainfo, bitrate, 
						storage, listener, url, useName);
				
				// Number of pieces.
				int pieces = metainfo.getPieces();
				
				// The bitrate in pieces per second.
				// Needed by the GUIDownloadProgressListener to animate
				// the playback of the file.
				double rate = (((double) bitrate) / 8.0) / 
					((double) metainfo.getPieceLength(0));
				
				// Number of seconds to wait before the 
				// GUIDownloadProgressListener starts to 'play' the file.
				int wait = 60;
				
				// Create a composite progress listener consisting of
				// the GUI and Log listeners.
				coord.setDownloadProgressListener(
					new CompositeDownloadProgressListener(
						new FileDownloadProgressListener("VoDBT"),
						new LogDownloadProgressListener(pieces),
						new GUIDownloadProgressListener(pieces, rate, wait)));
			}
		} 
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		return coord;
	}


}
