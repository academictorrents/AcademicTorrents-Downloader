import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.Logger;

public class Main {

	final public static int TIMEOUT = 3000;
	final public static String ATDIR = ".atdownload";
	
	
	public static void main(String[] args) throws Exception {
	
		
		System.out.println("Welcome to the Academic Torrents Download tool!");
		
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
        	String name = url.getFile();
        	System.out.println(name);
        	FileUtils.copyURLToFile(url, new File(ATDIR + "/" + name));
        	downloadedTorrentFile = new File(ATDIR + "/" + name);
        	
        }
        
        

		VuzeATDownloadEngine.download(downloadedTorrentFile);
		
	}

}
