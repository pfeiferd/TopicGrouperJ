package org.hhn.topicgrouper.tg.report;

import java.util.ArrayList;
import java.util.List;

import org.hhn.topicgrouper.tg.report.store.MapNode;

/**
 * Selects a given number of topic nodes from a MapNode hierarchy by splitting the budget between subtrees according to their cost.
 * @param <T> the word type
 */
public class TopTopicSearcher<T> {
	/**
	 * Selects n nodes from the hierarchy below the root.
	 * @param n number of topics wanted
	 * @param root root of the hierarchy
	 * @return the selected nodes
	 */
	public List<MapNode<T>> getBestTopics(int n, MapNode<T> root) {
		List<MapNode<T>> res = new ArrayList<MapNode<T>>();
		split(n, root, res);
		return res;
	}

	/**
	 * Distributes n selections over the subtree of the node, adding chosen nodes to the result.
	 * @param n number of selections for this subtree
	 * @param node current node
	 * @param res result list
	 */
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
	
	/**
	 * Returns the share of selections to give the left child, a logistic function of the cost difference.
	 * @param node node with two children
	 * @return a value in (0,1) for the left child
	 */
	protected double ratio(MapNode<T> node) {
		double leftCost = cost(node.getLeftNode());
		double rightCost = cost(node.getRightNode());
//		
//		return leftCost / (leftCost + rightCost);
		return 1 / (1 + Math.exp(rightCost - leftCost));
	}

	/**
	 * Returns the likelihood per topic frequency of the node.
	 * @param node the node
	 * @return the cost value
	 */
	protected double cost(MapNode<T> node) {
		return node.getLikelihood() / node.getTopicFrequency();
	}

	/**
	 * Counts the word-leaf nodes (negative ids) below the node.
	 * @param node the node
	 * @return number of leaves
	 */
	protected int getWords(MapNode<T> node) {
		if (node.getId() < 0) {
			return 1;
		}
		return (node.getLeftNode() == null ? 0 : getWords(node.getLeftNode()))
				+ (node.getRightNode() == null ? 0 : getWords(node
						.getRightNode()));
	}
}
