package p2pp;

public interface AnalyzerFactory<T> {
	Analyzer<T> getAnalyzer(String filename);
}
