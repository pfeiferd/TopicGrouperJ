package org.hhn.topicgrouper.tg.paint;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hhn.topicgrouper.tg.report.store.MapNode;
import org.hhn.topicgrouper.tg.report.store.TopicHierarchy;
import org.hhn.topicgrouper.tg.report.store.WordInfo;

/**
 * Renders a {@link TopicHierarchy} as an indented, connected line drawing onto a
 * {@link TreeCanvas}: one line per node, labelled with its id, top words and
 * frequency, the lines of a node's children indented below it and connected to
 * it. Per-node indentation and dashing reflect the <em>join cost</em> of a
 * topic, i.e. the log likelihood lost per word occurrence by the join that
 * created it. The drawing can be pruned to the solution with a given number of
 * topics, to a minimum topic size or to a maximum depth; pruned topics appear as
 * a single line with their top words. Concrete subclasses supply the layout and
 * rendering parameters via {@link #initParams()}, {@link DefaultTopicTreePainter}
 * has setters for all of them. The traversal does not recurse and every node
 * costs a constant number of drawing primitives, so deep hierarchies are no
 * problem.
 *
 * @param <T>
 *            the word type
 */
public abstract class TopicTreePainter<T> {
	private static final DecimalFormat DF = new DecimalFormat("0.000", new DecimalFormatSymbols(Locale.US));

	private final TopicHierarchy<T> hierarchy;
	private final List<MapNode<T>> roots;

	/** The font of word leaves. */
	protected Font font;
	/** The font of topic (inner) nodes. */
	protected Font topicFont;
	/** The font of sense leaves. */
	protected Font senseFont;
	/** The vertical spacing between lines, as a multiple of the font height. */
	protected double spacingFactor;
	/** The standard horizontal indent per level, as a multiple of the line height. */
	protected double indentFactor;
	/** The horizontal gap between a node's connector line and its text, as a multiple of the font height. */
	protected double textGapFactor;
	/** The maximum extra indentation of a node, as a multiple of the line height (0: none). */
	protected double nodeIndentFactor;
	/**
	 * Whether the extra indentation of a node reflects its join cost (true) or
	 * the share of the corpus it does not cover (false, small topics are indented
	 * more).
	 */
	protected boolean costIndent;
	/** Join cost (lost log likelihood per occurrence) above which a node's connector line is dashed. */
	protected double dashedCostThreshold;
	/** Whether the connector lines of marked nodes are drawn in {@link #markColor}. */
	protected boolean showMarks;
	/** Whether the node id precedes the label of a topic. */
	protected boolean showId;
	/** Whether the frequency is appended to the label. */
	protected boolean showFrequency;
	/** Whether the number of words of a topic is appended to its label (collapsed topics always show it). */
	protected boolean showTopicSize;
	/** Whether the join cost is appended to the label of a topic. */
	protected boolean showCost;
	/** Whether the topic log likelihood is appended to the label of a topic. */
	protected boolean showLikelihood;
	/** The maximum number of top words in the label of a topic. */
	protected int topWords;
	/** The drawing stops at the solution with this number of topics (0: the whole hierarchy). */
	protected int topics;
	/** Topics with fewer words are drawn as one line (1: every word gets a line). */
	protected int minTopicSize;
	/** Nodes at this depth are drawn as one line (0: no limit). */
	protected int maxDepth;
	/** The color of the connector lines. */
	protected Color lineColor;
	/** The color of the connector lines of marked nodes. */
	protected Color markColor;
	/** The text color. */
	protected Color textColor;
	/** The background behind the text (so that connector lines do not cross it). */
	protected Color background;
	/** The margin around the drawing in pixels. */
	protected int margin;

	/**
	 * Creates a painter for the whole hierarchy.
	 *
	 * @param hierarchy
	 *            the hierarchy to paint
	 */
	public TopicTreePainter(TopicHierarchy<T> hierarchy) {
		this(hierarchy, hierarchy.getRoots());
	}

	/**
	 * Creates a painter for some subtrees of a hierarchy.
	 *
	 * @param hierarchy
	 *            the hierarchy
	 * @param roots
	 *            the nodes whose subtrees are painted, one below the other
	 */
	public TopicTreePainter(TopicHierarchy<T> hierarchy, List<MapNode<T>> roots) {
		this.hierarchy = hierarchy;
		this.roots = new ArrayList<MapNode<T>>(roots);
		initParams();
	}

	/**
	 * Initializes the layout, font and rendering parameters. Called from the constructor.
	 */
	protected abstract void initParams();

	/**
	 * The hierarchy being painted.
	 *
	 * @return the hierarchy
	 */
	public TopicHierarchy<T> getHierarchy() {
		return hierarchy;
	}

	/**
	 * A line of the drawing.
	 *
	 * @param <T>
	 *            the word type
	 */
	protected static class Row<T> {
		/** The node. */
		public final MapNode<T> node;
		/** Its depth below the painted root. */
		public final int level;
		/** Whether it is the last child of its parent. */
		public final boolean lastChild;
		/** Whether its children are not drawn. */
		public final boolean collapsed;

