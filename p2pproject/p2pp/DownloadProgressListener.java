/*
 * Used to monitor download progress of pieces. Use 
 * PeerCoordinator.setDownloadProgressListener to set a progress listener.
 */

package p2pp;

import org.klomp.snark.Peer;
import org.klomp.snark.PeerCoordinator;

public interface DownloadProgressListener {
	
	// Called when a valid piece has been downloaded.
	// Peer is null if downloaded from server.
	void pieceDownloaded(Peer peer, int piece);
	
	// Called when all pieces have been downloaded.
	void downloadComplete();
	
	// Called when a piece has been requested. 
	// Peer is null if requested from server.
	void pieceRequested(Peer peer, int piece);
	
	// This method is called within the PeerCoordinator to set itself as
	// event notifier.
	void setPeerCoordinator(PeerCoordinator coord);
}
