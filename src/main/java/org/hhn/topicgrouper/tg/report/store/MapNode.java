package org.hhn.topicgrouper.tg.report.store;

import gnu.trove.list.TIntList;

import java.io.Serializable;
import java.util.List;

public class MapNode<T> implements Serializable {
	private static final long serialVersionUID = 1L;

	private List<WordInfo<T>> topTopicWordIds;

	private final int id;
	private final int topicFrequency;
	private final MapNode<T> leftNode;
	private final MapNode<T> rightNode;
	private MapNode<T> parent;
	private final double totalLikelihood;
	private final double likelihood;
	private final double deltaLikelihood;
	private boolean marked;

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
	
	public double getTotalLikelihood() {
		return totalLikelihood;
	}
	
	public void setParent(MapNode<T> parent) {
		this.parent = parent;
	}
	
	public MapNode<T> getParent() {
		return parent;
	}

	public int getTopicFrequency() {
		return topicFrequency;
	}

	public MapNode<T> getLeftNode() {
		return leftNode;
	}

	public MapNode<T> getRightNode() {
		return rightNode;
	}

	public List<WordInfo<T>> getTopTopicWordInfos() {
		return topTopicWordIds;
	}

	public boolean isMarked() {
		return marked;
	}

	public void setMarked(boolean marked) {
		this.marked = marked;
	}

	public int getId() {
		return id;
	}

	public double getDeltaLikelihood() {
		return deltaLikelihood;
	}

	public double getLikelihood() {
		return likelihood;
	}
	
	@Override
	public String toString() {
		return id + ": " + topTopicWordIds;
	}
	
	public int getTopicSize() {
		return getTopicSize(this);
	}
	
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
	
	
	public void collectTopicWords(TIntList topicWords) {
		collectTopicWords(this, topicWords);
	}
	
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
	
	public void collectWordNodes(List<MapNode<T>> list) {
		collectWordNodes(this, list);
	}
	
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
	
	public void collectWordInfos(List<WordInfo<T>> list) {
		collectWordInfos(this, list);
	}
	
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
	
	public int countTopicWords() {
		return countTopicWords(this);
	}
	
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