package atdown;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.peer.PEPeer;

import smartnode.models.Entry;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreFactory;


public class VuzeATDownloadEngine implements DownloadEngine{

	AzureusCore core;
	final static Thread progressChecker  = new Thread(new VuzeATDownloadEngineStatus());
	
	public VuzeATDownloadEngine() throws Exception {		
		
		System.setProperty("azureus.install.path",Main.ATDIR);
		System.setProperty( "azureus.app.name","ATDownloader");
		
		try{
			core = AzureusCoreFactory.create();
		}catch (Throwable re){
			Main.println("Error starting core: " + re.getLocalizedMessage());
		}
		
	    if (!core.isStarted())
	    	core.start();
	    
	  //remove previous stuff
	    try {
	    	
		    GlobalManager globalManager = core.getGlobalManager();
		    for (DownloadManager d : globalManager.getDownloadManagers()){
		    	System.out.println("Removed: " + d.getDisplayName());
		    	
					globalManager.removeDownloadManager(d);
		    }
		} catch (GlobalManagerDownloadRemovalVetoException e) {
			e.printStackTrace();
			throw new Exception("Error setting up engine");
		}
	    
	    
	    //setup status checker
		progressChecker.setDaemon(true);

	    
	    
	    
	    
//	    new PojoExplorer(core);
//	    PojoExplorer.pausethread();


	}
	
	public void shutdown(){
		Main.println("Shutting down...");
		try {
			core.requestStop();
		} catch (AzureusCoreException aze) {
			Main.println("Could not end session gracefully - forcing exit.....");
			core.stop();
		}
	}
	
	
	
	public void download(Entry entry, String specificFile) throws InterruptedException, GlobalManagerDownloadRemovalVetoException, IOException {
		
		Main.println("Downloading: " + entry.getName());
//	    new File(Main.ATDIR + "/az-config").delete();
//	    System.setProperty("azureus.config.path", Main.ATDIR + "/az-config");

	    
	   // [Start/Stop Rules, Torrent Removal Rules, Share Hoster, Default Tracker Web, Core Update Checker, Core Patch Checker, Platform Checker, UPnP, DHT, DHT Tracker, Magnet URI Handler, External Seed, Local Tracker, Tracker Peer Auth, Network Status, Buddy, RSS]

//	    PluginManager.getDefaults().setDefaultPluginEnabled(PluginManagerDefaults.PID_PLATFORM_CHECKER, false);
//	    PluginManager.getDefaults().setDefaultPluginEnabled(PluginManagerDefaults.PID_CORE_PATCH_CHECKER, false);
//	    PluginManager.getDefaults().setDefaultPluginEnabled(PluginManagerDefaults.PID_CORE_UPDATE_CHECKER, false);
//	    PluginManager.getDefaults().setDefaultPluginEnabled(PluginManagerDefaults.PID_PLUGIN_UPDATE_CHECKER, false);
//	    
	    

	    
	    File downloadDirectory = new File("."); //Destination directory
	    //if(downloadDirectory.exists() == false) downloadDirectory.mkdir();
	    
	    //File downloadedTorrentFile = new File(Main.ATDIR + "/" + entry.getInfohash() + ".torrent");
	    
	    //Start the download of the torrent 
	    GlobalManager globalManager = core.getGlobalManager();
	    
	    //FileUtils.copyInputStreamToFile(new ByteArrayInputStream(entry.getTorrentFile()), downloadedTorrentFile);
	    
	    DownloadManager manager = globalManager.addDownloadManager(Main.ATDIR + entry.getInfohash() + ".torrent",
	                                                               downloadDirectory.getAbsolutePath());
	    
	    //manager.getDiskManager().
	    
//	    for (DiskManagerFileInfo i : manager.getDiskManager().getFiles()){
//	    	
////	    	i.
//	    }
	    
	    
	    
	    //Main.println("Downloading");
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
			//Main.println("Downloading....");
			// Start a new daemon thread periodically check
			// the progress of the upload and print it out
			// to the command line
			if (!VuzeATDownloadEngine.progressChecker.isAlive())
				VuzeATDownloadEngine.progressChecker.start();
			break;
		case DownloadManager.STATE_CHECKING:
			Main.println("Checking Existing Data.." + manager.getDisplayName());
			break;
		case DownloadManager.STATE_ERROR:
			System.out.println("Error : ( Check Log " + manager.getErrorDetails());
			break;
		case DownloadManager.STATE_STOPPED:
			//Main.println("\nStopped.." + manager.getDisplayName());
			break;
		case DownloadManager.STATE_ALLOCATING:
			Main.println("Allocating File Space.." + manager.getDisplayName());
			break;
		case DownloadManager.STATE_INITIALIZING:
			Main.println("Initializing.." + manager.getDisplayName());
			break;
		case DownloadManager.STATE_FINISHING:
			//Main.println("Finishing.." + manager.getDisplayName());
			break;
		default :
			//Main.println("state:" + state);
			
		}
	}

	
	
	
	
	
	public void downloadComplete(DownloadManager manager) {
		Main.println("\nDownload Completed\n");
		
		if (Main.keepsharing){
			Main.println("\n-s Will keep sharing\n");
			return;
		}
		
		
		AzureusCore core = AzureusCoreFactory.getSingleton();
		
		try {
			core.getGlobalManager().removeDownloadManager(manager,false, false);
		} catch (AzureusCoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GlobalManagerDownloadRemovalVetoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// if done
		if (core.getGlobalManager().isSeedingOnly()){
			
			try {
				core.requestStop();
			} catch (AzureusCoreException aze) {
				Main.println("Could not end session gracefully - forcing exit.....");
				core.stop();
			}
		}
	}

	@Override
	public void completionChanged(DownloadManager manager, boolean bCompleted) {
		System.out.println("completionChanged");
		
	}

	@Override
	public void positionChanged(DownloadManager download, int oldPosition,
			int newPosition) {
		System.out.println("positionChanged");
		
	}

	@Override
	public void filePriorityChanged(DownloadManager download,
			DiskManagerFileInfo file) {
		System.out.println("filePriorityChanged");
		
	}
	
	

	
	
}
