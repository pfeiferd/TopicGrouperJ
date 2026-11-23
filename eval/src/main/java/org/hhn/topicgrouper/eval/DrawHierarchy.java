package org.hhn.topicgrouper.eval;

import java.awt.Font;
import java.io.File;
import java.io.IOException;

import org.hhn.topicgrouper.tg.paint.DefaultTopicTreePainter;
import org.hhn.topicgrouper.tg.report.store.TopicHierarchy;
import org.hhn.topicgrouper.tg.report.store.TopicHierarchyJsonReader;
import org.hhn.topicgrouper.tg.report.store.TopicHierarchyObjectReader;

/**
 * Command line tool that draws a recorded topic hierarchy (a
 * <code>.json</code> / <code>.json.gz</code> file of the topic examples or a
 * serialized node list) as an SVG file, with the pruning and label options of
 * {@link DefaultTopicTreePainter}:
 *
 * <pre>
 * DrawHierarchy &lt;hierarchy file&gt; &lt;svg file&gt; [--topics N] [--min-size N] [--max-depth N]
 *     [--top-words N] [--font-size N] [--cost] [--likelihood] [--no-ids] [--no-word-count] [--indent]
 *     [--dashed COST]
 * </pre>
 *
 * <code>--topics N</code> stops at the solution with N topics (default: the
 * whole hierarchy), <code>--min-size N</code> collapses topics with fewer
 * words, <code>--max-depth N</code> collapses nodes at that depth,
 * <code>--top-words N</code> sets the words per topic label (5),
 * <code>--font-size N</code> the font size (12), <code>--cost</code> and
 * <code>--likelihood</code> add the join cost and the log likelihood to the
 * labels, <code>--no-ids</code> omits the topic ids, <code>--no-word-count</code>
 * omits the number of words from the labels of expanded topics, <code>--indent</code>
 * indents nodes by their join cost and <code>--dashed COST</code> dashes the
 * connectors of joins costing more than COST (lost log likelihood per
 * occurrence).
 */
public class DrawHierarchy {
	private final File input;
	private final File output;
	private int topics;
	private int minTopicSize = 1;
	private int maxDepth;
	private int topWords = 5;
	private int fontSize = 12;
	private boolean showCost;
	private boolean showLikelihood;
	private boolean showIds = true;
	private boolean showWordCount = true;
	private boolean indent;
	private double dashed = Double.POSITIVE_INFINITY;

	/**
	 * Creates the tool with the default options.
	 *
	 * @param input
	 *            the hierarchy file (JSON, optionally gzip compressed, or a
	 *            serialized node list)
	 * @param output
	 *            the SVG file to write
	 */
	public DrawHierarchy(File input, File output) {
		this.input = input;
		this.output = output;
	}

	/**
	 * Applies the command line options (see the class comment).
	 *
	 * @param args
	 *            the options
	 * @param from
	 *            the index of the first option in <code>args</code>
	 * @throws IllegalArgumentException
	 *             on an unknown option or a missing value
	 */
	public void setOptions(String[] args, int from) {
		for (int i = from; i < args.length; i++) {
			String a = args[i];
			boolean hasValue = i + 1 < args.length;
			if (a.equals("--topics") && hasValue) {
				topics = Integer.parseInt(args[++i]);
			} else if (a.equals("--min-size") && hasValue) {
				minTopicSize = Integer.parseInt(args[++i]);
			} else if (a.equals("--max-depth") && hasValue) {
				maxDepth = Integer.parseInt(args[++i]);
			} else if (a.equals("--top-words") && hasValue) {
				topWords = Integer.parseInt(args[++i]);
			} else if (a.equals("--font-size") && hasValue) {
				fontSize = Integer.parseInt(args[++i]);
			} else if (a.equals("--dashed") && hasValue) {
				dashed = Double.parseDouble(args[++i]);
			} else if (a.equals("--cost")) {
				showCost = true;
			} else if (a.equals("--likelihood")) {
				showLikelihood = true;
			} else if (a.equals("--no-ids")) {
				showIds = false;
			} else if (a.equals("--no-word-count")) {
				showWordCount = false;
			} else if (a.equals("--indent")) {
				indent = true;
			} else {
				throw new IllegalArgumentException("unknown option or missing value: " + a);
			}
		}
	}

	/**
	 * Reads the hierarchy.
	 *
	 * @return the hierarchy
	 * @throws IOException
	 *             on read failure
	 */
	public TopicHierarchy<String> read() throws IOException {
		String name = input.getName();
		if (name.endsWith(".json") || name.endsWith(".json.gz")) {
			return new TopicHierarchyJsonReader().read(input);
		}
		return new TopicHierarchyObjectReader<String>().read(input);
	}

	/**
	 * Creates the painter configured by the options.
	 *
	 * @param hierarchy
	 *            the hierarchy to draw
	 * @return the painter
	 */
	public DefaultTopicTreePainter<String> createPainter(TopicHierarchy<String> hierarchy) {
		return new DefaultTopicTreePainter<String>(hierarchy).setTopics(topics).setMinTopicSize(minTopicSize)
				.setMaxDepth(maxDepth).setTopWords(topWords).setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize))
				.setLabelParts(showIds, true, showCost, showLikelihood).setShowTopicSize(showWordCount)
				.setNodeIndent(indent ? 3 : 0, true)
				.setDashedCostThreshold(dashed);
	}

	/**
	 * Reads the hierarchy and writes the drawing.
	 *
	 * @throws IOException
	 *             on read or write failure
	 */
	public void run() throws IOException {
		TopicHierarchy<String> hierarchy = read();
		createPainter(hierarchy).writeSvg(output);
	}

	/**
	 * Runs the tool.
	 *
	 * @param args
	 *            see the class comment
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("usage: DrawHierarchy <hierarchy file> <svg file> [--topics N] [--min-size N]"
					+ " [--max-depth N] [--top-words N] [--font-size N] [--cost] [--likelihood] [--no-ids]"
					+ " [--no-word-count] [--indent] [--dashed COST]");
			System.exit(2);
		}
		DrawHierarchy tool = new DrawHierarchy(new File(args[0]), new File(args[1]));
		try {
			tool.setOptions(args, 2);
			tool.run();
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			System.exit(2);
		} catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}
	}
}
