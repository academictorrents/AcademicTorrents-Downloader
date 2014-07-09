package p2pp;
import java.io.IOException;
import java.util.logging.Level;

import org.klomp.snark.CoordinatorListener;
import org.klomp.snark.MetaInfo;
import org.klomp.snark.PeerCoordinator;
import org.klomp.snark.Storage;



public class LiveBTPeerCoordinatorFactory implements PeerCoordinatorFactory {
	
	public static void main(String[] args) throws InterruptedException {
		// This torrent can be used for testing (50 mb).
		//String torrent = "trusted-computing.torrent";
		String torrent = "eclipse.torrent";
		
		// The estimated bitrate in kiloBits/second.
		int bitrate = 1911000;
		
		//String serverUrl = "http://ge.tt/6aYJ5/";
		//String serverUrl = "http://ge.tt/#554b7LQ";
		
		//boolean useName = true;
		
		Thread livebt = new Thread(new Driver(Level.WARNING, torrent,
			new LiveBTPeerCoordinatorFactory(bitrate)));
		livebt.start();
		
		livebt.join();
	}
	
	private int bitrate;
	
	// The one instance of the coordinator.
	// This is not necessary, as the method getPeerCoordinator only
	// is called once from within the Snark class.
	private PeerCoordinator coord;
	
	public LiveBTPeerCoordinatorFactory(int bitrate) {
		this.bitrate = bitrate;
	}

	@Override
	public PeerCoordinator getPeerCoordinator(byte[] id, MetaInfo metainfo,
			Storage storage, CoordinatorListener listener) {
		try {
			// Create new coordinator.
			if(coord == null) {
				coord = new LiveBTPeerCoordinator(id, metainfo, 
						bitrate, storage, listener);
				
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
						new FileDownloadProgressListener("livebt"),
						new LogDownloadProgressListener(pieces),
						new GUIDownloadProgressListener(pieces, rate, wait)));
			}
		} 
		catch(IOException e) {
			e.printStackTrace();
		}
		
		return coord;
	}

}
