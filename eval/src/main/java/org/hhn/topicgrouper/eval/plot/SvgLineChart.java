package org.hhn.topicgrouper.eval.plot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A line chart with markers, optional error bars, linear or logarithmic axes
 * and a legend, written as a self-contained SVG file. The chart is drawn to
 * scale with "nice" tick values, in a colorblind safe palette (Okabe and Ito)
 * with a different marker per series, so that it also reads in grey scale.
 * Used for the figures of the evaluation.
 */
public class SvgLineChart {
	/** The colors of the series (Okabe and Ito palette). */
	private static final String[] COLORS = { "#0072b2", "#d55e00", "#009e73", "#cc79a7", "#e69f00", "#56b4e9",
			"#000000", "#f0e442" };
	/** The markers of the series. */
	private static final String[] MARKERS = { "circle", "square", "triangle", "diamond", "cross", "down", "plus",
			"circle" };

	/** One line of the chart. */
	public static class Series {
		private final String name;
		private final double[] x;
		private final double[] y;
		private final double[] error;

		/**
		 * Creates a series.
		 *
		 * @param name
		 *            the legend entry
		 * @param x
		 *            the x values (ascending)
		 * @param y
		 *            the y values
		 * @param error
		 *            the half lengths of the error bars, <code>null</code> for none
		 */
		public Series(String name, double[] x, double[] y, double[] error) {
			if (x.length != y.length || (error != null && error.length != y.length)) {
				throw new IllegalArgumentException("series " + name + ": lengths differ");
			}
			this.name = name;
			this.x = x;
			this.y = y;
			this.error = error;
		}
	}

	private final String xLabel;
	private final String yLabel;
	private final List<Series> series = new ArrayList<Series>();
	private boolean logX;
	private boolean logY;
	private int width = 560;
	private int height = 340;
	private String title;
	private Double yMin;
	private Double yMax;

	/**
	 * Creates an empty chart.
	 *
	 * @param xLabel
	 *            the label of the x axis
	 * @param yLabel
	 *            the label of the y axis
	 */
	public SvgLineChart(String xLabel, String yLabel) {
		this.xLabel = xLabel;
		this.yLabel = yLabel;
	}

	/**
	 * Adds a series.
	 *
	 * @param s
	 *            the series
	 * @return this chart
	 */
	public SvgLineChart add(Series s) {
		series.add(s);
		return this;
	}

	/**
	 * Adds a series without error bars.
	 *
	 * @param name
	 *            the legend entry
	 * @param x
	 *            the x values
	 * @param y
	 *            the y values
	 * @return this chart
	 */
	public SvgLineChart add(String name, double[] x, double[] y) {
		return add(new Series(name, x, y, null));
	}

	/**
	 * Sets a logarithmic x axis.
	 *
	 * @param logX
	 *            true for logarithmic
	 * @return this chart
	 */
	public SvgLineChart setLogX(boolean logX) {
		this.logX = logX;
		return this;
	}

	/**
	 * Sets a logarithmic y axis.
	 *
	 * @param logY
	 *            true for logarithmic
	 * @return this chart
	 */
	public SvgLineChart setLogY(boolean logY) {
		this.logY = logY;
		return this;
	}

	/**
	 * Sets the size of the plot area (the legend is added to the right).
	 *
	 * @param width
	 *            the width in pixels
	 * @param height
	 *            the height in pixels
	 * @return this chart
	 */
	public SvgLineChart setSize(int width, int height) {
		this.width = width;
		this.height = height;
		return this;
	}

	/**
	 * Sets a title above the plot.
	 *
	 * @param title
	 *            the title, <code>null</code> for none
	 * @return this chart
	 */
	public SvgLineChart setTitle(String title) {
		this.title = title;
		return this;
	}

	/**
	 * Fixes the range of the y axis (by default it is derived from the data).
	 *
	 * @param min
	 *            the lower end, <code>null</code> for automatic
	 * @param max
	 *            the upper end, <code>null</code> for automatic
	 * @return this chart
	 */
	public SvgLineChart setYRange(Double min, Double max) {
		this.yMin = min;
		this.yMax = max;
		return this;
	}

	/**
	 * The number of series.
	 *
	 * @return the number of series added
	 */
	public int getNumberOfSeries() {
		return series.size();
	}

