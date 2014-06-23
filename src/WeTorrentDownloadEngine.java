import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.bitlet.wetorrent.Metafile;
import org.bitlet.wetorrent.Torrent;
import org.bitlet.wetorrent.disk.PlainFileSystemTorrentDisk;
import org.bitlet.wetorrent.disk.ResumeListener;
import org.bitlet.wetorrent.disk.TorrentDisk;
import org.bitlet.wetorrent.peer.IncomingPeerListener;


public class WeTorrentDownloadEngine {
	
	private static final int PORT = 6881;

	public static void download(File file) throws Exception {
		System.out.println("Using WeTorrent Engine");


        final Metafile metafile = new Metafile(new BufferedInputStream(new FileInputStream(file)));
        System.out.println("Downloading: " + metafile.getName());
        
//        for (Object s : metafile.getInfo().keySet())
//        	System.out.println(s + " " + metafile.getInfo().get(s));
//        
        
        // Create the torrent disk, this is the destination where the torrent file/s will be saved
        TorrentDisk tdisk = new PlainFileSystemTorrentDisk(metafile, new File("."));
        
        if (tdisk.init()){
	        tdisk.resume(new ResumeListener() {
				
				@Override
				public void percent(long completed, long resumed) {
					
					for (int i = 0; i< 80; i++){
		            	System.out.print("\b");
		            }
					
					int done = (int) ((completed*1.0/metafile.getLength()*1.0)*100);
		            
		            System.out.print("Resuming (have/scanned/total) " + 
		            		Main.humanReadableByteCount(resumed,true) + "/" + 
		            		Main.humanReadableByteCount(completed,true) + "/" + 
		            		Main.humanReadableByteCount(metafile.getLength(),true) + ", " + 
		            done + "%");
					
				}
			});
        }
        
        IncomingPeerListener peerListener = new IncomingPeerListener(PORT);
        peerListener.start();

        Torrent torrent = new Torrent(metafile, tdisk, peerListener);
        
        
        System.out.println();
        torrent.startDownload();

        DescriptiveStatistics stats = new DescriptiveStatistics();
        Long previoussize = torrent.getTorrentDisk().getCompleted();
        int previousStringLength = 100;
        while (!torrent.isCompleted()) {

            try {
                Thread.sleep(1000);
            } catch(InterruptedException ie) {
                break;
            }

            torrent.tick();
            for (int i = 0; i< previousStringLength+1; i++){
            	System.out.print("\b");
            }
            
            
            int done = (int) ((torrent.getTorrentDisk().getCompleted()*1.0/metafile.getLength()*1.0)*100);
            
            stats.addValue(torrent.getTorrentDisk().getCompleted() - previoussize);
            previoussize = torrent.getTorrentDisk().getCompleted();
            
            String toprint = String.format("Downloading (have/total) %s/%s, " + done + "%%, %s bytes/sec, %s peers",
            		Main.humanReadableByteCount(torrent.getTorrentDisk().getCompleted(), true),
            		Main.humanReadableByteCount(metafile.getLength(), true),
            		Main.humanReadableByteCount((long) stats.getMean(), true),
            		torrent.getPeersManager().getActivePeersNumber()
            		);
            previousStringLength = toprint.length();
            System.out.print(toprint);
            
        }
        
        System.out.println("\nFinished");
        System.exit(0);
        //torrent.interrupt();
        //peerListener.interrupt();
        
		
		
		
	}

}
