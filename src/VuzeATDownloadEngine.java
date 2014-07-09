import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentFile;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentFileImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreFactory;


public class VuzeATDownloadEngine implements DownloadEngine{

	
	public VuzeATDownloadEngine() {
		// TODO Auto-generated constructor stub
	}
	
	public void download(byte[] torrentFile, String specificFile) throws InterruptedException, GlobalManagerDownloadRemovalVetoException {
		
    
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
	    
	    //Start the download of the torrent 
	    GlobalManager globalManager = core.getGlobalManager();
	    for (DownloadManager d : globalManager.getDownloadManagers()){
	    	Main.println("Removed:" + d.getDisplayName());
	    	globalManager.removeDownloadManager(d);
	    }
	    
	    
	    
	    DownloadManager manager = null;//globalManager.addDownloadManager(downloadedTorrentFile.getAbsolutePath(),
	                                   //                            downloadDirectory.getAbsolutePath());
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
	public void ls(byte[] torrentFile) throws Exception {
		
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
							for (PEPeer peer : man.getCurrentPeers()){
								
								peers.add(peer.getIPHostName());
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
		}
	}

	public void downloadComplete(DownloadManager manager) {
		Main.println("Download Completed - Exiting.....");
		AzureusCore core = AzureusCoreFactory.getSingleton();
		try {
			core.requestStop();
		} catch (AzureusCoreException aze) {
			Main.println("Could not end Azureus session gracefully - "
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
}
