package p2pp;

import java.io.IOException;
import java.net.MalformedURLException;

import org.klomp.snark.CoordinatorListener;
import org.klomp.snark.MetaInfo;
import org.klomp.snark.PeerCoordinator;
import org.klomp.snark.Storage;

public class BassPeerCoordinatorFactory implements PeerCoordinatorFactory {

	private String serverUrl;
	private int bitrate;
	private boolean useName;
	
	// The one instance of the coordinator.
	// This is not necessary, as the method getPeerCoordinator only
	// is called once from within the Snark class.
	private PeerCoordinator coord;
	
	public BassPeerCoordinatorFactory(int bitrate, String serverUrl, boolean useName) {
		this.bitrate = bitrate;
		this.serverUrl = serverUrl;
		this.useName = useName;
	}

	@Override
	public PeerCoordinator getPeerCoordinator(byte[] id, MetaInfo metainfo,
			Storage storage, CoordinatorListener listener) {
		try {
			// Create new coordinator.
			if(coord == null) {
				coord = new BassPeerCoordinator(id, metainfo, bitrate, 
						storage, listener, serverUrl, useName);
				
				// Number of pieces.
				int pieces = metainfo.getPieces();
				
				// The bitrate in pieces per second.
				// Needed by the GUIDownloadProgressListener to animate
				// the playback of the file.
				double rate = (((double) bitrate) / 8.0) / 
					((double) metainfo.getPieceLength(0));
				
				// Number of seconds to wait before the 
				// GUIDownloadProgressListener starts to 'play' the file.
				int wait = 20;
				
				// Create a composite progress listener consisting of
				// the GUI and Log listeners.
				coord.setDownloadProgressListener(
					new CompositeDownloadProgressListener(
						new FileDownloadProgressListener("bass"),
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
