package p2pp;

import java.io.IOException;
import java.util.List;

import p2pp.FileDownloadProgressListener.LogReader;
import p2pp.FileDownloadProgressListener.Piece;
import p2pp.FileDownloadProgressListener.Request;

public class WebSeedAnalyzer implements Analyzer<Double> {
	
	private List<Piece> pieces;
	
	public WebSeedAnalyzer(String filename) throws IOException {
		LogReader reader = new LogReader(filename);
		this.pieces = reader.read(true);
	}

	@Override
	public Double analyze() {
		double res = 0;
		
		for(Piece piece : pieces) {
			Request req = piece.getRequests().get(0);
			if(req.getFrom().equals(WebSeed.description))
				res++;
		}
		
		return res / pieces.size();
	}

}
