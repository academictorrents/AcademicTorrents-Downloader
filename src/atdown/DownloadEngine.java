package atdown;

import smartnode.models.Entry;


public interface DownloadEngine {

	void shutdown() throws Exception;
	
	void ls(Entry entry) throws Exception;

	void download(Entry entry, String string) throws Exception;


}
