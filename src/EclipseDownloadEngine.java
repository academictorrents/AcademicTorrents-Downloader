
import java.io.File;
import java.io.IOException;

import org.eclipse.bittorrent.Torrent;
import org.eclipse.bittorrent.TorrentConfiguration;
import org.eclipse.bittorrent.TorrentFactory;
import org.eclipse.bittorrent.TorrentFile;
import org.eclipse.bittorrent.TorrentServer;
import org.eclipse.bittorrent.TorrentConfiguration.IDebugListener;


public class EclipseDownloadEngine {
	
	private static final int PORT = 6881;

	public static void download(File torrentFile) throws Exception {
		System.out.println("Using Eclipse Torrent Engine");

		
		
		TorrentConfiguration.setConfigurationPath(new File(System
				.getProperty("user.home"), ".hilberteffect"));
		int offset = 0;
		boolean debug = true;
		int port = -1;

		try {

			if (port != -1) {
				TorrentServer.setPort(port);
			}
			TorrentFile file = new TorrentFile(torrentFile);
			
			if (file.isMultiFile())
				file.setTargetFile(new File(file.getHexHash()));
			else
				file.setTargetFile(new File(file.getFilenames()[0].replaceAll("[^\\x00-\\x7f]", "")));
			
			System.out.println(file.getTargetFile());

			Torrent host = TorrentFactory.createTorrent(file);
			
			if (debug) {
				TorrentConfiguration.DEBUG = true;
				TorrentConfiguration.setDebugListener(new DebugListener());
			} else {
				OutputThread thread = new OutputThread(host, file
						.getTargetFile(), file.getTotalLength());
				thread.start();
			}
			host.start();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static class DebugListener implements IDebugListener {

		public void print(String message) {
			System.out.println(message);
		}

	}

	private static class OutputThread extends Thread {

		private Torrent torrent;

		private File file;

		private double total;

		public OutputThread(Torrent torrent, File file, double total) {
			this.torrent = torrent;
			this.file = file;
			this.total = total;
		}

		public void run() {
			while (true) {
				System.out.println("Saving:\t\t\t" + file.getName());
				System.out.println("Connected Peers:\t"
						+ torrent.getConnectedPeers());
				System.out.println("Seeds/Peers:\t\t"
						+ (torrent.getSeeds() == -1 ? "Unknown" : Integer
								.toString(torrent.getSeeds()))
						+ "/"
						+ (torrent.getPeers() == -1 ? "Unknown" : Integer
								.toString(torrent.getPeers())));
				System.out.println("Percent Done:\t\t"
						+ ((total - torrent.getRemaining()) / total * 100)
						+ "%");
				System.out.println("Downloading To:\t\t"
						+ file.getAbsolutePath());
				System.out.println("Download Total:\t\t"
						+ torrent.getDownloaded() + " bytes");
				System.out.println("Upload Total:\t\t" + torrent.getUploaded()
						+ " bytes");
				long eta = torrent.getTimeRemaining();
				System.out.println("ETA:\t\t\t"
						+ (eta == -1 ? "Unknown" : eta + " seconds"));
				System.out.println("Download Speed:\t\t"
						+ (torrent.getDownSpeed() / 1024) + " kb");
				System.out.println("Upload Speed:\t\t"
						+ (torrent.getUpSpeed() / 1024) + " kb");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

		
		
		

}
