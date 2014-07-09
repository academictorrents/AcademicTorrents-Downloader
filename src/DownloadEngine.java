import java.io.File;

import smartnode.models.Entry;


public interface DownloadEngine {
	
	void ls(Entry entry) throws Exception;

	void download(Entry entry, String string) throws Exception;

}