	/**
	 * Writes the chart as an SVG file (UTF-8).
	 *
	 * @param file
	 *            the file
	 * @throws IOException
	 *             on write failure
	 */
	public void write(File file) throws IOException {
		Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")));
		try {
			write(out);
		} finally {
			out.close();
		}
	}

	/** A linear or logarithmic axis scale. */
	private static class Axis {
		final boolean log;
		final double min;
		final double max;
		final double[] ticks;

		Axis(double dataMin, double dataMax, boolean log, Double fixedMin, Double fixedMax) {
			this.log = log;
			if (log) {
				double lo = fixedMin != null ? fixedMin : dataMin;
				double hi = fixedMax != null ? fixedMax : dataMax;
				if (!(lo > 0)) {
					lo = Math.max(1e-9, hi / 1000);
				}
				if (!(hi > lo)) {
					hi = lo * 10;
				}
				double[] t = logTicks(lo, hi);
				min = fixedMin != null ? fixedMin : Math.min(lo, t[0]);
				max = fixedMax != null ? fixedMax : Math.max(hi, t[t.length - 1]);
				ticks = t;
			} else {
				double lo = fixedMin != null ? fixedMin : dataMin;
				double hi = fixedMax != null ? fixedMax : dataMax;
				if (!(hi > lo)) {
					hi = lo + 1;
				}
				double step = niceStep((hi - lo) / 5);
				double tMin = fixedMin != null ? lo : Math.floor(lo / step) * step;
				double tMax = fixedMax != null ? hi : Math.ceil(hi / step) * step;
				List<Double> t = new ArrayList<Double>();
				for (double v = tMin; v <= tMax + step / 1000; v += step) {
					t.add(Math.abs(v) < step / 1e6 ? 0 : v);
				}
				ticks = new double[t.size()];
				for (int i = 0; i < ticks.length; i++) {
					ticks[i] = t.get(i);
				}
				min = tMin;
				max = tMax;
			}
		}

		double position(double v) {
			if (log) {
				return (Math.log(v) - Math.log(min)) / (Math.log(max) - Math.log(min));
			}
			return (v - min) / (max - min);
		}

		private static double niceStep(double raw) {
			double e = Math.pow(10, Math.floor(Math.log10(raw)));
			double f = raw / e;
			double n = f <= 1 ? 1 : f <= 2 ? 2 : f <= 2.5 ? 2.5 : f <= 5 ? 5 : 10;
			return n * e;
		}

		private static double[] logTicks(double lo, double hi) {
			List<Double> t = new ArrayList<Double>();
			int eLo = (int) Math.floor(Math.log10(lo));
			int eHi = (int) Math.ceil(Math.log10(hi));
			boolean fine = Math.log10(hi / lo) < 2.5;
			for (int e = eLo; e <= eHi; e++) {
				for (double m : fine ? new double[] { 1, 2, 5 } : new double[] { 1 }) {
					double v = m * Math.pow(10, e);
					if (v >= lo / 1.0001 && v <= hi * 1.0001) {
						t.add(v);
					}
				}
			}
			if (t.size() < 2) {
				t.clear();
				t.add(lo);
				t.add(hi);
			}
			double[] res = new double[t.size()];
			for (int i = 0; i < res.length; i++) {
				res[i] = t.get(i);
			}
			return res;
		}
	}

