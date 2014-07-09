/*
 * Make an instance of the Driver class to start the program.
 */

package p2pp;

import java.io.IOException;
import java.util.logging.Level;

import org.klomp.snark.ShutdownListener;
import org.klomp.snark.Snark;
import org.klomp.snark.SnarkShutdown;

public class Driver implements Runnable {
	
	public static void main(String... args) throws InterruptedException {
		// This torrent can be used for testing (50 mb).
		String torrent = "http://academictorrents.com/download/7858fdf307d9fe94aeaaeaeadfc554988b80a3ce.torrent";
		//torrent = "http://ge.tt/#554b7LQ";
		
		// The url to server.
		String serverUrl = "http://www.jmlr.org/papers/volume8/tewari07a/tewari07a.pdf";
		
		// The estimated bitrate in kiloBits/second.
		int bitrate = 1911000;
		
		boolean useName = true;
		
		// Create new driver object to start the program.
//		Thread bass = new Thread(new Driver(Level.ALL, torrent, 
//			new BassPeerCoordinatorFactory(bitrate, serverUrl, useName)));
		
		Thread bass = new Thread(new Driver(Level.ALL, torrent, 
				new VoDBTPeerCoordinatorFactory(bitrate, serverUrl, useName)));
		
		bass.start();

		bass.join();
	}
	
	private Level level;
	private String torrent;
	private PeerCoordinatorFactory fac;

	/**
	 * Create a Driver object to start the program.
	 * @param level The log level to be used.
	 * @param torrent Path to a .torrent file.
	 * @param fac PeerCoordinatorFactory to be used by Snark. The factory is
	 * called when a new instance of PeerCoordinator is needed (only called
	 * once from within Snark). Can be null, in that case a default 
	 * PeerCoordinator is used.
	 */
	public Driver(Level level, String torrent, PeerCoordinatorFactory fac)  {
		this.level = level;
		this.torrent = torrent;
		this.fac = fac;
	}
	
	public Driver(String torrent, PeerCoordinatorFactory fac) {
		this(Level.INFO, torrent, fac);
	}

	@Override
	public void run() {
		Snark.setLogLevel(level);
		Snark snark = new Snark(torrent, 
				null, -1, new NullStorageListener(), null);
		snark.setPeerCoordinatorFactory(fac);
		
		ShutdownListener listener = new ShutdownListener() {
            // documentation inherited from interface ShutdownListener
            public void shutdown ()
            {
                // Should not be necessary since all non-deamon threads should
                // have died. But in reality this does not always happen.
                System.exit(0);
            }
        };
        
        SnarkShutdown hook = new SnarkShutdown(snark, listener);
        Runtime.getRuntime().addShutdownHook(hook);

        // Let's start grabbing files!
        try {
            snark.setupNetwork();
            snark.collectPieces();
        } catch (IOException ioe) {
            System.exit(-1);
        }
	}
}
