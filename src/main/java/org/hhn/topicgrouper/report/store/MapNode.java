package org.hhn.topicgrouper.report.store;

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
	private final double likelihood;
	private final double deltaLikelihood;
	private boolean marked;

	public MapNode(int id, MapNode<T> leftNode, MapNode<T> rightNode,
			List<WordInfo<T>> topTopicWordIds, double likelihood,
			double deltaLikelihood, int topicFrequency) {
		this.id = id;
		this.leftNode = leftNode;
		this.rightNode = rightNode;
		this.topTopicWordIds = topTopicWordIds;
		this.marked = false;
		this.likelihood = likelihood;
		this.deltaLikelihood = deltaLikelihood;
		this.topicFrequency = topicFrequency;
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
}