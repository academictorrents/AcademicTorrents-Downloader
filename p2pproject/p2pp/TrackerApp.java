package p2pp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;

import org.klomp.snark.ConnectionAcceptor;
import org.klomp.snark.HttpAcceptor;
import org.klomp.snark.MetaInfo;
import org.klomp.snark.Tracker;
import org.klomp.snark.*;
import java.util.logging.*;


public class TrackerApp {

	// Start up a tracker.
	public static void main(String[] args) throws FileNotFoundException, IOException {
		int user_port = 7070;
		String torrent = "eclipse.torrent";
		
		MetaInfo info = new MetaInfo(new FileInputStream(torrent));
		
		Snark.setLogLevel(Level.ALL);
		
		Tracker tracker = new Tracker(info);
        HttpAcceptor httpacceptor = new HttpAcceptor(tracker);
        ConnectionAcceptor acceptor = new ConnectionAcceptor(
            new ServerSocket(user_port), httpacceptor, null);
        System.out.println("started " + info.getHexInfoHash());
        acceptor.start();
	}
	
}
