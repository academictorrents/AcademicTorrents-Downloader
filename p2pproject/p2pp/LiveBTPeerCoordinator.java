package p2pp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.klomp.snark.BitField;
import org.klomp.snark.CoordinatorListener;
import org.klomp.snark.MetaInfo;
import org.klomp.snark.Peer;
import org.klomp.snark.PeerCoordinator;
import org.klomp.snark.Storage;
import org.klomp.snark.TrackerClient;

import p2pp.LiveBTPeer.LiveBTPeerOrganizer;

public class LiveBTPeerCoordinator extends PeerCoordinator {
	
	// Every MBDF_SLEEP milliseconds find a new piece to
	// request by using the MBDF rules.
	private static final int MBDF_SLEEP = 1000;
	
	// Every RESCHEDULE_SLEEP milliseconds reorder the piece
	// request queue if needed.
	private static final int RESCHEDULE_SLEEP = 40000;
	
	// Every DOWNLOAD_QUEUE_SLEEP milliseconds tune the download
	// queue (update download queue capacities).
	private static final int TUNE_QUEUE_SLEEP = RESCHEDULE_SLEEP;
	private static final int TUNE_QUEUE_INIT_DELAY = TUNE_QUEUE_SLEEP / 2;
	
	public static final int NO_PIECE = -1;
	private static final int DOWNLOAD_TIMEOUT = 10000;
	
	private static final int NUMBER_OF_MOST_WANTED_BLOCKS = 32;
	private static final int THETA = 32;
	private static final double P = 0.8;
	
	private final Random RAND = new Random();
	private final Timer TASK_TIMER = new Timer(true);
	
	private LiveBTTable table;
	private LiveBTPeerOrganizer organizer;
	private Map<Integer, Long> downloadingPieces = new HashMap<Integer, Long>();

	public LiveBTPeerCoordinator(byte[] id, MetaInfo metainfo, 
			int bitrate, Storage storage, CoordinatorListener listener) {
		super(id, metainfo, storage, listener);
		
		table = new LiveBTTable(metainfo.getPieceLength(0), bitrate);
		organizer = new LiveBTPeerOrganizer();
		
		Collections.sort(wantedPieces);
	}
	
	@Override
	public boolean gotPiece(Peer peer, int piece, byte[] bs) throws IOException {
		synchronized(peers) {
			boolean valid = super.gotPiece(peer, piece, bs);
			
			downloadingPieces.remove(piece);
			
			if(valid){
				organizer.updateDownloaded(peer, bs.length);
				table.removeRequest(piece);
			}
			
			return valid;
		}
	}
	
	@Override
	public int wantPiece(Peer peer, BitField field) {
		synchronized(peers) {
			int piece = table.getFirstInQueue(peer);
			
			if(piece == NO_PIECE) {
				piece = getMostWantedBlock(field);
			}
			else
				table.removeRequest(peer, piece);
			
			if(piece != NO_PIECE && downloadProgress != null)
				downloadProgress.pieceRequested(peer, piece);
			
			if(piece != NO_PIECE)
				downloadingPieces.put(piece, System.currentTimeMillis());
			
			return piece;
		}
	}
	
	@Override
	public void setTracker(TrackerClient client) {
		super.setTracker(client);
		
		startMBDF();
		startRescheduleBlock();
		startTuneQueue();
	}
	
	@Override
	public void halt() {
		super.halt();
		
		TASK_TIMER.cancel();
	}
	
	private void startMBDF() {
		TASK_TIMER.schedule(new TimerTask() {
			@Override
			public void run() {
				synchronized(peers) {
					int piece = getMostWantedBlock(null);
					
					if(piece != NO_PIECE)
						downloadNewPiece(piece);
				}
			}
		}, 0, MBDF_SLEEP);
	}
	
	private void startRescheduleBlock() {
		TASK_TIMER.schedule(new TimerTask() {
			@Override
			public void run() {
				synchronized(peers) {
					reschedulePieces();
				}
			}
		}, 0, RESCHEDULE_SLEEP);
	}
	
	private void startTuneQueue() {
		TASK_TIMER.schedule(new TimerTask() {
			@Override
			public void run() {
				synchronized(peers) {
					for(Peer peer : peers) {
						double speed = organizer.getCurrentSpeed(peer);
						table.updateQueueLength(peer, speed, 
							organizer.updateSpeed(peer));
					}
				}
			}
		}, TUNE_QUEUE_INIT_DELAY, TUNE_QUEUE_SLEEP);
	}
	
