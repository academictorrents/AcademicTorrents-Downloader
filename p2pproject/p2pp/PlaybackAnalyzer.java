package p2pp;

import java.io.IOException;
import java.util.List;

import p2pp.FileDownloadProgressListener.LogReader;
import p2pp.FileDownloadProgressListener.Piece;
import p2pp.FileDownloadProgressListener.Request;

public class PlaybackAnalyzer implements Analyzer<Integer> {
	
	private List<Piece> pieces;
	// Pieces per miliseconds (depends on the time unit used).
	private double bitrate;
	
	public PlaybackAnalyzer(String filename, double bitrate) throws IOException {
		this.bitrate = bitrate;
		LogReader reader = new LogReader(filename);
		this.pieces = reader.read(true);
	}

	@Override
	public Integer analyze() {
		double result = 0;
		
		for(Piece piece : pieces) {
			List<Request> requests = piece.getRequests();
			Request last = requests.get(0);
			
			double at = ((double) last.getReceived()) - 
				(piece.getId() / bitrate);
			if(at > result)
				result = at;
		}
		
		return (int) result;
	}

}
