package org.hhn.topicgrouper.eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hhn.topicgrouper.eval.plot.SvgLineChart;

/**
 * Draws the figures of the papers from the CSV files of the experiments into
 * the <code>figs</code> subdirectory of the result directory, as SVG files
 * named so that <code>doc/topic-grouper.md</code> and
 * <code>doc/feature-reduction.md</code> reference them: copy <code>figs</code>
 * next to those documents and the references resolve.
 *
 * <table border="1">
 * <caption>CSV files and the figures drawn from them</caption>
 * <tr><th>CSV</th><th>Figures</th></tr>
 * <tr><td><code>perplexity-&lt;corpus&gt;.csv</code></td><td><code>perplexity-&lt;corpus&gt;.svg</code></td></tr>
 * <tr><td><code>twc-alpha.csv</code></td><td><code>twc-alpha-perplexity.svg</code>, <code>twc-alpha-error.svg</code></td></tr>
 * <tr><td><code>twc-docs.csv</code></td><td><code>twc-docs-perplexity.svg</code>, <code>twc-docs-error.svg</code></td></tr>
 * <tr><td><code>performance-&lt;corpus&gt;.csv</code></td><td><code>performance-&lt;corpus&gt;.svg</code></td></tr>
 * <tr><td><code>classification-&lt;corpus&gt;.csv</code></td><td><code>classification-&lt;corpus&gt;-micro.svg</code>, <code>classification-&lt;corpus&gt;-macro.svg</code></td></tr>
 * <tr><td><code>homonyms.csv</code></td><td><code>homonyms-error.svg</code>, <code>homonyms-perplexity.svg</code></td></tr>
 * </table>
 * The hierarchy drawings <code>hierarchy-&lt;corpus&gt;.svg</code> are written
 * to the same directory by the topics experiment. The experiments call
 * {@link #plot(File)} when they close a CSV file; {@link #plotAll()} (or the
 * command line, <code>Figures [results directory]</code>) redraws everything.
 */
public class Figures {
	/** Name of the figure directory below the result directory. */
	public static final String DIRECTORY = "figs";

	private static final Map<String, String> LABELS = new LinkedHashMap<String, String>();
	static {
		LABELS.put("tg", "Topic Grouper");
		LABELS.put("tg-sense", "Topic Grouper with senses");
		LABELS.put("lda", "LDA");
		LABELS.put("lda-avg", "LDA, averaging estimate");
		LABELS.put("lda-opt", "LDA, alpha and beta optimized");
		LABELS.put("lda-opt-avg", "LDA, optimized, averaging estimate");
		LABELS.put("lda-alpha", "LDA, alpha vector optimized");
		LABELS.put("plsa", "pLSI");
		LABELS.put("unigram", "unigram model");
		LABELS.put("perfect", "true topics");
		LABELS.put("ehac", "EHAC, 1 thread");
		LABELS.put("lowmem", "LowMem, 1 thread");
		LABELS.put("ehac-mt", "EHAC, all processors");
		LABELS.put("lowmem-mt", "LowMem, all processors");
		LABELS.put("tg-nb", "TG topics, naive Bayes");
		LABELS.put("tg-nb-smoothed", "TG topics, naive Bayes, smoothed");
		LABELS.put("tg-tfidf", "TG topics, tf-idf");
		LABELS.put("lda-nb", "LDA topics, naive Bayes");
		LABELS.put("lda-nb-opt", "LDA topics (optimized), naive Bayes");
		LABELS.put("vocab-ig", "words by information gain, naive Bayes");
		LABELS.put("vocab-df", "words by document frequency, naive Bayes");
	}

	private final File out;
	private final File figs;

	/**
	 * Creates the figure writer for a result directory.
	 *
	 * @param out
	 *            the result directory holding the CSV files; the figures go to
	 *            its <code>figs</code> subdirectory
	 */
	public Figures(File out) {
		this.out = out;
		this.figs = new File(out, DIRECTORY);
	}

	/**
	 * The figure directory.
	 *
	 * @return <code>&lt;results&gt;/figs</code> (created on demand)
	 */
	public File getDirectory() {
		figs.mkdirs();
		return figs;
	}

	/**
	 * Draws the figures that belong to a CSV file; unknown files are ignored.
	 *
	 * @param csv
	 *            the CSV file written by an experiment
	 * @throws IOException
	 *             on read or write failure
	 */
	public void plot(File csv) throws IOException {
		String name = csv.getName();
		if (!name.endsWith(".csv")) {
			return;
		}
		String base = name.substring(0, name.length() - 4);
		if (base.startsWith("perplexity-")) {
			perplexity(csv, base.substring("perplexity-".length()));
		} else if (base.equals("twc-alpha")) {
			twcSeries(csv, "alpha1", "Dirichlet prior alpha_1 of topic 1", "twc-alpha");
		} else if (base.equals("twc-docs")) {
			twcSeries(csv, "docs", "number of documents", "twc-docs");
		} else if (base.startsWith("performance-")) {
			performance(csv, base.substring("performance-".length()));
		} else if (base.startsWith("classification-")) {
			classification(csv, base.substring("classification-".length()));
		} else if (base.equals("homonyms")) {
			homonyms(csv);
		}
	}

	/**
	 * Draws the figures of all CSV files in the result directory.
	 *
	 * @return the number of CSV files processed
	 * @throws IOException
	 *             on read or write failure
	 */
	public int plotAll() throws IOException {
		File[] files = out.listFiles();
		int n = 0;
		if (files != null) {
			Arrays.sort(files);
			for (File f : files) {
				if (f.getName().endsWith(".csv") && !f.getName().startsWith("topics-")) {
					plot(f);
					n++;
				}
			}
		}
		return n;
	}

