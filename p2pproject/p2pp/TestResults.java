package p2pp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import p2pp.FileDownloadProgressListener.LogReader;
import p2pp.FileDownloadProgressListener.Piece;

public class TestResults {
	
	public static final String sep = System.getProperty("file.separator");
	
	public static final String[] basslogs = {
		"bass-1292856914847.log",
		"bass-1292857922747.log",
		"bass-1292858887523.log",
		"bass-1292859514709.log",
		"bass-1292860087822.log"
	};
	
	public static final String[] livebtlogs = {
		"livebt-1292862410626.log",
		"livebt-1292863194483.log",
		"livebt-1292864360704.log",
		"livebt-1292866671642.log",
		"livebt-1292943720358.log"
	};
	
	public static final String[] vodbtlogs = {
		"VoDBT-1292860869597.log",
		"VoDBT-1292950997104.log",
		"VoDBT-1292955765270.log",
		"VoDBT-1292953625268.log",
		"VoDBT-1292954735565.log"
	};
	
	// Piece size in bytes.
	public static final int pieceLength = 262144;
	
	// Bitrate used in the tests (bits per second).
	public static final int bitrate = 1911000;

	public static final double pieceRate = bitrate / 8.0 / pieceLength / 1000;
	
	// The time unit to be used in plots (NOTE: logs are i milliseconds).
	public static final String timeUnit = "seconds";
	
	// Number of sessions (dirs per client in testlogs)
	public static final int sessions = 5;
	
	// Location of gnuplot. If gnuplot is set in environment variables
	// then only 'gnuplot' is necessary.
	public static final String gnuplot = 
		"C:\\Users\\mirza\\Downloads\\gnuplot\\binary\\gnuplot.exe";
	
	
	public static void main(String... args) throws IOException {
		//plotClientSessions("basstest", "bass-comp", completeFactory(), gnuplot);
		//plotClientSessions("basstest", "bass-play", playbackFactory(
		//		bitrate / 8.0 / pieceLength / 1000), gnuplot);
		plotClient(basslogs, "testlogs" + sep + "basstest", 
				"bass-arrival", gnuplot);
		
		//plotClientSessions("vodbttest", "vodbt-comp", completeFactory(), gnuplot);
		//plotClientSessions("vodbttest", "vodbt-play", playbackFactory(pieceRate), gnuplot);
		
		//plotClient(vodbtlogs, "testlogs" + sep + "vodbttest", "vodbt-arrival", gnuplot);
		
		//double bass = webseedPercentage(basslogs, "testlogs" + sep + "basstest");
		//double vodbt = webseedPercentage(vodbtlogs, "testlogs" + sep + "vodbttest");
		//System.out.println(bass + ", " + vodbt);
		
		//plotClientSessions("livebttest", "livebt-comp", completeFactory(), gnuplot);
		//plotClientSessions("livebttest", "livebt-play", playbackFactory(pieceRate), gnuplot);
		
		//plotClient(livebtlogs, "testlogs" + sep + "livebttest", "livebt-arriaval", gnuplot);
		
		System.out.println("Finished");
	}
	
	public static double webseedPercentage(String[] files, String rootName) 
			throws IOException {
		double result = 0;
		
		for(int i = 1; i < files.length; i++) {
			String filename = rootName + i + sep + files[i - 1];
			WebSeedAnalyzer anal = new WebSeedAnalyzer(filename);
			result += anal.analyze();
		}
		
		return result / files.length;
	}
	
	// rootName on the form testlogs/basstest
	public static void plotClient(String[] files, String rootName, 
			String desc, String gnuplot) throws IOException {
		Plotter plotter = new Plotter(desc, "Time", "Piece arrival");
		
		for(int i = 1; i < files.length; i++) {
			LogReader reader = new LogReader(rootName + i + sep + files[i - 1]);
			List<Piece> pieces = reader.read(true);
			
			for(Piece p : pieces) {
				plotter.put(p.getRequests().get(0).getReceived() / 1000, p.getId());
			}
		}
		
		plotter.setAxisLabels("Time (" + timeUnit + ")", "Piece ID");
		//plotter.setTitle("Pieces Received");
		plotter.setLegendPlacing("right", "bottom");
		plotter.line(bitrate / 8.0 / pieceLength, 0, "Playback");
		plotter.setYMin(0);
		plotter.doFitWithLine();
		
		plotter.createPlot("points");
		plotter.runGnuplot(gnuplot);
	}
	
	public static  void plotClientSessions(String rootName, String desc, 
			AnalyzerFactory<Integer> fac, String gnuplot) throws IOException {
		String[] titles = {"Session", "Average", "Comp 1", "Comp 2", "Comp 3"};
		
		Plotter plotter = new Plotter(desc, titles);
		
		double sum = 0;
		int count = 0;
		
		// Order: i(int) avg(int) session(List<Integer>)
		for(int i = 1; i <= sessions; i++) {
			SessionAnalyzer session = new SessionAnalyzer(fac, "testlogs" + 
					sep + rootName + i);
			SessionAverageAnalyzer avg = new SessionAverageAnalyzer(session);
			
			List<Integer> results = session.analyze();
			for(int j : results)
				sum += j;
			count += results.size();
			
			results.add(0, avg.analyze());
			List<Integer> seconds = new ArrayList<Integer>();
			for(int milli : results)
				seconds.add(milli / 1000);
			results = seconds;
			
			results.add(0, i);
			
			plotter.put(results);
		}
		
		plotter.setAxisLabels("Sessions", "Time (" + timeUnit + ")");
		//plotter.setTitle("Completion");
		plotter.set("xtics", "1");
		plotter.setXRange(0, sessions + 1);
		plotter.horizontalLine(sum / count / 1000, "Overall average");
		plotter.setLegendPlacing("left", "top");
		
		plotter.createPlot("points");
		plotter.runGnuplot(gnuplot);
	}
	
	// bitrate in pieces pr. milliseconds (time is logged in milliseconds in
	// the .log files).
	public static AnalyzerFactory<Integer> playbackFactory(final double bitrate) {
		return new AnalyzerFactory<Integer>() {
			@Override
			public Analyzer<Integer> getAnalyzer(String filename) {
				try {
					return new PlaybackAnalyzer(filename, bitrate);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				return null;
			}
		};
	}
	
	public static AnalyzerFactory<Integer> completeFactory() {
		return new AnalyzerFactory<Integer>() {
			@Override
			public Analyzer<Integer> getAnalyzer(String filename) {
				try {
					return new CompleteAnalyzer(filename);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				return null;
			}
		};
	}
	
	public static AnalyzerFactory<Double> webseedFactory() {
		return new AnalyzerFactory<Double>() {
			@Override
			public Analyzer<Double> getAnalyzer(String filename) {
				try {
					return new WebSeedAnalyzer(filename);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				return null;
			}
		};
	}

}
