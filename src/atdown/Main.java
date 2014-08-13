package atdown;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bitlet.wetorrent.Metafile;
import org.gudy.azureus2.core3.logging.Logger;

import smartnode.models.Collection;
import smartnode.models.Entry;
import smartnode.utils.ATFetcher;
import smartnode.utils.ATLogger;
import smartnode.utils.ATLogger.LogLevel;

public class Main {

	final public static int TIMEOUT = 3000;
	final public static String ATDIR = System.getProperty("user.home") + "/.atdown/";
	
	private static PrintStream stdout = System.out;
	private static PrintStream stderr = System.err;
	
	public static void println(String s){
		
		stdout.println(s);
	}
	
	public static void print(String s){
		
		stdout.print(s);
	}
	
	
	private static void hardLogging() throws FileNotFoundException{
		
		//Logger.allowLoggingToStdErr(true);
		System.setOut(new PrintStream(new File(ATDIR + "log.out")));
		System.setErr(new PrintStream(new File(ATDIR + "log.err")));
		
	}
	
	public static void main(String[] args) throws Exception {
	
		new File(ATDIR).mkdirs();
		
		List<String> argsl = new ArrayList<String>(Arrays.asList(args));
		
		if (!argsl.remove("-v")){
			hardLogging();
		}
		
		
		Main.println("Welcome to the Academic Torrents Download tool!");
		
		try{
			
			main2(argsl.toArray(new String[]{}));
		}catch(Exception e){
			
			Main.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	public static void main2(String[] args) throws Exception {
		
		//args = new String[]{"82c64b111b07ff855b8966701a13a25512687521","ls"};
		//args = new String[]{"059ed25558b4587143db637ac3ca94bebb57d88d","ls"};
		
		//args = new String[]{"7858fdf307d9fe94aeaaeaeadfc554988b80a3ce"};
		
		//mdim
		//args = new String[]{"059ed25558b4587143db637ac3ca94bebb57d88d"};
		
		// wiki
		//args = new String[]{"30ac2ef27829b1b5a7d0644097f55f335ca5241b"};
		
		//args = new String[]{"joseph-paul-cohen-publications"};
		
		//args = new String[]{"ffa02bdccbfd01ac5ce35c2bfee6210abb4ddd0f.torrent"};
		
		//args = new String[]{"tex"};
		
		//args = new String[]{"gnu-radio-rf-captures", "ls"};
		//args = new String[]{"massgis-datasets", "ls"};
		//args = new String[]{"journal-of-machine-learning-research","ls"};
		
		if (args.length < 1){
			Main.println("Usage: atdown ls // list connections");
			Main.println("Usage: atdown INFOHASH // download entry");
			Main.println("Usage: atdown INFOHASH ls // list contents of entry");
			System.exit(0);
		}
		
		String input = args[0];//"7858fdf307d9fe94aeaaeaeadfc554988b80a3ce";

		
		if (input.equals("ls")){
			
			Main.println("Fetching all collections..");
			ATLogger logger = new ATLogger(ATDIR + "log.atlogger", LogLevel.Error);
			ArrayList<Collection> collections = new ATFetcher(logger).getCollections();
			
			Main.println(String.format("|%-65s|%-25.25s|%6s|%9s|", "url-name", "Name", "count", "total size"));
			for (Collection c : collections){
				
				
				Main.println(String.format("|%-65s|%-25.25s|%6s|%9s|", c.getUrlname(), c.getName(), c.getTorrent_count(), Main.humanReadableByteCount(c.getTotal_size_bytes(),true)));
				//Main.println(c.getUrlname() + ", " + c.getName() + ", " + c.getTorrent_count() + " entries, " + Main.humanReadableByteCount(c.getTotal_size_bytes(),true));
				
			}
			System.exit(0);
			
		}
		
		
        // read torrent filename from command line arg
        List<Entry> toget = new ArrayList<Entry>();
        
        if (new File(input).exists() && !(new File(input).isDirectory())){
        	
        	byte[] torrent = IOUtils.toByteArray(new FileInputStream(new File(input)));
        	Metafile meta = new Metafile(new ByteArrayInputStream(torrent));
        	String infohash = DatatypeConverter.printHexBinary(meta.getInfoSha1());
        	Entry e = new Entry(infohash);
        	e.setName(meta.getName());
        	e.setBibtex(meta.getComment());
        	e.setTorrentFile(torrent);
        	toget.add(e);
        }else if (input.startsWith("http") || input.startsWith("ftp")){
        	
        	byte[] torrent = IOUtils.toByteArray(new URL(input));
        	Metafile meta = new Metafile(new ByteArrayInputStream(torrent));
        	String infohash = DatatypeConverter.printHexBinary(meta.getInfoSha1());
        	Entry e = new Entry(infohash);
        	e.setName(meta.getName());
        	e.setBibtex(meta.getComment());
        	e.setTorrentFile(torrent);
        	toget.add(e);
        	
        }else{
        	

        		try{
        			
                	// if length 40 we try as info hash
                	if (input.length() != 40)
                		throw new Exception("Cannot be hash, not a big deal");
        			
            		byte[] torrent = getFromCacheOrDownload(input);
            		Metafile meta = new Metafile(new ByteArrayInputStream(torrent));
            		String infohash = DatatypeConverter.printHexBinary(meta.getInfoSha1());
                	Entry e = new Entry(infohash);
                	e.setName(meta.getName());
                	e.setBibtex(meta.getComment());
                	e.setTorrentFile(torrent);
                	toget.add(e);

                	
        		}catch(Exception e){
        			
        			// not a hash, maybe collection
        			ATLogger logger = new ATLogger(ATDIR + "log.atlogger", LogLevel.Error);
        			Map<String, Entry> collection = new ATFetcher(logger).getCollectionEntries(input);
        			
        			if (collection == null || collection.size() == 0){
        				Main.println("Error fetching collection");
        				logger.printLastLines();
        				System.exit(-1);
        			}
        			
        			int count = 0;
        			Main.print("Fetching collection " + count + "/" + collection.size());
        			
        			for (Entry entry : collection.values()){
        				count++;
        				try{
        					//System.out.println(entry.getInfohash());
        					byte[] torrent = getFromCacheOrDownload(entry.getInfohash());
        					
	        				Metafile meta = new Metafile(new ByteArrayInputStream(torrent));
	        				String infohash = DatatypeConverter.printHexBinary(meta.getInfoSha1());
	        				entry.setTorrentFile(torrent);
	        				if (!entry.getInfohash().equalsIgnoreCase(infohash)){
	        					Main.println(entry.getInfohash());
	        					Main.println(infohash);
	        					throw new Exception("Collection-Entry Consistancy Error");
	        				}
	        				
	        				toget.add(entry);
        				
        				}catch(Exception ex){
        					Main.println("Error with entry: " + entry.getInfohash());
        				}
        				
        				
        				Main.print("\rFetching collection " + count + "/" + collection.size());
        				
        			}
        			Main.println("");
        		}
        }
        
        if (toget.size() < 1){
			Main.println("Nothing selected to download");
			System.exit(-1);
        }
        	
        
        /////////////////////////////////////////////////
        // now toget has the files we are looking for
        
        
        DownloadEngine de;
        
        if (args.length >= 2){
        	
        	// special op
        	
        	
        	if ("ls".equals(args[1])){
        		
        		// just list files
        		for (Entry e : toget){
        			new WeTorrentDownloadEngine().ls(e);
        		}
        	}else if ("info".equals(args[1])){
        		
        		
        		
        	}else{
        		
        		de = new VuzeATDownloadEngine();
        		
        		// download specific files
        		// NOT WORKING YET
        		for (Entry e : toget){
        			de.download(e, args[1]);
        		}
        	}
        }else{
        
        	// just resume or start download it
        	de = new VuzeATDownloadEngine();
        	
    		for (Entry e : toget){
    			de.download(e, null);
    		}
        }
        
        //de.shutdown();
		
		
	}
	
	
	
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + "B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f%sB", bytes / Math.pow(unit, exp), pre);
	}
	
	public static String humanReadableByteCountRatio(long bytes, long totbytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (totbytes < unit) return bytes + "/" + totbytes + "B";
	    int exp = (int) (Math.log(totbytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f/%.1f%sB", bytes / Math.pow(unit, exp),  totbytes / Math.pow(unit, exp), pre);
	}
	
	public static String clean(String s){
		return s.replaceAll("[^\\x00-\\x7f]", "");
	}
	
	
	private static byte[] getFromCacheOrDownload(String infohash) throws MalformedURLException, IOException{
		
		infohash = infohash.toLowerCase();
		
		try{
			
			byte[] torrent = IOUtils.toByteArray(new FileInputStream(new File(Main.ATDIR + infohash + ".torrent")));
			
			// verify it works
			Metafile meta = new Metafile(new ByteArrayInputStream(torrent));
			
			return torrent;
			
		}catch(Exception e){

			byte[] torrent = IOUtils.toByteArray(new URL("http://academictorrents.com/download/" + infohash));

			FileOutputStream fw = new FileOutputStream(Main.ATDIR + infohash + ".torrent");
			IOUtils.copy(new ByteArrayInputStream(torrent), fw);
			fw.flush();
			fw.close();

			
			return torrent;
			
		}
		
	}
	
	

}
