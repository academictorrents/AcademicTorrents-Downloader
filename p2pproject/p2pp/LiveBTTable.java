package p2pp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.klomp.snark.Peer;

public class LiveBTTable {
	
	private final double SIGMA = 1;
	private final double RHO = 0.1;
	
	private List<RequestTriple> triples = new ArrayList<RequestTriple>();
	private Map<Peer, List<RequestTriple>> downloadQueues = 
		new HashMap<Peer, List<RequestTriple>>();
	private Map<Integer, List<RequestTriple>> blockQueues = 
		new HashMap<Integer, List<RequestTriple>>();
	
	private Map<Peer, Integer> queueLengths = 
		new HashMap<Peer, Integer>();
	
	private int pieceSize;
	private double bitrate; // pieces per second
	
	public LiveBTTable(int pieceSize, int bitrate) {
		this.pieceSize = pieceSize;
		// We get the bitrate in bits per second.
		this.bitrate = (((double) bitrate) / 8.0) / 
			((double) pieceSize);
	}
	
	public Set<Integer> getRequestedPieces() {
		return blockQueues.keySet();
	}
	
	public boolean isQueueFull(Peer peer) {
		List<RequestTriple> queue = downloadQueues.get(peer);
		int size = queueLengths.containsKey(peer) ? queueLengths.get(peer) : 1;
		if(queue == null)
			return false;
		else
			return queue.size() >= size;
	}
	
	public int size() {
		return triples.size();
	}
	
	public boolean isRequested(int piece) {
		List<RequestTriple> queue = blockQueues.get(piece);
		return queue != null && !queue.isEmpty();
	}
	
	public int getFirstInQueue(Peer peer) {
		List<RequestTriple> queue = downloadQueues.get(peer);
		if(queue == null || queue.isEmpty())
			return LiveBTPeerCoordinator.NO_PIECE;
		
		return queue.get(0).piece;
	}
	
	public boolean canMeetDeadline(int piece) {
		return estimatedEarliestBlockFinish(piece) > (piece * bitrate);
	}
	
	public int estimatedDownloadFinish(Peer peer, double speed) {
		if(speed == 0)
			return Integer.MAX_VALUE;
		
		return (int) (estimatedAllDownloadsFinish(peer, speed) + 
				(pieceSize / speed));
	}
	
	public int estimatedAllDownloadsFinish(Peer peer, double speed) { // Tf
		List<RequestTriple> queue = downloadQueues.get(peer);
		if(queue == null || queue.isEmpty())
			return 0;
		else if(speed == 0)
			return Integer.MAX_VALUE;
		
		return (int) (pieceSize * queue.size() / speed);
		
		//return queue.get(queue.size() - 1).getTime();
		
	}

	public int estimatedEarliestBlockFinish(int piece) { //Tb
		List<RequestTriple> queue = blockQueues.get(piece);
		if(queue == null || queue.isEmpty())
			return Integer.MAX_VALUE;
		
		return queue.get(0).time;
	}
	
	public void updateQueueLength(Peer peer, double oldSpeed, double newSpeed) {
		Integer cap = queueLengths.get(peer);
		if(cap == null)
			cap = 1;
		
		if(newSpeed > oldSpeed)
			cap = (int) (cap * (1 + SIGMA));
		else if(newSpeed < oldSpeed) {
			cap = (int) (cap * (1 - RHO));
			if(cap <= 0)
				cap = 1;
		}
		
		queueLengths.put(peer, cap);
	}
	
	public void updateTimes(Peer peer, double speed) {
		List<RequestTriple> queue = downloadQueues.get(peer);
		if(queue == null)
			return;
		
		for(int i = 0; i < queue.size(); i++) {
			queue.get(i).setTime(speed == 0 ? Integer.MAX_VALUE : 
				(int) ((i + 1) * pieceSize / speed));
		}
	}
	
	public void sortBlockQueues() {
		Comparator<RequestTriple> comp = compareTime();
		for(int piece : blockQueues.keySet())
			Collections.sort(blockQueues.get(piece), comp);
	}
	
