package p2pp;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

public class SessionAnalyzer implements Analyzer<List<Integer>> {
	
	private List<Analyzer<Integer>> analyzers;
	
	public SessionAnalyzer(AnalyzerFactory<Integer> fac, String dir) {
		File file = new File(dir);
		if(!file.isDirectory())
			throw new IllegalArgumentException(dir + " is not a directory!");
		
		File[] files = file.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".log");
			}
		});
		
		analyzers = new ArrayList<Analyzer<Integer>>();
		
		for(File f : files)
			analyzers.add(fac.getAnalyzer(f.getAbsolutePath()));
	}

	@Override
	public List<Integer> analyze() {
		List<Integer> results = new ArrayList<Integer>();
		
		for(Analyzer<Integer> anal : analyzers)
			results.add(anal.analyze());
		
		return results;
	}

}
