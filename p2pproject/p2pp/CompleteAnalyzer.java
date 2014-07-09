package p2pp;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import p2pp.FileDownloadProgressListener.LogReader;
import p2pp.FileDownloadProgressListener.Piece;

public class CompleteAnalyzer implements Analyzer<Integer> {

	private List<Piece> pieces;
	
	public CompleteAnalyzer(String filename) throws IOException {
		LogReader reader = new LogReader(filename);
		this.pieces = reader.read(true);
	}
	
	public CompleteAnalyzer(List<Piece> pieces) {
		this.pieces = pieces;
	}

	@Override
	public Integer analyze() {
		Comparator<Piece> complete = new Comparator<Piece>() {

			@Override
			public int compare(Piece o1, Piece o2) {
				return (int) (o1.getRequests().get(0).getReceived() - 
					o2.getRequests().get(0).getReceived());
			}
			
		};
		
		Piece last = Collections.max(pieces, complete);
		return (int) last.getRequests().get(0).getReceived();
	}

}
