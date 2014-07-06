import java.io.BufferedInputStream;
import java.io.File;
import java.net.URL;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.bitlet.wetorrent.Metafile;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.Logger;

public class Main {

	final public static int TIMEOUT = 3000;
	final public static String ATDIR = ".atdownload";
	
	
	public static void main(String[] args) throws Exception {
	
		
		System.out.println("Welcome to the Academic Torrents Download tool!");
		
		//String input = "82c64b111b07ff855b8966701a13a25512687521";

		if (args.length < 1){
			System.out.println("Usage: atdownload INFOHASH");
			System.exit(0);
		}
		
		String input = args[0];//"7858fdf307d9fe94aeaaeaeadfc554988b80a3ce";

		
        // read torrent filename from command line arg
        File downloadedTorrentFile;

   
        if (new File(input).exists()){
        	downloadedTorrentFile = new File(input);
        }else if (input.startsWith("http")){
        	
        	URL url = new URL(input);
        	String name = url.getFile();
        	FileUtils.copyURLToFile(url, new File(ATDIR + "/" + name));
        	downloadedTorrentFile = new File(ATDIR + "/" + name);
        }else{
        	
        	URL url = new URL("http://academictorrents.com/download/" + input);
        	
        	Metafile meta = new Metafile(new BufferedInputStream(url.openStream()));
        	
        	// no good reason to name files like this
        	String name = meta.getInfoSha1Encoded();
        	//System.out.println(name);
        	FileUtils.copyURLToFile(url, new File(ATDIR + "/temp/" + name));
        	downloadedTorrentFile = new File(ATDIR + "/temp/" + name);
        	
        }
        
        //EclipseDownloadEngine.download(downloadedTorrentFile);
        //VuzeATDownloadEngine.download(downloadedTorrentFile);
		WeTorrentDownloadEngine.download(downloadedTorrentFile);
		
	}
	
	
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

}
