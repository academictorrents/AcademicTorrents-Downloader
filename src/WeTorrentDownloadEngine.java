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

	public static void main(String[] args) throws Exception {
		System.out.println("Welcome to the Academic Torrents Download tool!");

		
        // read torrent filename from command line arg
        String filename = args[0];

        // Parse the metafile
        //final Metafile metafile = new Metafile(new BufferedInputStream(new FileInputStream(new File(filename))));
        
        final Metafile metafile;
        if (new File(filename).exists()){
        	metafile = new Metafile(new BufferedInputStream(new FileInputStream(new File(filename))));
        }else if (filename.startsWith("http")){
        	metafile = new Metafile(new BufferedInputStream(new URL(filename).openStream()));
        }else{
        	metafile = new Metafile(new BufferedInputStream(new URL("http://academictorrents.com/download/" + filename).openStream()));
        }
        
        if (args.length > 1){
        	
        	if (args[1].equals("ls")){
        		
        		System.out.println("File Listing:");
        		
        		for (Object s : metafile.getFiles())
                	System.out.println(s);
        		
        	}
        	
        	
        	//System.exit(0);
        }
        
        
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
		            humanReadableByteCount(resumed,true) + "/" + 
		            humanReadableByteCount(completed,true) + "/" + 
		            humanReadableByteCount(metafile.getLength(),true) + ", " + 
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
            		humanReadableByteCount(torrent.getTorrentDisk().getCompleted(), true),
            		humanReadableByteCount(metafile.getLength(), true),
            		humanReadableByteCount((long) stats.getMean(), true),
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
	
	
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

}
