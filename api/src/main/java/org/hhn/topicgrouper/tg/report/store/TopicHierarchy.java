package org.hhn.topicgrouper.tg.report.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A topic hierarchy recorded by a {@link org.hhn.topicgrouper.tg.report.MindMapSolutionReporter}:
 * the {@link MapNode}s in the order of their creation (the word leaves first,
 * then every join or sense split, a sense leaf directly before the node that
 * joined it) plus derived information that is computed without recursion, so
 * that very deep hierarchies (a large topic absorbing one word at a time) are
 * no problem.
 * <p>
 * The <em>solution with n topics</em> is the state of the solver right before
 * the join that reduced the number of topics to n - 1, i.e. after all sense
 * splits that happened at n topics; see {@link #getTopicsAt(int)}.
 *
 * @param <T>
 *            the word type
 */
public class TopicHierarchy<T> {
	private final List<MapNode<T>> nodes;
	private final List<MapNode<T>> roots;
	private final Map<MapNode<T>, Integer> index;
	private final int[] topicSizes;
	private final int[] topicsAfter;
	private final int words;
	private final int senses;

	/**
	 * Wraps the nodes of a hierarchy.
	 *
	 * @param nodes
	 *            all nodes in creation order (children before their parent)
	 * @throws IllegalArgumentException
	 *             if a parent precedes one of its children
	 */
	public TopicHierarchy(List<MapNode<T>> nodes) {
		this.nodes = Collections.unmodifiableList(new ArrayList<MapNode<T>>(nodes));
		index = new IdentityHashMap<MapNode<T>, Integer>(nodes.size() * 2);
		roots = new ArrayList<MapNode<T>>();
		topicSizes = new int[nodes.size()];
		topicsAfter = new int[nodes.size()];
		int w = 0;
		int s = 0;
		int topics = 0;
		for (int i = 0; i < nodes.size(); i++) {
			MapNode<T> node = nodes.get(i);
			index.put(node, i);
			if (node.getParent() == null) {
				roots.add(node);
			}
			if (node.isLeaf()) {
				topicSizes[i] = 1;
				topics++;
				if (node.isSenseLeaf()) {
					s++;
				} else {
					w++;
				}
			} else {
				int children = 0;
				int size = 0;
				for (MapNode<T> child : children(node)) {
					Integer ci = index.get(child);
					if (ci == null) {
						throw new IllegalArgumentException("node " + node.getId() + " precedes its child "
								+ child.getId());
					}
					size += topicSizes[ci];
					children++;
				}
				topicSizes[i] = size;
				topics += 1 - children;
			}
			topicsAfter[i] = topics;
		}
		words = w;
		senses = s;
	}

	/**
	 * The children of a node (left before right, <code>null</code> children omitted).
	 *
	 * @param node
	 *            a node
	 * @return the children, empty for a leaf
	 */
	public List<MapNode<T>> children(MapNode<T> node) {
		List<MapNode<T>> res = new ArrayList<MapNode<T>>(2);
		if (node.getLeftNode() != null) {
			res.add(node.getLeftNode());
		}
		if (node.getRightNode() != null) {
			res.add(node.getRightNode());
		}
		return res;
	}

	/**
	 * All nodes in creation order.
	 *
	 * @return the nodes (read only)
	 */
	public List<MapNode<T>> getNodes() {
		return nodes;
	}

	/**
	 * The number of nodes.
	 *
	 * @return the size of {@link #getNodes()}
	 */
	public int size() {
		return nodes.size();
	}

	/**
	 * The node at a position of the creation order.
	 *
	 * @param i
	 *            the position
	 * @return the node
	 */
	public MapNode<T> get(int i) {
		return nodes.get(i);
	}

	/**
	 * The position of a node in the creation order.
	 *
	 * @param node
	 *            a node of this hierarchy
	 * @return its position, -1 if the node does not belong to the hierarchy
	 */
	public int indexOf(MapNode<T> node) {
		Integer i = index.get(node);
		return i == null ? -1 : i;
	}

	/**
	 * The nodes without parent, i.e. the topics the solver stopped with.
	 *
	 * @return the roots in creation order (a single node if the solver ran down to one topic)
	 */
	public List<MapNode<T>> getRoots() {
		return Collections.unmodifiableList(roots);
	}

	/**
	 * The last root, which is the only one if the solver ran down to one topic.
	 *
	 * @return the root
	 */
	public MapNode<T> getRoot() {
		return roots.get(roots.size() - 1);
	}

	/**
	 * The number of word leaves, i.e. of initial topics.
	 *
	 * @return the number of grouped words
	 */
	public int getNumberOfWords() {
		return words;
	}

	/**
	 * The number of sense leaves.
	 *
	 * @return the number of senses split off
	 */
	public int getNumberOfSenses() {
		return senses;
	}

	/**
	 * The maximum number of topics, i.e. the number of word leaves.
	 *
	 * @return the number of topics of the initial solution
	 */
	public int getMaxTopics() {
		return words;
	}

	/**
	 * The number of words (leaves) below a node.
	 *
	 * @param node
	 *            a node of this hierarchy
	 * @return the topic size, senses counted as words
	 */
	public int getTopicSize(MapNode<T> node) {
		return topicSizes[indexOf(node)];
	}

	/**
	 * The number of topics the solver had after creating the node.
	 *
	 * @param i
	 *            position of the node in the creation order
	 * @return the number of topics (for a join: its id; for a sense split: the
	 *         number of topics before and after the split; for the initial
	 *         leaves: the running count of initial topics)
	 */
	public int getTopicsAfter(int i) {
		return topicsAfter[i];
	}

	/**
	 * Whether the node was created by a sense split (its id is at least the
	 * number of words and one of its children is a sense leaf).
	 *
	 * @param node
	 *            a node
	 * @return true for a sense split node
	 */
	public boolean isSplitNode(MapNode<T> node) {
		return !node.isLeaf() && node.getId() >= words;
	}

	/**
	 * The topics of the solution with the given number of topics.
	 *
	 * @param n
	 *            the number of topics, between 1 and {@link #getMaxTopics()}
	 * @return the nodes representing the topics, in creation order
	 * @throws IllegalArgumentException
	 *             if n is out of range
	 */
	public List<MapNode<T>> getTopicsAt(int n) {
		if (n < 1 || n > words) {
			throw new IllegalArgumentException("number of topics out of range: " + n);
		}
		Set<MapNode<T>> current = new LinkedHashSet<MapNode<T>>();
		for (MapNode<T> node : nodes) {
			if (node.isLeaf()) {
				current.add(node);
			} else {
				List<MapNode<T>> children = children(node);
				if (current.size() - children.size() + 1 < n) {
					break;
				}
				current.removeAll(children);
				current.add(node);
			}
		}
		return new ArrayList<MapNode<T>>(current);
	}

	/**
	 * Adds the leaves below a node (in left to right order) without recursion.
	 *
	 * @param node
	 *            the node
	 * @param leaves
	 *            the list receiving the leaves
	 */
	public void collectLeaves(MapNode<T> node, List<MapNode<T>> leaves) {
		List<MapNode<T>> stack = new ArrayList<MapNode<T>>();
		stack.add(node);
		while (!stack.isEmpty()) {
			MapNode<T> n = stack.remove(stack.size() - 1);
			if (n.isLeaf()) {
				leaves.add(n);
			} else {
				if (n.getRightNode() != null) {
					stack.add(n.getRightNode());
				}
				if (n.getLeftNode() != null) {
					stack.add(n.getLeftNode());
				}
			}
		}
	}

	/**
	 * The depth of a node, 0 for a root.
	 *
	 * @param node
	 *            a node
	 * @return the number of ancestors
	 */
	public int getLevel(MapNode<T> node) {
		int level = 0;
		for (MapNode<T> p = node.getParent(); p != null; p = p.getParent()) {
			level++;
		}
		return level;
	}
}
