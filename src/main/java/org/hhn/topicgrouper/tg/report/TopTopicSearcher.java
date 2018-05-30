package org.hhn.topicgrouper.tg.report;

import java.util.ArrayList;
import java.util.List;

import org.hhn.topicgrouper.tg.report.store.MapNode;

public class TopTopicSearcher<T> {
	public List<MapNode<T>> getBestTopics(int n, MapNode<T> root) {
		List<MapNode<T>> res = new ArrayList<MapNode<T>>();
		split(n, root, res);
		return res;
	}

	protected void split(int n, MapNode<T> node, List<MapNode<T>> res) {
		if (node != null && node.getId() >= 0) {
			if (n == 1) {
				res.add(node);
			} else if (node.getLeftNode() == null) {
				if (node.getRightNode() != null) {
					split(n, node.getRightNode(), res);
				}
			} else if (node.getRightNode() == null) {
				if (node.getLeftNode() != null) {
					split(n, node.getLeftNode(), res);
				}
			} else {
				int nLeft = (int) Math.round(n * ratio(node));
				int nRight = n - nLeft;

				split(nLeft, node.getLeftNode(), res);
				split(nRight, node.getRightNode(), res);
			}
		}
	}
	
	protected double ratio(MapNode<T> node) {
		double leftCost = cost(node.getLeftNode());
		double rightCost = cost(node.getRightNode());
//		
//		return leftCost / (leftCost + rightCost);
		return 1 / (1 + Math.exp(rightCost - leftCost));
	}

	protected double cost(MapNode<T> node) {
		return node.getLikelihood() / node.getTopicFrequency();
	}

	protected int getWords(MapNode<T> node) {
		if (node.getId() < 0) {
			return 1;
		}
		return (node.getLeftNode() == null ? 0 : getWords(node.getLeftNode()))
				+ (node.getRightNode() == null ? 0 : getWords(node
						.getRightNode()));
	}
}
