import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.io.FileUtils;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentFile;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentFileImpl;

import smartnode.models.Entry;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreFactory;


public class VuzeATDownloadEngine implements DownloadEngine{

	
	public VuzeATDownloadEngine() {
		// TODO Auto-generated constructor stub
	}
	
	public void download(Entry entry, String specificFile) throws InterruptedException, GlobalManagerDownloadRemovalVetoException, IOException {
		
    
//	    new File(Main.ATDIR + "/az-config").delete();
//	    System.setProperty("azureus.config.path", Main.ATDIR + "/az-config");
//	    
	    AzureusCore core = AzureusCoreFactory.create();
	    
	    
//	    new PojoExplorer(core);
//	    PojoExplorer.pausethread();
	    core.start();
	    
	    
	    //Main.println("Completed download of : " + downloadedTorrentFile.getName());
	    //Main.println("File stored as : " + downloadedTorrentFile.getAbsolutePath());
	    
	    File downloadDirectory = new File("."); //Destination directory
	    //if(downloadDirectory.exists() == false) downloadDirectory.mkdir();
	    
	    File downloadedTorrentFile = new File(Main.ATDIR + "/" + entry.getInfohash() + ".torrent");
	    
	    //Start the download of the torrent 
	    GlobalManager globalManager = core.getGlobalManager();
	    for (DownloadManager d : globalManager.getDownloadManagers()){
	    	System.out.println("Removed: " + d.getDisplayName());
	    	globalManager.removeDownloadManager(d);
	    }
	    
	    FileUtils.copyInputStreamToFile(new ByteArrayInputStream(entry.getTorrentFile()), downloadedTorrentFile);
	    
	    DownloadManager manager = globalManager.addDownloadManager(downloadedTorrentFile.getAbsolutePath(),
	                                                               downloadDirectory.getAbsolutePath());
	    Main.println("Downloading");
	    DownloadManagerListener listener = new DownloadStateListener();
	    manager.addListener(listener);    
//	    Main.println(manager.getErrorDetails());
//	    new PojoExplorer(manager);
//	    PojoExplorer.pausethread();
	    globalManager.startAllDownloads();
	    
	    //core.requestStop();

	}

	@Override
	public void ls(Entry entry) throws Exception {
		
	}

}


class DownloadStateListener implements DownloadManagerListener {

	public void stateChanged(DownloadManager manager, int state) {
		switch (state) {
		case DownloadManager.STATE_DOWNLOADING:
			Main.println("Downloading....");
			// Start a new daemon thread periodically check
			// the progress of the upload and print it out
			// to the command line
			Runnable checkAndPrintProgress = new Runnable() {

				public void run() {
					try {
						boolean downloadCompleted = false;
						while (!downloadCompleted) {
							AzureusCore core = AzureusCoreFactory.getSingleton();
							List<DownloadManager> managers = core.getGlobalManager().getDownloadManagers();

							// There is only one in the queue.
							DownloadManager man = managers.get(0);
							
							List<String> peers = new ArrayList<String>();
							int count = 0;
							for (PEPeer peer : man.getCurrentPeers()){
								
								long dlrate = peer.getStats().getDataReceiveRate();
								
								if (dlrate != 0){
									count++;
									String pstring = peer.getIPHostName();
									
									if (!hasAlpha(pstring)){
										pstring = getRevName(pstring);
										
									}
	
									// get rid of last .
									if (pstring.length() == pstring.lastIndexOf('.')+1)
										pstring = pstring.substring(0, pstring.length()-1);
									
									// get end of dns
									pstring = pstring.substring(pstring.lastIndexOf('.',pstring.lastIndexOf('.')-1)+1);
									
									// only show some peers but show all edu
									if (!(count > 5) || pstring.contains(".edu"))
										peers.add(pstring + " " + Main.humanReadableByteCount(dlrate, true) + "/s");
								}
							}
							
							
							Main.println("Downloading " +
									Main.humanReadableByteCount(man.getSize() - man.getDiskManager().getRemainingExcludingDND(),true) + "/" + 
									Main.humanReadableByteCount(man.getSize(), true) + " " + 
									+ (man.getStats().getCompleted() / 10.0) + "%, " 
									+ man.getNbSeeds() + " Mirrors " + peers.toString());
							downloadCompleted = man.isDownloadComplete(true);
							// Check every 10 seconds on the progress
							Thread.sleep(1000);
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

				}
			};

			Thread progressChecker = new Thread(checkAndPrintProgress);
			progressChecker.setDaemon(true);
			progressChecker.start();
			break;
		case DownloadManager.STATE_CHECKING:
			Main.println("Checking");
		default :
			//Main.println("state:" + state);
			
		}
	}

	public void downloadComplete(DownloadManager manager) {
		Main.println("Download Completed - Exiting.....");
		AzureusCore core = AzureusCoreFactory.getSingleton();
		try {
			core.requestStop();
		} catch (AzureusCoreException aze) {
			Main.println("Could not end session gracefully - "
					+ "forcing exit.....");
			core.stop();
		}
	}

	@Override
	public void completionChanged(DownloadManager manager, boolean bCompleted) {
		Main.println("completionChanged");
		
	}

	@Override
	public void positionChanged(DownloadManager download, int oldPosition,
			int newPosition) {
		Main.println("positionChanged");
		
	}

	@Override
	public void filePriorityChanged(DownloadManager download,
			DiskManagerFileInfo file) {
		Main.println("filePriorityChanged");
		
	}
	
	
	
	
	public static String getRevName(String oipAddr) throws NamingException {
		
		String ipAddr = oipAddr;
		try{
			Properties env = new Properties();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
			InitialDirContext idc = new InitialDirContext(env);
			
			  String revName = null;
			  String[] quads = ipAddr.split("\\.");
			 
			  //StringBuilder would be better, I know.
			  ipAddr = "";
			 
			  for (int i = quads.length - 1; i >= 0; i--) {
			    ipAddr += quads[i] + ".";
			  }
			 
			  ipAddr += "in-addr.arpa.";
			  Attributes attrs = idc.getAttributes(ipAddr, new String[] {"PTR"});
			  Attribute attr = attrs.get("PTR");
			 
			  if (attr != null) {
			    revName = (String) attr.get(0);
			  }
			  
			  return revName;
		}catch (Exception e){
			
			 return ipAddr;
		}
		 
		 
	}
	
	
	public static boolean hasAlpha(String name) {
	    char[] chars = name.toCharArray();

	    for (char c : chars) {
	        if(Character.isLetter(c)) {
	            return true;
	        }
	    }

	    return false;
	}
	
	
	
}