	private void perplexity(File csv, String corpus) throws IOException {
		Table t = Table.read(csv);
		SvgLineChart chart = new SvgLineChart("number of topics", "hold-out perplexity");
		addSeries(chart, t, "method", "topics", "perplexity", null);
		write(chart, "perplexity-" + corpus);
	}

	private void twcSeries(File csv, String xColumn, String xLabel, String prefix) throws IOException {
		Table t = Table.read(csv);
		SvgLineChart p = new SvgLineChart(xLabel, "hold-out perplexity");
		addSeries(p, t, "method", xColumn, "perplexity", "perplexityStdDev");
		write(p, prefix + "-perplexity");
		SvgLineChart e = new SvgLineChart(xLabel, "error rate");
		addSeries(e, t, "method", xColumn, "errorRate", "errorRateStdDev");
		write(e, prefix + "-error");
	}

	private void performance(File csv, String corpus) throws IOException {
		Table t = Table.read(csv);
		SvgLineChart chart = new SvgLineChart("number of documents", "run time in seconds");
		addSeries(chart, t, "solver", "docs", "avgSeconds", "stdDevSeconds");
		write(chart, "performance-" + corpus);
	}

	private void classification(File csv, String corpus) throws IOException {
		Table t = Table.read(csv);
		SvgLineChart micro = new SvgLineChart("number of features (topics or words)", "accuracy (micro average)")
				.setLogX(true);
		addSeries(micro, t, "method", "topics", "microAvg", null);
		write(micro, "classification-" + corpus + "-micro");
		SvgLineChart macro = new SvgLineChart("number of features (topics or words)", "accuracy (macro average)")
				.setLogX(true);
		addSeries(macro, t, "method", "topics", "macroAvg", null);
		write(macro, "classification-" + corpus + "-macro");
	}

	private void homonyms(File csv) throws IOException {
		Table t = Table.read(csv);
		SvgLineChart e = new SvgLineChart("number of homonyms", "error rate");
		addSeries(e, t, "method", "homonyms", "errorRate", null);
		write(e, "homonyms-error");
		SvgLineChart p = new SvgLineChart("number of homonyms", "hold-out perplexity");
		addSeries(p, t, "method", "homonyms", "perplexity", null);
		write(p, "homonyms-perplexity");
	}

	private void write(SvgLineChart chart, String name) throws IOException {
		if (chart.getNumberOfSeries() > 0) {
			chart.write(new File(getDirectory(), name + ".svg"));
		}
	}

	/**
	 * Adds one series per distinct value of the series column, points sorted by x.
	 */
	private void addSeries(SvgLineChart chart, Table t, String seriesColumn, String xColumn, String yColumn,
			String errorColumn) {
		int sc = t.column(seriesColumn);
		int xc = t.column(xColumn);
		int yc = t.column(yColumn);
		int ec = errorColumn == null ? -1 : t.column(errorColumn);
		if (sc < 0 || xc < 0 || yc < 0) {
			return;
		}
		Map<String, List<double[]>> bySeries = new LinkedHashMap<String, List<double[]>>();
		for (String[] row : t.rows) {
			List<double[]> points = bySeries.get(row[sc]);
			if (points == null) {
				points = new ArrayList<double[]>();
				bySeries.put(row[sc], points);
			}
			points.add(new double[] { number(row[xc]), number(row[yc]), ec < 0 ? Double.NaN : number(row[ec]) });
		}
		for (Map.Entry<String, List<double[]>> e : bySeries.entrySet()) {
			List<double[]> points = e.getValue();
			Collections.sort(points, new Comparator<double[]>() {
				public int compare(double[] a, double[] b) {
					return Double.compare(a[0], b[0]);
				}
			});
			double[] x = new double[points.size()];
			double[] y = new double[points.size()];
			double[] err = new double[points.size()];
			boolean anyErr = false;
			for (int i = 0; i < x.length; i++) {
				x[i] = points.get(i)[0];
				y[i] = points.get(i)[1];
				err[i] = points.get(i)[2];
				anyErr |= !Double.isNaN(err[i]) && err[i] > 0;
			}
			String label = LABELS.containsKey(e.getKey()) ? LABELS.get(e.getKey()) : e.getKey();
			chart.add(new SvgLineChart.Series(label, x, y, anyErr ? err : null));
		}
	}

	private static double number(String s) {
		try {
			return Double.parseDouble(s.trim());
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	/** A CSV file as written by {@link Csv}: a header and rows of fields. */
	private static class Table {
		final String[] header;
		final List<String[]> rows = new ArrayList<String[]>();

		Table(String[] header) {
			this.header = header;
		}

		int column(String name) {
			for (int i = 0; i < header.length; i++) {
				if (header[i].trim().equals(name)) {
					return i;
				}
			}
			return -1;
		}

		static Table read(File file) throws IOException {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),
					Charset.forName("UTF-8")));
			try {
				String line = in.readLine();
				if (line == null) {
					throw new IOException("empty file " + file);
				}
				Table t = new Table(line.split(";", -1));
				while ((line = in.readLine()) != null) {
					if (line.trim().length() > 0) {
						t.rows.add(line.split(";", -1));
					}
				}
				return t;
			} finally {
				in.close();
			}
		}
	}

	/**
	 * Draws all figures of a result directory.
	 *
	 * @param args
	 *            the result directory (default <code>results</code>)
	 * @throws IOException
	 *             on read or write failure
	 */
	public static void main(String[] args) throws IOException {
		File out = new File(args.length > 0 ? args[0] : "results");
		Figures figures = new Figures(out);
		int n = figures.plotAll();
		System.out.println(n + " CSV files plotted into " + figures.getDirectory());
	}
}
