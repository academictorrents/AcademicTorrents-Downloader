package p2pp;

import java.util.HashMap;
import java.util.Map;

import org.klomp.snark.Peer;

public class LiveBTPeer {
	
	private long downloaded;
	private long timeStart;
	private long sizeStart;
	private double speed = 0; //bytes per second
	
	public void addDownloaded(long down) {
		downloaded += down;
	}
	
	public double currentSpeed() {
		return speed;
	}
	
	public double updateSpeed() {
		long tmp = System.currentTimeMillis();
		long deltaTime = (tmp - timeStart) / 1000;
		this.timeStart = tmp;

		long deltaSize = (downloaded - sizeStart);
		this.sizeStart = downloaded;
		
		if(deltaTime != 0)
			this.speed = ((double) deltaSize) / ((double) deltaTime);
		
		return speed;
	}
	
	static class LiveBTPeerOrganizer {
		private Map<Peer, LiveBTPeer> peers = new
			HashMap<Peer, LiveBTPeer>();
		
		public double getCurrentSpeed(Peer peer) {
			return getPeer(peer).currentSpeed();
		}
		
		public double updateSpeed(Peer peer) {
			return getPeer(peer).updateSpeed();
		}
		
		public void updateDownloaded(Peer peer, long down) {
			LiveBTPeer live = getPeer(peer);
			live.addDownloaded(down);
		}
		
		public void removePeer(Peer peer) {
			peers.remove(peer);
		}
		
		public void dumpList() {
			System.out.println("Velocity List");
			for(Peer peer : peers.keySet()) {
				System.out.println("--------------------------------------------");
				System.out.println("v(" + peer.toString() + ") = " + 
					peers.get(peer).currentSpeed());
			}
		}
		
		private LiveBTPeer getPeer(Peer peer) {
			LiveBTPeer live = peers.get(peer);
			if(live == null) {
				live = new LiveBTPeer();
				
				live.sizeStart = 0;
				live.timeStart = System.currentTimeMillis();
				peers.put(peer, live);
			}
			
			return live;
		}
	}
}