		Row(MapNode<T> node, int level, boolean lastChild, boolean collapsed) {
			this.node = node;
			this.level = level;
			this.lastChild = lastChild;
			this.collapsed = collapsed;
		}
	}

	/**
	 * Computes the lines of the drawing: a pre-order traversal of the subtrees
	 * with the pruning rules applied.
	 *
	 * @return the rows top down
	 */
	protected List<Row<T>> createRows() {
		Set<MapNode<T>> frontier = Collections.newSetFromMap(new IdentityHashMap<MapNode<T>, Boolean>());
		if (topics > 0 && topics <= hierarchy.getMaxTopics()) {
			frontier.addAll(hierarchy.getTopicsAt(topics));
		}
		List<Row<T>> rows = new ArrayList<Row<T>>();
		List<Row<T>> stack = new ArrayList<Row<T>>();
		for (int r = roots.size() - 1; r >= 0; r--) {
			stack.add(createRow(roots.get(r), 0, r == roots.size() - 1, frontier));
		}
		while (!stack.isEmpty()) {
			Row<T> row = stack.remove(stack.size() - 1);
			rows.add(row);
			if (!row.collapsed) {
				List<MapNode<T>> children = hierarchy.children(row.node);
				for (int i = children.size() - 1; i >= 0; i--) {
					stack.add(createRow(children.get(i), row.level + 1, i == children.size() - 1, frontier));
				}
			}
		}
		return rows;
	}

	private Row<T> createRow(MapNode<T> node, int level, boolean lastChild, Set<MapNode<T>> frontier) {
		boolean collapsed = node.isLeaf() || frontier.contains(node)
				|| (minTopicSize > 1 && hierarchy.getTopicSize(node) < minTopicSize)
				|| (maxDepth > 0 && level >= maxDepth);
		return new Row<T>(node, level, lastChild, collapsed);
	}

	/**
	 * Paints the tree onto a Swing / AWT graphics.
	 *
	 * @param graphics
	 *            the graphics
	 * @return the size of the drawing
	 */
	public Dimension paintTree(Graphics2D graphics) {
		return paintTree(new Graphics2DTreeCanvas(graphics));
	}

	/**
	 * Computes the size of the drawing without drawing it.
	 *
	 * @return the size the drawing would have on an SVG canvas
	 */
	public Dimension getSize() {
		return paintTree(new SvgTreeCanvas());
	}

	/**
	 * Paints the tree.
	 *
	 * @param canvas
	 *            the canvas to draw onto
	 * @return the size (width and height) required by the drawing
	 */
	public Dimension paintTree(TreeCanvas canvas) {
		FontMetrics metrics = canvas.getFontMetrics(font);
		int ascent = metrics.getAscent();
		int descent = metrics.getDescent();
		int fontHeight = ascent + descent;
		int lineHeight = (int) Math.round(spacingFactor * fontHeight);
		int stdIndentWidth = (int) Math.round(indentFactor * lineHeight);
		int xTextGap = (int) Math.round(textGapFactor * fontHeight);
		float strokeWidth = Math.max(1f, fontHeight / 12f);

		List<Row<T>> rows = createRows();
		int maxLevel = 0;
		double maxCost = 0;
		double maxLogFr = 0;
		for (Row<T> row : rows) {
			maxLevel = Math.max(maxLevel, row.level);
			maxCost = Math.max(maxCost, cost(row.node));
			maxLogFr = Math.max(maxLogFr, Math.log(Math.max(1, row.node.getTopicFrequency())));
		}
		// Per level: the text x and the row of the last node seen at that level (the
		// parent of the following deeper rows, as the rows are in pre-order).
		int[] xAtLevel = new int[maxLevel + 1];
		int[] rowAtLevel = new int[maxLevel + 1];

		int maxWidth = 0;
		int xLineOffSet = (int) (lineHeight * 0.25);
		int c = 0;
		for (Row<T> row : rows) {
			MapNode<T> node = row.node;
			int level = row.level;
			int x = (level == 0 ? margin : xAtLevel[level - 1] + stdIndentWidth)
					+ indentForNode(node, maxCost, maxLogFr, lineHeight);
			xAtLevel[level] = x;
			rowAtLevel[level] = c;
			int yTop = margin + c * lineHeight;
			int yMid = yTop + lineHeight / 2;
			if (level > 0) {
				// The connector from the parent's vertical line to the text.
				int xPos = xAtLevel[level - 1] + xLineOffSet;
				Color color = showMarks && node.isMarked() ? markColor : lineColor;
				boolean dashed = cost(node) > dashedCostThreshold;
				canvas.drawLine(xPos, yMid, x - xTextGap, yMid, color, strokeWidth, dashed);
				if (row.lastChild) {
					// The parent's vertical line, once, down to its last child.
					int yParentBottom = margin + rowAtLevel[level - 1] * lineHeight + lineHeight;
					canvas.drawLine(xPos, yParentBottom, xPos, yMid, lineColor, strokeWidth, false);
				}
			}

			String text = getNodeText(row);
			Font f = getFont(node);
			FontMetrics fm = canvas.getFontMetrics(f);
			int y = yTop + (lineHeight - fontHeight) / 2 + ascent;
			int textWidth = fm.stringWidth(text);
			canvas.fillRect(x - xTextGap / 2, yTop, textWidth + xTextGap, lineHeight, background);
			canvas.drawText(x, y, text, f, textColor);
			maxWidth = Math.max(maxWidth, x + textWidth);
			c++;
		}
		return new Dimension(maxWidth + fontHeight + margin, c * lineHeight + 2 * margin);
	}

