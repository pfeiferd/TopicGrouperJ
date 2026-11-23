package org.hhn.topicgrouper.tg.paint;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import org.hhn.topicgrouper.tg.report.store.MapNode;
import org.hhn.topicgrouper.tg.report.store.TopicHierarchy;

/**
 * A {@link TopicTreePainter} with sensible defaults (12 pt sans serif, bold
 * topics, italic senses, ids, frequencies and word counts shown, five top
 * words, no pruning)
 * and setters for all parameters. The setters return the painter so that they
 * can be chained.
 *
 * @param <T>
 *            the word type
 */
public class DefaultTopicTreePainter<T> extends TopicTreePainter<T> {
	/**
	 * Creates a painter for the whole hierarchy.
	 *
	 * @param hierarchy
	 *            the hierarchy
	 */
	public DefaultTopicTreePainter(TopicHierarchy<T> hierarchy) {
		super(hierarchy);
	}

	/**
	 * Creates a painter for some subtrees of a hierarchy.
	 *
	 * @param hierarchy
	 *            the hierarchy
	 * @param roots
	 *            the nodes whose subtrees are painted
	 */
	public DefaultTopicTreePainter(TopicHierarchy<T> hierarchy, List<MapNode<T>> roots) {
		super(hierarchy, roots);
	}

	@Override
	protected void initParams() {
		font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
		topicFont = font.deriveFont(Font.BOLD);
		senseFont = font.deriveFont(Font.ITALIC);
		spacingFactor = 1.3;
		indentFactor = 1.0;
		textGapFactor = 0.3;
		nodeIndentFactor = 0;
		costIndent = true;
		dashedCostThreshold = Double.POSITIVE_INFINITY;
		showMarks = true;
		showId = true;
		showFrequency = true;
		showTopicSize = true;
		showCost = false;
		showLikelihood = false;
		topWords = 5;
		topics = 0;
		minTopicSize = 1;
		maxDepth = 0;
		lineColor = Color.BLACK;
		markColor = Color.RED;
		textColor = Color.BLACK;
		background = Color.WHITE;
		margin = 4;
	}

	/**
	 * Sets the fonts: the given one for words, bold for topics, italic for senses.
	 *
	 * @param font
	 *            the base font
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setFont(Font font) {
		this.font = font;
		this.topicFont = font.deriveFont(Font.BOLD);
		this.senseFont = font.deriveFont(Font.ITALIC);
		return this;
	}

	/**
	 * Sets the font of topics.
	 *
	 * @param font
	 *            the font
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setTopicFont(Font font) {
		this.topicFont = font;
		return this;
	}

	/**
	 * Sets the font of senses.
	 *
	 * @param font
	 *            the font
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setSenseFont(Font font) {
		this.senseFont = font;
		return this;
	}

	/**
	 * Sets the line spacing.
	 *
	 * @param spacingFactor
	 *            line height as a multiple of the font height
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setSpacingFactor(double spacingFactor) {
		this.spacingFactor = spacingFactor;
		return this;
	}

	/**
	 * Sets the indent per level.
	 *
	 * @param indentFactor
	 *            indent as a multiple of the line height
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setIndentFactor(double indentFactor) {
		this.indentFactor = indentFactor;
		return this;
	}

	/**
	 * Sets the gap between connector line and text.
	 *
	 * @param textGapFactor
	 *            gap as a multiple of the font height
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setTextGapFactor(double textGapFactor) {
		this.textGapFactor = textGapFactor;
		return this;
	}

	/**
	 * Sets the extra indentation of nodes.
	 *
	 * @param nodeIndentFactor
	 *            maximum extra indent as a multiple of the line height, 0 for none
	 * @param costIndent
	 *            true to indent by join cost, false by (small) frequency
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setNodeIndent(double nodeIndentFactor, boolean costIndent) {
		this.nodeIndentFactor = nodeIndentFactor;
		this.costIndent = costIndent;
		return this;
	}

	/**
	 * Sets the join cost above which connector lines are dashed.
	 *
	 * @param threshold
	 *            the cost in lost log likelihood per occurrence, infinity for never
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setDashedCostThreshold(double threshold) {
		this.dashedCostThreshold = threshold;
		return this;
	}

	/**
	 * Sets whether marked nodes get colored connector lines.
	 *
	 * @param showMarks
	 *            true to show marks
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setShowMarks(boolean showMarks) {
		this.showMarks = showMarks;
		return this;
	}

	/**
	 * Sets what the labels contain.
	 *
	 * @param showId
	 *            the id of topics
	 * @param showFrequency
	 *            the frequency
	 * @param showCost
	 *            the join cost of topics
	 * @param showLikelihood
	 *            the log likelihood of topics
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setLabelParts(boolean showId, boolean showFrequency, boolean showCost,
			boolean showLikelihood) {
		this.showId = showId;
		this.showFrequency = showFrequency;
		this.showCost = showCost;
		this.showLikelihood = showLikelihood;
		return this;
	}

	/**
	 * Sets whether every topic label states the number of words of the topic
	 * (collapsed topics always do).
	 * 
	 * @param showTopicSize
	 *            true to show the word count on every topic
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setShowTopicSize(boolean showTopicSize) {
		this.showTopicSize = showTopicSize;
		return this;
	}

	/**
	 * Sets the number of top words in the label of a topic.
	 *
	 * @param topWords
	 *            the maximum number of words
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setTopWords(int topWords) {
		this.topWords = topWords;
		return this;
	}

	/**
	 * Prunes the drawing to the solution with the given number of topics.
	 *
	 * @param topics
	 *            the number of topics whose nodes are drawn collapsed, 0 for the whole hierarchy
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setTopics(int topics) {
		this.topics = topics;
		return this;
	}

	/**
	 * Collapses small topics.
	 *
	 * @param minTopicSize
	 *            topics with fewer words are drawn as one line
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setMinTopicSize(int minTopicSize) {
		this.minTopicSize = minTopicSize;
		return this;
	}

	/**
	 * Limits the depth of the drawing.
	 *
	 * @param maxDepth
	 *            nodes at this depth are drawn collapsed, 0 for no limit
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
		return this;
	}

	/**
	 * Sets the colors.
	 *
	 * @param lineColor
	 *            connector lines
	 * @param markColor
	 *            connector lines of marked nodes
	 * @param textColor
	 *            text
	 * @param background
	 *            background behind the text
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setColors(Color lineColor, Color markColor, Color textColor, Color background) {
		this.lineColor = lineColor;
		this.markColor = markColor;
		this.textColor = textColor;
		this.background = background;
		return this;
	}

	/**
	 * Sets the margin.
	 *
	 * @param margin
	 *            pixels around the drawing
	 * @return this painter
	 */
	public DefaultTopicTreePainter<T> setMargin(int margin) {
		this.margin = margin;
		return this;
	}
}
