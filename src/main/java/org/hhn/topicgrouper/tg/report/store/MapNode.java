package org.hhn.topicgrouper.tg.report.store;

import gnu.trove.list.TIntList;

import java.io.Serializable;
import java.util.List;

/**
 * Serializable node of a topic merge hierarchy: a word leaf or a topic formed by merging a left and a right node.
 * @param <T> the word type
 */
public class MapNode<T> implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Most frequent words of the topic.
	 */
	private List<WordInfo<T>> topTopicWordIds;

	/**
	 * Node id: positive for merged topics (the topic count after the merge), non-positive for word leaves.
	 */
	private final int id;
	/**
	 * Total word frequency of the topic.
	 */
	private final int topicFrequency;
	/**
	 * First merged child, or null.
	 */
	private final MapNode<T> leftNode;
	/**
	 * Second merged child, or null.
	 */
	private final MapNode<T> rightNode;
	/**
	 * Node this one was merged into, or null.
	 */
	private MapNode<T> parent;
	/**
	 * Total log likelihood of the solution after this node was created.
	 */
	private final double totalLikelihood;
	/**
	 * Log likelihood of this topic.
	 */
	private final double likelihood;
	/**
	 * Log likelihood improvement of the merge creating this node.
	 */
	private final double deltaLikelihood;
	/**
	 * Whether the node has been marked as significant.
	 */
	private boolean marked;

	/**
	 * Creates a node.
	 * @param id node id
	 * @param leftNode first child or null
	 * @param rightNode second child or null
	 * @param topTopicWordIds top words of the topic
	 * @param totalLikelihood total log likelihood of the solution
	 * @param likelihood log likelihood of this topic
	 * @param deltaLikelihood improvement of the merge creating this node
	 * @param topicFrequency total word frequency of the topic
	 */
	public MapNode(int id, MapNode<T> leftNode, MapNode<T> rightNode,
			List<WordInfo<T>> topTopicWordIds, double totalLikelihood, double likelihood,
			double deltaLikelihood, int topicFrequency) {
		this.id = id;
		this.leftNode = leftNode;
		this.rightNode = rightNode;
		this.topTopicWordIds = topTopicWordIds;
		this.marked = false;
		this.totalLikelihood = totalLikelihood;
		this.likelihood = likelihood;
		this.deltaLikelihood = deltaLikelihood;
		this.topicFrequency = topicFrequency;
	}
	
	/**
	 * Returns the total log likelihood of the solution at creation of this node.
	 * @return the total likelihood
	 */
	public double getTotalLikelihood() {
		return totalLikelihood;
	}
	
	/**
	 * Sets the parent node.
	 * @param parent the parent
	 */
	public void setParent(MapNode<T> parent) {
		this.parent = parent;
	}
	
	/**
	 * Returns the parent node.
	 * @return the parent, or null
	 */
	public MapNode<T> getParent() {
		return parent;
	}

	/**
	 * Returns the total word frequency of the topic.
	 * @return the frequency
	 */
	public int getTopicFrequency() {
		return topicFrequency;
	}

	/**
	 * Returns the left child.
	 * @return the left child, or null
	 */
	public MapNode<T> getLeftNode() {
		return leftNode;
	}

	/**
	 * Returns the right child.
	 * @return the right child, or null
	 */
	public MapNode<T> getRightNode() {
		return rightNode;
	}

	/**
	 * Returns the (mutable) list of top words.
	 * @return the top word infos
	 */
	public List<WordInfo<T>> getTopTopicWordInfos() {
		return topTopicWordIds;
	}

	/**
	 * Returns whether the node is marked.
	 * @return true if marked
	 */
	public boolean isMarked() {
		return marked;
	}

	/**
	 * Marks or unmarks the node.
	 * @param marked the new mark
	 */
	public void setMarked(boolean marked) {
		this.marked = marked;
	}

	/**
	 * Returns the node id.
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Returns the likelihood improvement of the merge creating this node.
	 * @return the delta likelihood
	 */
	public double getDeltaLikelihood() {
		return deltaLikelihood;
	}

	/**
	 * Returns the log likelihood of this topic.
	 * @return the likelihood
	 */
	public double getLikelihood() {
		return likelihood;
	}
	
	@Override
	public String toString() {
		return id + ": " + topTopicWordIds;
	}
	
	/**
	 * Returns the number of words in this topic's subtree.
	 * @return the topic size
	 */
	public int getTopicSize() {
		return getTopicSize(this);
	}
	
	/**
	 * Returns the number of words in the subtree of the node.
	 * @param node the node, may be null
	 * @param <T> the word type
	 * @return the topic size, 0 for null
	 */
	public static <T> int getTopicSize(MapNode<T> node) {
		if (node == null) {
			return 0;
		}
		if (node.getTopTopicWordInfos().size() == 1) {
			return 1;
		}
		return getTopicSize(node.getLeftNode())
				+ getTopicSize(node.getRightNode());
	}
	
	
	/**
	 * Adds the word ids of all leaves below this node.
	 * @param topicWords list receiving the ids
	 */
	public void collectTopicWords(TIntList topicWords) {
		collectTopicWords(this, topicWords);
	}
	
	/**
	 * Adds the word ids of all leaves below the node.
	 * @param node the node, may be null
	 * @param topicWords list receiving the ids
	 * @param <T> the word type
	 */
	public static <T> void collectTopicWords(MapNode<T> node, TIntList topicWords) {
		if (node == null) {
			return;
		}
		if (node.getLeftNode() == null && node.getRightNode() == null) {
			topicWords.add(node.getTopTopicWordInfos().get(0).getWordId());
		} else {
			collectTopicWords(node.getLeftNode(), topicWords);
			collectTopicWords(node.getRightNode(), topicWords);
		}
	}
	
	/**
	 * Adds all leaf nodes below this node.
	 * @param list list receiving the leaves
	 */
	public void collectWordNodes(List<MapNode<T>> list) {
		collectWordNodes(this, list);
	}
	
	/**
	 * Adds all leaf nodes below the node.
	 * @param node the node, may be null
	 * @param list list receiving the leaves
	 * @param <T> the word type
	 */
	public static <T> void collectWordNodes(MapNode<T> node, List<MapNode<T>> list) {
		if (node == null) {
			return;
		}
		if (node.getLeftNode() == null && node.getRightNode() == null) {
			list.add(node);
		} else {
			collectWordNodes(node.getLeftNode(), list);
			collectWordNodes(node.getRightNode(), list);
		}
	}
	
	/**
	 * Adds the word infos of all leaves below this node.
	 * @param list list receiving the word infos
	 */
	public void collectWordInfos(List<WordInfo<T>> list) {
		collectWordInfos(this, list);
	}
	
	/**
	 * Adds the word infos of all leaves below the node.
	 * @param node the node, may be null
	 * @param list list receiving the word infos
	 * @param <T> the word type
	 */
	public static <T> void collectWordInfos(MapNode<T> node, List<WordInfo<T>> list) {
		if (node == null) {
			return;
		}
		if (node.getLeftNode() == null && node.getRightNode() == null) {
			list.add(node.getTopTopicWordInfos().get(0));
		} else {
			collectWordInfos(node.getLeftNode(), list);
			collectWordInfos(node.getRightNode(), list);
		}
	}
	
	/**
	 * Counts the leaves below this node.
	 * @return number of words
	 */
	public int countTopicWords() {
		return countTopicWords(this);
	}
	
	/**
	 * Counts the leaves below the node.
	 * @param node the node, may be null
	 * @param <T> the word type
	 * @return number of words, 0 for null
	 */
	public static <T> int countTopicWords(MapNode<T> node) {
		if (node == null) {
			return 0;
		}
		if (node.getLeftNode() == null && node.getRightNode() == null) {
			return 1;
		} else {
			return countTopicWords(node.getLeftNode())
					+ countTopicWords(node.getRightNode());
		}
	}
}