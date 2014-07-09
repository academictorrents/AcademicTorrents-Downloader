package p2pp;

import java.util.List;

public class SessionAverageAnalyzer implements Analyzer<Integer> {
	
	private SessionAnalyzer session;
	
	public SessionAverageAnalyzer(AnalyzerFactory<Integer> fac, String dir) {
		this.session = new SessionAnalyzer(fac, dir);
	}
	
	public SessionAverageAnalyzer(SessionAnalyzer session) {
		this.session = session;
	}

	@Override
	public Integer analyze() {
		List<Integer> results = session.analyze();
		double res = 0;
		
		for(int i : results)
			res += i;
		
		return (int) (res / results.size());
	}

}