	/**
	 * Writes the chart.
	 *
	 * @param out
	 *            the writer (not closed)
	 * @throws IOException
	 *             on write failure
	 */
	public void write(Writer out) throws IOException {
		double xMinD = Double.POSITIVE_INFINITY, xMaxD = Double.NEGATIVE_INFINITY;
		double yMinD = Double.POSITIVE_INFINITY, yMaxD = Double.NEGATIVE_INFINITY;
		for (Series s : series) {
			for (int i = 0; i < s.x.length; i++) {
				if (Double.isNaN(s.y[i]) || Double.isInfinite(s.y[i]) || (logY && s.y[i] <= 0)
						|| (logX && s.x[i] <= 0)) {
					continue;
				}
				xMinD = Math.min(xMinD, s.x[i]);
				xMaxD = Math.max(xMaxD, s.x[i]);
				double e = s.error == null || Double.isNaN(s.error[i]) ? 0 : s.error[i];
				yMinD = Math.min(yMinD, logY ? s.y[i] : s.y[i] - e);
				yMaxD = Math.max(yMaxD, s.y[i] + e);
			}
		}
		if (xMinD == Double.POSITIVE_INFINITY) {
			xMinD = 0;
			xMaxD = 1;
			yMinD = 0;
			yMaxD = 1;
		}
		Axis xAxis = new Axis(xMinD, xMaxD, logX, null, null);
		Axis yAxis = new Axis(yMinD, yMaxD, logY, yMin, yMax);

		int left = 64, right = 16, top = title == null ? 16 : 36, bottom = 48;
		int legendWidth = 0;
		for (Series s : series) {
			legendWidth = Math.max(legendWidth, 30 + (int) (s.name.length() * 6.6));
		}
		int totalWidth = left + width + right + legendWidth + 10;
		int totalHeight = top + height + bottom;
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append(String.format(Locale.US,
				"<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\" font-family=\"sans-serif\" font-size=\"12\">\n",
				totalWidth, totalHeight, totalWidth, totalHeight));
		sb.append(String.format(Locale.US, "<rect x=\"0\" y=\"0\" width=\"%d\" height=\"%d\" fill=\"#ffffff\"/>\n",
				totalWidth, totalHeight));
		if (title != null) {
			sb.append(String.format(Locale.US,
					"<text x=\"%d\" y=\"20\" text-anchor=\"middle\" font-size=\"14\" font-weight=\"bold\" fill=\"#000000\">%s</text>\n",
					left + width / 2, escape(title)));
		}
		// Grid and ticks.
		for (double t : yAxis.ticks) {
			double y = top + height - yAxis.position(t) * height;
			sb.append(String.format(Locale.US,
					"<line x1=\"%d\" y1=\"%.1f\" x2=\"%d\" y2=\"%.1f\" stroke=\"#dddddd\" stroke-width=\"1\"/>\n", left,
					y, left + width, y));
			sb.append(String.format(Locale.US,
					"<text x=\"%d\" y=\"%.1f\" text-anchor=\"end\" dominant-baseline=\"middle\" fill=\"#000000\">%s</text>\n",
					left - 8, y, format(t)));
		}
		for (double t : xAxis.ticks) {
			double x = left + xAxis.position(t) * width;
			sb.append(String.format(Locale.US,
					"<line x1=\"%.1f\" y1=\"%d\" x2=\"%.1f\" y2=\"%d\" stroke=\"#dddddd\" stroke-width=\"1\"/>\n", x,
					top, x, top + height));
			sb.append(String.format(Locale.US,
					"<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\" fill=\"#000000\">%s</text>\n", x,
					top + height + 18, format(t)));
		}
		// Frame.
		sb.append(String.format(Locale.US,
				"<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" fill=\"none\" stroke=\"#000000\" stroke-width=\"1\"/>\n",
				left, top, width, height));
		// Axis labels.
		sb.append(String.format(Locale.US,
				"<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" fill=\"#000000\">%s</text>\n", left + width / 2,
				top + height + 38, escape(xLabel)));
		sb.append(String.format(Locale.US,
				"<text transform=\"translate(%d,%d) rotate(-90)\" text-anchor=\"middle\" fill=\"#000000\">%s</text>\n",
				16, top + height / 2, escape(yLabel)));
		// Series.
		for (int k = 0; k < series.size(); k++) {
			Series s = series.get(k);
			String color = COLORS[k % COLORS.length];
			String marker = MARKERS[k % MARKERS.length];
			StringBuilder path = new StringBuilder();
			StringBuilder marks = new StringBuilder();
			boolean first = true;
			for (int i = 0; i < s.x.length; i++) {
				if (Double.isNaN(s.y[i]) || Double.isInfinite(s.y[i]) || (logY && s.y[i] <= 0)
						|| (logX && s.x[i] <= 0)) {
					first = true;
					continue;
				}
				double x = left + xAxis.position(s.x[i]) * width;
				double y = top + height - clamp(yAxis.position(s.y[i])) * height;
				path.append(first ? "M" : "L").append(String.format(Locale.US, "%.1f %.1f ", x, y));
				first = false;
				if (s.error != null && !Double.isNaN(s.error[i]) && s.error[i] > 0) {
					double yLo = top + height - clamp(yAxis.position(s.y[i] - s.error[i])) * height;
					double yHi = top + height - clamp(yAxis.position(s.y[i] + s.error[i])) * height;
					if (logY && s.y[i] - s.error[i] <= 0) {
						yLo = top + height;
					}
					marks.append(String.format(Locale.US,
							"<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"%s\" stroke-width=\"1\"/>",
							x, yLo, x, yHi, color));
					marks.append(String.format(Locale.US,
							"<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"%s\" stroke-width=\"1\"/>",
							x - 3, yLo, x + 3, yLo, color));
					marks.append(String.format(Locale.US,
							"<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"%s\" stroke-width=\"1\"/>\n",
							x - 3, yHi, x + 3, yHi, color));
				}
				marks.append(markerSvg(marker, x, y, color)).append('\n');
			}
			if (path.length() > 0) {
				sb.append(String.format(Locale.US,
						"<path d=\"%s\" fill=\"none\" stroke=\"%s\" stroke-width=\"1.6\" stroke-linejoin=\"round\"/>\n",
						path.toString().trim(), color));
			}
			sb.append(marks);
			// Legend entry.
			int lx = left + width + right;
			int ly = top + 10 + k * 20;
			sb.append(String.format(Locale.US,
					"<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"%s\" stroke-width=\"1.6\"/>\n", lx, ly,
					lx + 22, ly, color));
			sb.append(markerSvg(marker, lx + 11, ly, color)).append('\n');
			sb.append(String.format(Locale.US,
					"<text x=\"%d\" y=\"%d\" dominant-baseline=\"middle\" fill=\"#000000\">%s</text>\n", lx + 28, ly,
					escape(s.name)));
		}
		sb.append("</svg>\n");
		out.write(sb.toString());
	}