	public boolean addRequest(Peer peer, int piece, int time) {
		if(isQueueFull(peer))
			return false;
		
		RequestTriple rt = new RequestTriple(peer, piece, time);
		triples.add(rt);
		
		List<RequestTriple> queue = downloadQueues.get(peer);
		if(queue == null) {
			queue = new ArrayList<RequestTriple>();
			downloadQueues.put(peer, queue);
		}
		
		queue.add(rt);
		
		Comparator<RequestTriple> timeComparator = compareTime();
		Collections.sort(queue, timeComparator);
		
		queue = blockQueues.get(piece);
		if(queue == null) {
			queue = new ArrayList<RequestTriple>();
			blockQueues.put(piece, queue);
		}

		queue.add(rt);
		Collections.sort(queue, timeComparator);
		
		return true;
	}
	
	public void removeRequest(RequestTriple rt) {
		triples.remove(rt);
		
		List<RequestTriple> queue = downloadQueues.get(rt.peer);
		if(queue != null)
			queue.remove(rt);
		
		queue = blockQueues.get(rt.piece);
		if(queue != null) {
			queue.remove(rt);
			if(queue.isEmpty())
				blockQueues.remove(rt.piece);
		}
	}
	
	public void removeRequest(Peer peer, int piece) {
		RequestTriple rt = new RequestTriple(peer, piece);
		removeRequest(rt);
	}
	
	public void removeRequest(int piece) {
		for(Peer peer : downloadQueues.keySet())
			removeRequest(peer, piece);
	}
	
	public void removeRequest(List<Peer> peers, int piece) {
		for(Peer peer : peers)
			removeRequest(peer, piece);
	}
	
	public void removeRequest(Peer peer) {
		List<RequestTriple> queue = downloadQueues.remove(peer);
		if(queue == null)
			return;
		
		for(RequestTriple rt : queue)
			removeRequest(rt);
	}
	
	public void dumpTable() {
		dump(downloadQueues, "Download Queues", 70);
		System.out.println("\n");
		dump(blockQueues, "Block Queues", 10);
	}
	
	private <T> void dump(Map<T, List<RequestTriple>> queues, 
			String title, int align) {
		System.out.println(title);
		for(T t : queues.keySet()) {
			StringBuilder row = new StringBuilder(t.toString());
			
			if(t instanceof Peer) {
				int size = queueLengths.containsKey(t) ? queueLengths.get(t) : 1;
				row.append(" (" + size + ")");
			}
			
			for(int i = t.toString().length(); i < align; i++)
				row.append(" ");
			
			int alignEnd = 100;
			List<RequestTriple> queue = queues.get(t);
			for(RequestTriple rt : queue) {
				row.append(rt.toString());
				for(int i = rt.toString().length(); i < alignEnd; i++)
					row.append(" ");
			}
			
			System.out.println("----------------------------------------------" +
				"-------------------------------------------------------------");
			System.out.println(row.toString());
		}
	}
	
	private Comparator<RequestTriple> compareTime() {
		return new Comparator<RequestTriple>() {
			@Override
			public int compare(RequestTriple o1, RequestTriple o2) {
				return o1.time - o2.time;
			}
		};
	}

	static class RequestTriple {
		private Peer peer;
		private int piece;
		private int time;
		
		public RequestTriple(Peer peer, int piece) {
			this(peer, piece, -1);
		}
		
		public RequestTriple(Peer peer, int piece, int time) {
			this.peer = peer;
			this.piece = piece;
			this.time = time;
		}
		
		@Override
		public String toString() {
			return "<" + peer.toString() + ", " + piece + ", " + time + ">";
		}
		
		@Override
		public boolean equals(Object o) {
			if(!(o instanceof RequestTriple))
				return false;
			
			RequestTriple r = (RequestTriple) o;
			return r.peer.equals(peer) && r.piece == piece;
		}

		public Peer getPeer() {
			return peer;
		}
		
		public int getPiece() {
			return piece;
		}
		
		public int getTime() {
			return time;
		}
		
		public void setTime(int time) {
			this.time = time;
		}
	}
}
