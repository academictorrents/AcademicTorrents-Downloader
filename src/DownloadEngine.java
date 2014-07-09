import java.io.File;


public interface DownloadEngine {
	
	void ls(byte[] torrentFile) throws Exception;

	void download(byte[] torrentFile, String string) throws Exception;

}
