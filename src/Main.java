import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bitlet.wetorrent.Metafile;

import smartnode.models.Entry;
import smartnode.utils.ATFetcher;
import smartnode.utils.ATLogger;
import smartnode.utils.ATLogger.LogLevel;

public class Main {

	final public static int TIMEOUT = 3000;
	final public static String ATDIR = ".atdown/";
	
	private static PrintStream stdout = null;
	private static PrintStream stderr = null;
	
	public static void println(String s){
		
		stdout.println(s);
	}
	
	public static void print(String s){
		
		stdout.print(s);
	}
	
	
	private static void hardLogging() throws FileNotFoundException{
		
		stdout = System.out;
		stderr = System.err;
		
		System.setOut(new PrintStream(new File(ATDIR + "log.out")));
		System.setErr(new PrintStream(new File(ATDIR + "log.err")));
		
	}
	
	public static void main(String[] args) throws Exception {
	
		new File(ATDIR).mkdirs();
		hardLogging();
		
		
		Main.println("Welcome to the Academic Torrents Download tool!");
		
		try{
			
			main2(args);
		}catch(Exception e){
			
			Main.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	public static void main2(String[] args) throws Exception {
		
		//args = new String[]{"82c64b111b07ff855b8966701a13a25512687521","ls"};
		//args = new String[]{"059ed25558b4587143db637ac3ca94bebb57d88d","ls"};
		
		//args = new String[]{"7858fdf307d9fe94aeaaeaeadfc554988b80a3ce"};
		
		args = new String[]{"059ed25558b4587143db637ac3ca94bebb57d88d"};
		
		// wiki
		//args = new String[]{"30ac2ef27829b1b5a7d0644097f55f335ca5241b"};
		
		
		
		//args = new String[]{"gnu-radio-rf-captures", "ls"};
		//args = new String[]{"massgis-datasets", "ls"};
		//args = new String[]{"journal-of-machine-learning-research","ls"};
		
		if (args.length < 1){
			Main.println("Usage: atdown INFOHASH");
			System.exit(0);
		}
		
		String input = args[0];//"7858fdf307d9fe94aeaaeaeadfc554988b80a3ce";

        // read torrent filename from command line arg
        List<Entry> toget = new ArrayList<Entry>();

        if (new File(input).exists() && !(new File(input).isDirectory())){
        	
        	byte[] torrent = IOUtils.toByteArray(new FileInputStream(new File(input)));
        	Metafile meta = new Metafile(new ByteArrayInputStream(torrent));
        	String infohash = DatatypeConverter.printHexBinary(meta.getInfoSha1());
        	Entry e = new Entry(infohash);
        	e.setTorrentFile(torrent);
        	toget.add(e);
        }else if (input.startsWith("http")){
        	
        	byte[] torrent = IOUtils.toByteArray(new URL(input));
        	Metafile meta = new Metafile(new ByteArrayInputStream(torrent));
        	String infohash = DatatypeConverter.printHexBinary(meta.getInfoSha1());
        	Entry e = new Entry(infohash);
        	e.setTorrentFile(torrent);
        	toget.add(e);
        	
        }else{
        	

        		try{
        			
                	// if length 40 we try as info hash
                	if (input.length() != 40)
                		throw new Exception("Cannot be hash, not a big deal");
        			
            		byte[] torrent = IOUtils.toByteArray(new URL("http://academictorrents.com/download/" + input));
            		Metafile meta = new Metafile(new ByteArrayInputStream(torrent));
            		String infohash = DatatypeConverter.printHexBinary(meta.getInfoSha1());
                	Entry e = new Entry(infohash);
                	e.setTorrentFile(torrent);
                	toget.add(e);

                	
        		}catch(Exception e){
        			
        			// not a hash, maybe collection
        			ATLogger logger = new ATLogger(ATDIR + "log.atlogger", LogLevel.Error);
        			Map<String, Entry> collection = new ATFetcher(logger).getCollectionEntries(input);
        			
        			if (collection == null || collection.size() == 0){
        				Main.println("Error fetching collection");
        			}
        			
        			int count = 0;
        			Main.print("Fetching collection " + count + "/" + collection.size());
        			
        			for (Entry entry : collection.values()){
        				count++;
        				try{
        					byte[] torrent = IOUtils.toByteArray(new URL("http://academictorrents.com/download/" + entry.getInfohash()));

	        				Metafile meta = new Metafile(new ByteArrayInputStream(torrent));
	        				String infohash = DatatypeConverter.printHexBinary(meta.getInfoSha1());
	        				entry.setTorrentFile(torrent);
	        				if (!entry.getInfohash().equalsIgnoreCase(infohash)){
	        					Main.println(entry.getInfohash());
	        					Main.println(infohash);
	        					throw new Exception("Collection-Entry Consistancy Error");
	        				}
	        				
	        				toget.add(entry);
        				
        				}catch(FileNotFoundException ex){
        					new Exception("Error with entry: " + entry.getInfohash());
        				}
        				
        				
        				Main.print("\rFetching collection " + count + "/" + collection.size());
        				
        			}
        			Main.println("");
        		}
        }
        
        /////////////////////////////////////////////////
        // now toget has the files we are looking for
        
        
        DownloadEngine de;
        
        de = new VuzeATDownloadEngine();
        
        if (args.length >= 2){
        	
        	// special op
        	
        	
        	if ("ls".equals(args[1])){
        		
        		// just list files
        		for (Entry e : toget){
        			new WeTorrentDownloadEngine().ls(e);
        		}
        	}else if ("info".equals(args[1])){
        		
        		
        		
        	}else{
        		
        		// download specific files
        		// NOT WORKING YET
        		for (Entry e : toget){
        			de.download(e, args[1]);
        		}
        	}
        }else{
        
        	// just resume or start download it
    		for (Entry e : toget){
    			de.download(e, null);
    		}
        }
		
		
	}
	
	
	
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	public static String humanReadableByteCountRatio(long bytes, long totbytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (totbytes < unit) return bytes + "/" + totbytes + " B";
	    int exp = (int) (Math.log(totbytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f/%.1f %sB", bytes / Math.pow(unit, exp),  totbytes / Math.pow(unit, exp), pre);
	}
	
	
	
	public static String clean(String s){
		return s.replaceAll("[^\\x00-\\x7f]", "");
	}

}