	/**
	 * Writes the tree as an SVG document.
	 *
	 * @param out
	 *            the writer (not closed)
	 * @throws IOException
	 *             on write failure
	 */
	public void writeSvg(Writer out) throws IOException {
		SvgTreeCanvas canvas = new SvgTreeCanvas();
		Dimension size = paintTree(canvas);
		canvas.write(out, size);
	}

	/**
	 * Writes the tree as an SVG file (UTF-8).
	 *
	 * @param file
	 *            the file
	 * @throws IOException
	 *             on write failure
	 */
	public void writeSvg(File file) throws IOException {
		Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")));
		try {
			writeSvg(out);
		} finally {
			out.close();
		}
	}

	/**
	 * The join cost of a node: the log likelihood lost per word occurrence by
	 * the join that created it (0 for leaves).
	 *
	 * @param node
	 *            the node
	 * @return the cost, at least 0
	 */
	public double cost(MapNode<T> node) {
		if (node.isLeaf() || node.getTopicFrequency() <= 0) {
			return 0;
		}
		return Math.max(0, -node.getDeltaLikelihood() / node.getTopicFrequency());
	}

	/**
	 * Returns the font for the label of a node.
	 *
	 * @param node
	 *            the node
	 * @return the font
	 */
	protected Font getFont(MapNode<T> node) {
		if (node.isSenseLeaf()) {
			return senseFont;
		}
		return node.isLeaf() ? font : topicFont;
	}

	/**
	 * Returns the extra horizontal indent of a node.
	 *
	 * @param node
	 *            the node
	 * @param maxCost
	 *            the maximum join cost of the drawn nodes
	 * @param maxLogFr
	 *            the maximum logarithmic frequency of the drawn nodes
	 * @param lineHeight
	 *            the line height in pixels
	 * @return the extra indent in pixels
	 */
	protected int indentForNode(MapNode<T> node, double maxCost, double maxLogFr, int lineHeight) {
		if (nodeIndentFactor <= 0) {
			return 0;
		}
		double share;
		if (costIndent) {
			share = maxCost > 0 ? cost(node) / maxCost : 0;
		} else {
			share = maxLogFr > 0 ? 1 - Math.log(Math.max(1, node.getTopicFrequency())) / maxLogFr : 0;
		}
		return (int) Math.round(share * nodeIndentFactor * lineHeight);
	}

	/**
	 * Builds the label of a row: the word of a leaf, or the id and top words of
	 * a topic, followed by the frequency, the number of words of the topic
	 * (always for a collapsed topic, whose words are not drawn), the join cost
	 * and the log likelihood as configured.
	 *
	 * @param row
	 *            the row
	 * @return the label
	 */
	protected String getNodeText(Row<T> row) {
		MapNode<T> node = row.node;
		StringBuilder sb = new StringBuilder();
		if (node.isLeaf()) {
			WordInfo<T> info = node.getWordInfo();
			sb.append(info == null ? String.valueOf(node.getId()) : info.getLabel());
		} else {
			if (showId) {
				sb.append('(').append(node.getId()).append(") ");
			}
			List<WordInfo<T>> infos = node.getTopTopicWordInfos();
			for (int i = 0; i < infos.size() && i < topWords; i++) {
				if (i > 0) {
					sb.append(", ");
				}
				sb.append(infos.get(i).getLabel());
			}
		}
		List<String> extras = new ArrayList<String>();
		if (showFrequency) {
			extras.add(String.valueOf(node.getTopicFrequency()));
		}
		if (node.isLeaf()) {
			WordInfo<T> info = node.getWordInfo();
			if (info != null && info.getDocuments() > 0) {
				extras.add(info.getDocuments() + " docs");
			}
		} else {
			if (row.collapsed || showTopicSize) {
				extras.add(hierarchy.getTopicSize(node) + " words");
			}
			if (showCost) {
				extras.add("c=" + DF.format(cost(node)));
			}
			if (showLikelihood) {
				extras.add("ll=" + DF.format(node.getLikelihood()));
			}
		}
		if (!extras.isEmpty()) {
			sb.append(" [");
			for (int i = 0; i < extras.size(); i++) {
				if (i > 0) {
					sb.append(", ");
				}
				sb.append(extras.get(i));
			}
			sb.append(']');
		}
		return sb.toString();
	}
}
