package p2pp;

import org.klomp.snark.Peer;
import org.klomp.snark.PeerCoordinator;

public class CompositeDownloadProgressListener implements
		DownloadProgressListener {
	
	DownloadProgressListener[] listeners;
	
	public CompositeDownloadProgressListener(DownloadProgressListener... listeners) {
		this.listeners = listeners;
	}

	@Override
	public void pieceDownloaded(Peer peer, int piece) {
		for(DownloadProgressListener l : listeners)
			l.pieceDownloaded(peer, piece);
	}

	@Override
	public void downloadComplete() {
		for(DownloadProgressListener l : listeners)
			l.downloadComplete();
	}

	@Override
	public void pieceRequested(Peer peer, int piece) {
		for(DownloadProgressListener l : listeners)
			l.pieceRequested(peer, piece);
	}

	@Override
	public void setPeerCoordinator(PeerCoordinator coord) {
		for(DownloadProgressListener l : listeners)
			l.setPeerCoordinator(coord);
	}

}