	// Called from MBDF timer and wantPiece
	private int getMostWantedBlock(BitField field) {
		if(downloadingPieces.size() > THETA)
			return NO_PIECE;
		
		int piece = NO_PIECE;
		if(RAND.nextDouble() < P)
			piece = getMostWantedPeerBlock(field);
		else
			piece = getMostWantedSystemBlock(field);
		
		return piece;
	}
	
	private int getMostWantedPeerBlock(BitField field) {
		for(int i = 0; i < wantedPieces.size(); i++) {
			int piece = wantedPieces.get(i);
			boolean has = field != null ? field.get(piece) : 
				blockAvailable(piece);
			
			boolean doRequest = downloadingPieces.containsKey(piece) ?
				System.currentTimeMillis() - downloadingPieces.get(piece) > 
					DOWNLOAD_TIMEOUT : true;
			if(has && doRequest) {
				return piece;
			}
		}
		
		return NO_PIECE;
	}
	
	private boolean blockAvailable(int piece) {
		for(Peer peer : peers) {
			if(peer.getHaves() != null && 
					peer.getHaves().get(piece) && !table.isQueueFull(peer))
				return true;
		}
		
		return false;
	}

	private int getMostWantedSystemBlock(BitField field) {
		Map<Integer, Integer> count = new HashMap<Integer, Integer>();
		
		for(Peer peer : peers) {
			BitField haves = peer.getHaves();
			if(haves == null)
				continue;
			
			for(int i = 0, j = 0; i < haves.size() && 
					j < NUMBER_OF_MOST_WANTED_BLOCKS; i++) {
				boolean has = field != null ? field.get(i) : true;
				if(has && !haves.get(i)) {
					j++;
					int c = count.containsKey(i) ? count.get(i) + 1 : 1;
					count.put(i, c);
				}
			}
		}
		
		List<Integer> result = new ArrayList<Integer>();
		int wanted = 0;
		for(int i : count.keySet())
			if(count.get(i) > wanted) {
				wanted = count.get(i);
				result.clear();
				result.add(i);
			}
			else if(count.get(i) == wanted)
				result.add(i);
		
		int piece = result.isEmpty() ? NO_PIECE : result.get(
				RAND.nextInt(result.size()));
		
		return piece;
	}
	
	// Called from MBDF and rescheduleBlock timers.
	private void downloadNewPiece(int piece) {
		List<Peer> qualified = new ArrayList<Peer>();
		for(Peer peer : peers)
			if(peer.getHaves() != null && 
					peer.getHaves().get(piece) && !table.isQueueFull(peer))
				qualified.add(peer);
		
		if(qualified.isEmpty()) //Should not happen often. But then again...
			return;
		
		Comparator<Peer> finishTime = new Comparator<Peer>() {
			@Override
			public int compare(Peer o1, Peer o2) {
				return finishTime(o1) - finishTime(o2);
			}
			
			private int finishTime(Peer peer) {
				return table.estimatedAllDownloadsFinish(peer, 
						organizer.getCurrentSpeed(peer));
			}
		};
		
		Peer peer = Collections.min(qualified, finishTime);
		
		table.addRequest(peer, piece, 
			table.estimatedDownloadFinish(
				peer, organizer.getCurrentSpeed(peer)));
	}
	
	private void rescheduleAPiece(int piece) {
		int index = wantedPieces.indexOf(piece);
		
		if(index >= NUMBER_OF_MOST_WANTED_BLOCKS)
			table.removeRequest(piece);
		// TODO Send cancel message to the corresponding peers.
		
		downloadNewPiece(piece);
	}
	
	// Called from rescheduleBlock timer.
	private void reschedulePieces() {
		for(Peer peer : peers) {
			double speed = organizer.updateSpeed(peer);
			table.updateTimes(peer, speed);
		}

		table.sortBlockQueues();
		
		ArrayList<Integer> pieces = 
			new ArrayList<Integer>(table.getRequestedPieces());
		for(int piece : pieces)
			if(!table.canMeetDeadline(piece)) {
				rescheduleAPiece(piece);
			}
	}

}
