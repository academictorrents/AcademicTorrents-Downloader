package p2pp;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.klomp.snark.Peer;
import org.klomp.snark.PeerCoordinator;

public class LogDownloadProgressListener implements DownloadProgressListener {
	
	private PeerCoordinator coordinator;
	
	private int nbOfPieces;
	private int piecesDownloaded = 0;
	
	private int downloadedServer = 0;
	
	private long started = System.currentTimeMillis();
	
	protected Logger log = Logger.getLogger("org.klomp.snark.peer");
	
	public LogDownloadProgressListener(int pieces) {
		this.nbOfPieces = pieces;
	}

	@Override
	public void pieceDownloaded(Peer peer, int piece) {
		String from = peer == null ? WebSeed.description : peer.toString();
		if(peer == null)
			downloadedServer++;
		
		piecesDownloaded++;
		double progress = ((double) piecesDownloaded) / ((double) nbOfPieces);
		log.log(Level.INFO, "Piece " + piece +  " downloaded from " + from +
				". Progress so far " + (progress * 100) + "%");
	}

	@Override
	public void downloadComplete() {
		coordinator.halt();
		long duration = System.currentTimeMillis() - started;
		
		double server = ((double) downloadedServer) / ((double) nbOfPieces);
		double peers = 1.0 - server;
		
		log.log(Level.INFO, "Download completed. Duration " + (duration / 1000) + " seconds.\n" +
				"Server = " + (server * 100) + "% - Peers = " + (peers * 100) + "%");
	}

	@Override
	public void setPeerCoordinator(PeerCoordinator coord) {
		this.coordinator = coord;
	}

	@Override
	public void pieceRequested(Peer peer, int piece) {
		String from = peer == null ? WebSeed.description : peer.toString();
		log.log(Level.INFO, "Piece " + piece + " requested from " + from);
	}

}
