import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreFactory;


public class VuzeATDownloadEngine {

	public static void download(File downloadedTorrentFile) throws InterruptedException {
		
    
	    new File(Main.ATDIR + "/az-config").delete();
	    System.setProperty("azureus.config.path", Main.ATDIR + "/az-config");
	    
	    AzureusCore core = AzureusCoreFactory.create();
	    core.start();
	    
	    
	    //System.out.println("Completed download of : " + downloadedTorrentFile.getName());
	    //System.out.println("File stored as : " + downloadedTorrentFile.getAbsolutePath());
	    
	    File downloadDirectory = new File("."); //Destination directory
	    //if(downloadDirectory.exists() == false) downloadDirectory.mkdir();
	    
	    //Start the download of the torrent 
	    GlobalManager globalManager = core.getGlobalManager();
	    DownloadManager manager = globalManager.addDownloadManager(downloadedTorrentFile.getAbsolutePath(),
	                                                               downloadDirectory.getAbsolutePath());
	    System.out.println("Downloading");
	    DownloadManagerListener listener = new DownloadStateListener();
	    manager.addListener(listener);    
	    System.out.println(manager.getErrorDetails());
	    new PojoExplorer(manager);
	    PojoExplorer.pausethread();
	    globalManager.startAllDownloads();
	    //core.requestStop();

	}

}


class DownloadStateListener implements DownloadManagerListener {

	public void stateChanged(DownloadManager manager, int state) {
		switch (state) {
		case DownloadManager.STATE_DOWNLOADING:
			System.out.println("Downloading....");
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
							System.out.println("Downloading " +
									Main.humanReadableByteCount(man.getDiskManager().getRemainingExcludingDND(),true) + "/" + 
									Main.humanReadableByteCount(man.getSize(), true) + 
									+ (man.getStats().getCompleted() / 10.0) + " %, " 
									+ man.getNbSeeds() + " Mirrors " + Arrays.toString(man.getCurrentPeers()));
							downloadCompleted = man.isDownloadComplete(true);
							// Check every 10 seconds on the progress
							Thread.sleep(10000);
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
		System.out.println("Download Completed - Exiting.....");
		AzureusCore core = AzureusCoreFactory.getSingleton();
		try {
			core.requestStop();
		} catch (AzureusCoreException aze) {
			System.out.println("Could not end Azureus session gracefully - "
					+ "forcing exit.....");
			core.stop();
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