	private static double clamp(double p) {
		return p < 0 ? 0 : (p > 1 ? 1 : p);
	}

	private static String markerSvg(String marker, double x, double y, String color) {
		double r = 3.2;
		if (marker.equals("circle")) {
			return String.format(Locale.US, "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"%.1f\" fill=\"%s\"/>", x, y, r, color);
		}
		if (marker.equals("square")) {
			return String.format(Locale.US, "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"%s\"/>",
					x - r, y - r, 2 * r, 2 * r, color);
		}
		if (marker.equals("triangle")) {
			return String.format(Locale.US, "<polygon points=\"%.1f,%.1f %.1f,%.1f %.1f,%.1f\" fill=\"%s\"/>", x,
					y - r - 1, x - r - 1, y + r, x + r + 1, y + r, color);
		}
		if (marker.equals("down")) {
			return String.format(Locale.US, "<polygon points=\"%.1f,%.1f %.1f,%.1f %.1f,%.1f\" fill=\"%s\"/>", x,
					y + r + 1, x - r - 1, y - r, x + r + 1, y - r, color);
		}
		if (marker.equals("diamond")) {
			return String.format(Locale.US, "<polygon points=\"%.1f,%.1f %.1f,%.1f %.1f,%.1f %.1f,%.1f\" fill=\"%s\"/>",
					x, y - r - 1, x + r + 1, y, x, y + r + 1, x - r - 1, y, color);
		}
		if (marker.equals("cross")) {
			return String.format(Locale.US,
					"<path d=\"M%.1f %.1f L%.1f %.1f M%.1f %.1f L%.1f %.1f\" stroke=\"%s\" stroke-width=\"1.8\" fill=\"none\"/>",
					x - r, y - r, x + r, y + r, x - r, y + r, x + r, y - r, color);
		}
		return String.format(Locale.US,
				"<path d=\"M%.1f %.1f L%.1f %.1f M%.1f %.1f L%.1f %.1f\" stroke=\"%s\" stroke-width=\"1.8\" fill=\"none\"/>",
				x - r - 1, y, x + r + 1, y, x, y - r - 1, x, y + r + 1, color);
	}

	/**
	 * Formats a tick value without superfluous digits.
	 *
	 * @param v
	 *            the value
	 * @return the label
	 */
	static String format(double v) {
		if (v == Math.rint(v) && Math.abs(v) < 1e15) {
			return String.format(Locale.US, "%d", (long) v);
		}
		String s = String.format(Locale.US, "%.6f", v);
		s = s.replaceAll("0+$", "");
		if (s.endsWith(".")) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	private static String escape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
