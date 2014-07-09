package p2pp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Plotter {

	private Map<String, String> options = new HashMap<String, String>();
	private List<String> labels = new ArrayList<String>();
	private String line = null;
	private String fittedLine = null;
	
	private List<double[]> data = new ArrayList<double[]>();
	private int nbOfPlots;
	private String[] titles;
	private String description;
	
	private File dataFile;
	private File scriptFile;
	
	private final String defaultType = "linespoints";
	
	public Plotter(String description, String... titles) {
		this(description, new File("plot"), titles);
	}
	
	public Plotter(String description, File path, String... titles) {
		this.nbOfPlots = titles.length;
		this.titles = titles;
		this.description = description;
		
		long time = System.currentTimeMillis();
		this.dataFile = new File(path, description + "-" + time + "-data.data");
		this.scriptFile = new File(path, description + "-" + time + "-script.p");
		
		this.set("xtics", "auto");
		this.set("ytics", "auto");
		this.set("datafile missing", "NaN", true);
		
		// Set output to pdf.
		this.set("terminal", "pdf");
		this.set("output", description + "-" + time + "-plot.pdf", true);
	}
	
	public void set(String name, String value) {
		set(name, value, false);
	}
	
	public void set(String name, String value, boolean wrap) {
		value = wrap ? "\"" + value + "\"" : value;
		options.put(name, value);
	}
	
	public void setTitle(String title) {
		set("title", title, true);
	}
	
	public void setAxisLabels(String xlabel, String ylabel) {
		set("xlabel", xlabel, true);
		set("ylabel", ylabel, true);
	}
	
	public void setLabel(String label, double x, double y) {
		labels.add("label \"" + label + "\" at " + x + ", " + y);
	}
	
	public void setLegendPlacing(String x, String y) {
		set("key", x + " " + y);
	}
	
	public void setLegendPlacing(double x, double y) {
		set("key", x + ", " + y);
	}
	
	public void setYMin(double min) {
		set("yrange", "[" + min + ":*]");
	}
	
	public void setXRange(double start, double end) {
		set("xrange", "[" + start + ":" + end + "]");
	}
	
	public void setXMin(double min) {
		set("xrange", "[" + min + ":*]");
	}
	
	public void doFitWithLine() {
		fittedLine = "fitted(x)";
	}
	
	public void line(double a, double b, String title) {
		line = a + "*x+" + b + " title '" + title + "'";
	}
	
	public void horizontalLine(double y, String title) {
		line = y + " title '" + title + "'";
	}
	
	public <T extends Number> void put(List<T> vector) {
		double[] v = new double[vector.size()];
		for(int i = 0; i < vector.size(); i ++)
			v[i] = vector.get(i).doubleValue();
		
		put(v);
	}
	
	// Add data.
	public void put(double... vector) {
		if(vector.length > nbOfPlots)
			throw new IllegalArgumentException(
				"The length of the vector must be less or egual to the given size!");
		else if(vector.length < nbOfPlots) {
			double[] norm = new double[nbOfPlots];
			System.arraycopy(vector, 0, norm, 0, vector.length);
			
			for(int i = vector.length; i < norm.length; i++)
				norm[i] = Double.NaN;
			
			vector = norm;
		}
		
		data.add(vector);
	}
	
	/**
	 * Executes gnuplot to create a graph. The method createPlot
	 * must be run first.
	 * @param path Path to the gnuplot executable file.
	 * @return Returns gnuplots exit value.
	 */
	public int runGnuplot(String path) throws IOException {
		Process gnuplot = Runtime.getRuntime().exec(path);
		OutputStream stdin = gnuplot.getOutputStream();

		String cmd = "cd \"" + scriptFile.getAbsoluteFile().
			getParent().replace('\\', '/') + "\"\n";
		
		stdin.write(cmd.getBytes());
		stdin.flush();
		
		cmd = "load \"" + scriptFile.getName() + "\"\n";
		stdin.write(cmd.getBytes());
		stdin.flush();
		
		cmd = "exit\n";
		stdin.write(cmd.getBytes());
		stdin.flush();
		
		stdin.close();
		
		try {
			gnuplot.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return gnuplot.exitValue();
	}
	
	/**
	 * Called to create all the necessary files so the data can be plotted
	 * in a 2 dimensional graph.
	 * The types argument specifies which type of graph to be used for each
	 * data set. If none are given the default is 'linespoints'. If the types
	 * array is smaller that the data sets, the last item is used for the 
	 * remaining sets.
	 * @param types The types to be used for each data set. E.g. points, lines or
	 * linespoints (see gnuplot manual for details).
	 */
	public void createPlot(String... types) throws IOException {
		writeData(dataFile);
		writeOptions(scriptFile, dataFile.getName(), 
				normilizeArray(types, titles.length));
	}
	
	private String[] normilizeArray(String[] strs, int length) {
		if(strs == null || strs.length == 0)
			strs = new String[] {defaultType};
		
		String[] result = new String[length];
		int len = strs.length > length ? length : strs.length;
		System.arraycopy(strs, 0, result, 0, len);
		
		for(int i = len; i < length; i++)
			result[i] = strs[len - 1];
		
		return result;
	}
	
	private void writeOptions(File file, String data, String[] types) 
			throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		
		out.write("# Plot script for " + description + ".\n");
		out.write("# Used with " + data + " data file.\n");
		out.write("# Run with the following commands:\n");
		out.write("# $> gnuplot\n");
		out.write("# $gnuplot> load '" + file.getName() + "'\n");
		
		for(String name : options.keySet())
			out.write("set " + name + " " + options.get(name) + "\n");
		
		for(String label : labels)
			out.write("set " + label + "\n");
		
		if(fittedLine != null) {
			out.write(fittedLine + "=a*x\n");
			out.write("fit " + fittedLine + " \"" + data + "\" using 1:2 via a\n");
		}
		
		out.write("plot ");
		for(int i = 1; i < types.length; i++) {
			out.write(plot(data, titles[i], types[i], "1:" + (i + 1)));
			if(i < types.length - 1)
				out.write(", ");
		}
		
		if(line != null)
			out.write(", " + line);
		
		if(fittedLine != null)
			out.write(", " + fittedLine + " title 'Average'");
		
		out.write("\n");
		out.close();
	}
	
	private String plot(String data, String title, String type, String using) {
		return "\"" + data + "\" using " + using + " title '" + 
			title + "' with " + type;
	}
	
	private void writeData(File file) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		
		out.write("# Plot data for " + description + ".\n");
		
		out.write("# ");
		String space = "                    ";
		int[] spaces = new int[nbOfPlots];
		
		for(int i = 0; i < titles.length; i++) {
			out.write(titles[i] + space);
			spaces[i] = titles[i].length() + space.length();
		}
		
		out.write("\n");
		
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator('.');
		// 5 decimal precision.
		DecimalFormat dec = new DecimalFormat("#.#####", dfs);
		
		for(double[] vector : data) {
			for(int i = 0; i < vector.length; i++) {
				String value = dec.format(vector[i]);
				int in = spaces[i] - value.length();
				
				String indent = "";
				for(int j = 0; j < in; j++)
					indent += " ";
				
				out.write(value + indent);
			}
			
			out.write("\n");
		}
		
		out.close();
	}
	
}
