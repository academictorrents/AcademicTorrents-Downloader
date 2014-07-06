import java.io.File;


public interface DownloadEngine {
	
	void ls(File downloadedTorrentFile) throws Exception;

	void download(File downloadedTorrentFile, String string) throws Exception;

}
