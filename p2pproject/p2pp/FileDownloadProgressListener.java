package p2pp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.klomp.snark.Peer;
import org.klomp.snark.PeerCoordinator;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class FileDownloadProgressListener implements DownloadProgressListener {
	
	private Gson gson;
	private JsonWriter jsonOut;
	private long started;
	private Map<Integer, FileDownloadProgressListener.Piece> pieces;
	
	private PeerCoordinator coord;
	private boolean firstReceived = false;
	
	public FileDownloadProgressListener(String desc) throws IOException {
		this("log", desc);
	}
	
	public FileDownloadProgressListener(String path, String description) 
			throws IOException {
		this.started = System.currentTimeMillis();
		this.gson = new Gson();
		this.pieces = new HashMap<Integer, FileDownloadProgressListener.Piece>();
		
		String name = description + "-" + System.currentTimeMillis() + ".log";
		FileWriter writer = new FileWriter(new File(path, name));
		this.jsonOut = new JsonWriter(writer);
		this.jsonOut.setIndent("  ");
	}

	@Override
	public void pieceDownloaded(Peer peer, int piece) {
		synchronized(pieces) {
			long time = System.currentTimeMillis() - started;
			if(!firstReceived) {
				firstReceived = true;
				try {
					jsonOut.beginArray();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			FileDownloadProgressListener.Piece p = pieces.get(piece);
			String from = peer == null ? "web seed" : peer.toString();
			p.received(from, time);
			
			gson.toJson(p, FileDownloadProgressListener.Piece.class, jsonOut);
			pieces.remove(piece);
		}
	}

	@Override
	public void downloadComplete() {
		coord.halt();
		try {
			jsonOut.endArray();
			jsonOut.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void pieceRequested(Peer peer, int piece) {
		synchronized(pieces) {
			long time = System.currentTimeMillis() - started;
			FileDownloadProgressListener.Piece p = pieces.get(piece);
			if(p == null) {
				p = new FileDownloadProgressListener.Piece(piece);
				pieces.put(piece, p);
			}
			
			String from = peer == null ? WebSeed.description : peer.toString();
			p.requested(from, time);
		}
	}

	@Override
	public void setPeerCoordinator(PeerCoordinator coord) {
		this.coord = coord;
	}
	
	static class LogReader {
		private JsonReader jsonIn;
		private Gson gson = new Gson();
		
		public LogReader(String file) throws FileNotFoundException {
			FileReader reader = new FileReader(file);
			this.jsonIn = new JsonReader(reader);
		}
		
		public List<Piece> read() throws IOException {
			return read(false);
		}
		
		public List<Piece> read(boolean filter) throws IOException {
			Type list = new TypeToken<List<
				FileDownloadProgressListener.Piece>>() {}.getType();
				
			List<Piece> pieces = gson.fromJson(jsonIn, list);
			
			if(filter) {
				for(Piece piece : pieces) {
					List<Request> requests = piece.requests;
					
					Request finished = null;
					
					for(Request request : requests)
						if(request.received != -1 && 
								(finished == null || 
								finished.received > request.received))
							finished = request;
					
					requests.clear();
					requests.add(finished);
				}
			}
				
			return pieces;
		}
	}
	
	static class Piece {
		private int id;
		private List<FileDownloadProgressListener.Request> requests;
		
		public Piece() {}
		
		public Piece(int id) {
			this.id = id;
			this.requests = new ArrayList<FileDownloadProgressListener.Request>();
		}
		
		public void requested(String from, long when) {
			this.requests.add(
				new FileDownloadProgressListener.Request(from, when));
		}
		
		public void received(String from, long when) {
			for(int i = requests.size() - 1; i >= 0; i--) {
				FileDownloadProgressListener.Request request = requests.get(i);
				if(request.from.equals(from)) {
					request.setReceived(when);
					break;
				}
			}
		}
		
		public int getId() {
			return id;
		}
		
		public List<Request> getRequests() {
			return requests;
		}
		
		@Override
		public String toString() {
			return id + ":" + requests.toString();
		}
	}
	
	static class Request {
		private String from;
		private long requested;
		private long received = -1;
		
		public Request() {}
		
		public Request(String from, long when) {
			this.requested = when;
			this.from = from;
		}
		
		public void setReceived(long rec) {
			this.received = rec;
		}
		
		public String getFrom() {
			return from;
		}

		public long getRequested() {
			return requested;
		}

		public long getReceived() {
			return received;
		}

		@Override
		public String toString() {
			return "{from=" + from + ", requested=" + requested + 
				", received=" + received + "}";
		}
	}

}
