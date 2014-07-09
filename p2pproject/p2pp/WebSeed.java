/*
 * Used to downloading pieces from a server. This is done by issuing a range
 * request to server.
 */

package p2pp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.klomp.snark.MetaInfo;

public class WebSeed {
	
	// Descriptive name of the web seed class.
	public final static String description = "web seed";
	
	public static boolean isWebSeed(String str) {
		return str != null ? str.equals(WebSeed.description) : false;
	}
	
	private String serverUrl;
	
	// The absolute url to all the files.
	private List<URL> files;
	
	// List of file lengths.
	private List<Long> lengths;
	
	private Random random = new Random();
	
	protected static final Logger log = Logger.getLogger("org.klomp.snark.peer");
	
	public WebSeed(List<URL> files, List<Long> lengths) {
		if(files.size() != lengths.size())
			throw new IllegalArgumentException(
					"The length list must match the files list!");
		
		this.files = files;
		this.lengths = lengths;
		this.serverUrl = files.get(0).toString();
	}
	
	/**
	 * Creates the file urls by using the url to the server and the information
	 * from the metainfo. Assuming the torrent file has a name attribute 'movie',
	 * the files will be downloaded locally in the movie/ dir. If useNameInPath
	 * is set to true the name attribute is used to construct the urls, which
	 * gives the urls like 'http://ge.tt/#6aYJ5/movie/...'.
	 * @param url The server url. E.g. 'http://ge.tt/#6aYJ5'.
	 * @param metainfo The torrent file information.
	 * @param useNameInPath If set to true the name attribute in the torrent
	 * file is used to create the file urls.
	 * @throws MalformedURLException If the serverUrl is invalid.
	 */
	@SuppressWarnings("unchecked")
	public WebSeed(String url, MetaInfo metainfo, boolean useNameInPath) 
		throws MalformedURLException {
		
		this.serverUrl = url;
		this.lengths = metainfo.getLengths();
		if(this.lengths == null) {
			this.lengths = new ArrayList<Long>();
			this.lengths.add(metainfo.getTotalLength());
		}
		
		this.files = makeUrls(url, metainfo, useNameInPath);
		
		for(URL u : files)
			System.out.println(u);
		
		if(!this.files.get(0).getProtocol().toLowerCase().equals("http"))
			throw new IllegalArgumentException("Must use http protocol!");
	}
	
	public WebSeed(String url, MetaInfo metainfo) 
			throws MalformedURLException {
		this(url, metainfo, true);
	}
	
	public WebSeed(String url, long length) throws MalformedURLException {
		this.files = new ArrayList<URL>();
		this.files.add(new URL(url));
		
		if(!this.files.get(0).getProtocol().toLowerCase().equals("http"))
			throw new IllegalArgumentException("Must use http protocol!");
		
		this.lengths = new ArrayList<Long>();
		this.lengths.add(length);
		
		this.serverUrl = url;
	}
	
	@Override
	public String toString() {
		return serverUrl;
	}
	
	// Returns true if the server supports range requests.
	public boolean supportsRange() {
		URL server = files.get(random.nextInt(files.size()));
		
		try {
			HttpURLConnection connection = (HttpURLConnection) server.openConnection();
			connection.setRequestMethod("HEAD");
			connection.connect();
			
			String accepts = connection.getHeaderField("Accept-Ranges");
			if(accepts != null)
				return accepts.trim().toLowerCase().equals("bytes");
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	// Returns the byte block specified by the index and the length.
	public byte[] getBlock(long index, int length) throws IOException {
		long start = index;
		int file = 0;
		long fileLength = lengths.get(file);
		while(start > fileLength) {
			file++;
			start -= fileLength;
			fileLength = lengths.get(file);
		}
		
		int read = 0;
		byte[] block = new byte[length];
		while(read < length) {
			int need = length - read;
			int next = start + need < fileLength ? need : (int) (fileLength - start);
			getBlock(this.files.get(file), start, read, next, block);
			read += next;
			
			if(need - next > 0) {
				file++;
				fileLength = lengths.get(file);
				start = 0;
			}
		}
		
		return block;
	}
	
	private void getBlock(URL url, long index, int off, int length, byte[] bs) 
			throws IOException {
		long end = index + length - 1;

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Range", "bytes=" + index + "-" + end);
		
		connection.connect();
		
		int contentLength = connection.getContentLength();
		String contentRange = connection.getHeaderField("Content-Range");
		if(connection.getResponseCode() == HttpURLConnection.HTTP_PARTIAL && 
				contentRange != null) {
			
			long[] range = parseContentRange(contentRange);

			if(contentLength != length || (range[0] != index && range[1] != end))
				throw new IOException("Wrong length or range received from server!");
		}
		else
			throw new IOException("Not a HTTP partial response received from server!");
		
		BufferedInputStream in = 
			new BufferedInputStream(connection.getInputStream());

		int len = 0;
		while((len = in.read(bs, off, length)) != -1 && length > 0)  {
			off += len;
			length -= len;
		}
		
		in.close();
	}
	
	private List<URL> makeUrls(String server, MetaInfo meta, boolean useName) 
		throws MalformedURLException {
		
		List<URL> urls = new ArrayList<URL>();
		
		if(!server.endsWith("/"))
			server += "/";
		
		String base = server;
		if(useName)
			base += meta.getName() + "/";
		
		@SuppressWarnings("unchecked")
		List<List<String>> files = meta.getFiles();
		if(files == null)
			urls.add(new URL(base.substring(0, base.length() - 1)));
		else {
			for(List<String> path : files) {
				Iterator<String> it = path.iterator();
				String fullPath = base;
				
				while(it.hasNext()) {
					String current = it.next();
					//if(it.hasNext())
						fullPath += current + "/";
					//else
					//	fullPath += current + "/";
				}
				
				urls.add(new URL(fullPath.substring(0, fullPath.length() - 1)));
			}
		}
		
		for(URL url : urls)
			log.log(Level.INFO, "Server url: " + url.toString());
		
		return urls;
	}

	private long[] parseContentRange(String contentRange) {
		String[] range = contentRange.split(" ")[1].split("/")[0].split("-");
		if(range.length != 2)
			return new long[2];
		
		try {
			long[] res = {Long.parseLong(range[0]), Long.parseLong(range[1])};
			return res;
		}
		catch(NumberFormatException e) {
			e.printStackTrace();
		}
		
		return new long[2];
	}
	
}
