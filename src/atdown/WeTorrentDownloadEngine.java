package atdown;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.bitlet.wetorrent.Metafile;
import org.bitlet.wetorrent.Torrent;
import org.bitlet.wetorrent.disk.PlainFileSystemTorrentDisk;
import org.bitlet.wetorrent.disk.ResumeListener;
import org.bitlet.wetorrent.disk.TorrentDisk;
import org.bitlet.wetorrent.peer.IncomingPeerListener;

import smartnode.models.Entry;


public class WeTorrentDownloadEngine  implements DownloadEngine{
	
	private static final int PORT = 6881;
	
	public WeTorrentDownloadEngine() {

		
	}
	
	@Override
	public void shutdown() throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	
	@Override
	public void download(Entry entry, String specficFile) throws Exception {
		System.out.println("Using WeTorrent Engine");


		final Metafile metafile = new Metafile(new ByteArrayInputStream(entry.getTorrentFile()));
        metafile.setName(Main.clean(metafile.getName()));
        System.out.println("Downloading: " + metafile.getName());
        
//        for (Object s : metafile.getInfo().keySet())
//        	System.out.println(s + " " + metafile.getInfo().get(s));
//        
        
        // Create the torrent disk, this is the destination where the torrent file/s will be saved
        TorrentDisk tdisk = new PlainFileSystemTorrentDisk(metafile, new File("."));

        
        if (tdisk.init()){
	        tdisk.resume(new ResumeListener() {
				
	        	int done = -1;
	        	
				@Override
				public void percent(long completed, long resumed) {

					int newDone = (int) ((completed*1.0/metafile.getLength()*1.0)*100);
					
					if (done != newDone){
						done = newDone;
						
						for (int i = 0; i< 80; i++){
			            	System.out.print("\b");
			            }
						System.out.print("\r");
						
			            System.out.print("Resuming (have/scanned/total) " + 
			            		Main.humanReadableByteCount(resumed,true) + "/" + 
			            		Main.humanReadableByteCount(completed,true) + "/" + 
			            		Main.humanReadableByteCount(metafile.getLength(),true) + ", " + 
			            done + "%");
					}
					
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
            System.out.print("\r");
            
            
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


	@Override
	public void ls(Entry entry) throws Exception {
		
        final Metafile metafile = new Metafile(new ByteArrayInputStream(entry.getTorrentFile()));
        metafile.setName(Main.clean(metafile.getName()));
        
        //System.out.println(metafile.getAnnounceList());
        
        if (metafile.isSingleFile()){
        	 Main.println(entry.getInfohash() + "/" + metafile.getName());
        }else{
	        for (Object elem : metafile.getFiles()) {
	            Map file = (Map) elem;
	            List path = (List) file.get(ByteBuffer.wrap("path".getBytes()));
	            String pathName = metafile.getName();
	
	            Iterator pathIterator = path.iterator();
	            while (pathIterator.hasNext()) {
	                byte[] pathElem = ((ByteBuffer) pathIterator.next()).array();
	                pathName += "/" + new String(pathElem);
	            }
	            Main.println(entry.getInfohash() + "/" + Main.clean(pathName));
	        }
        }
        
        
        
        
//        new PojoExplorer(metafile);
//        PojoExplorer.pausethread();

		
		
		
	}

}
