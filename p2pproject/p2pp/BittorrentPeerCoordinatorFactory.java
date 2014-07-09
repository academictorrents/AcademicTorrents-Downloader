package p2pp;

import java.io.IOException;

import org.klomp.snark.CoordinatorListener;
import org.klomp.snark.MetaInfo;
import org.klomp.snark.PeerCoordinator;
import org.klomp.snark.Storage;

public class BittorrentPeerCoordinatorFactory implements PeerCoordinatorFactory {
	
	private PeerCoordinator coord;
	private int bitrate;
	
	public BittorrentPeerCoordinatorFactory(int bitrate) {
		this.bitrate = bitrate;
	}

	@Override
	public PeerCoordinator getPeerCoordinator(byte[] id, MetaInfo metainfo,
			Storage storage, CoordinatorListener listener) {
		try {
			if(coord == null) {
				coord = new PeerCoordinator(id, metainfo, storage, listener);
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
						new FileDownloadProgressListener("bittorrent"),
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
