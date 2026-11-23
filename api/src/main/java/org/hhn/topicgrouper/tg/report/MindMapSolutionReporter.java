package org.hhn.topicgrouper.tg.report;

import gnu.trove.TIntCollection;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.List;

import org.hhn.topicgrouper.tg.TGSenseSolution;
import org.hhn.topicgrouper.tg.TGSenseSolutionListener;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.report.store.MapNode;
import org.hhn.topicgrouper.tg.report.store.TopicHierarchy;
import org.hhn.topicgrouper.tg.report.store.WordInfo;

/**
 * Solution listener that records the merge history as a binary tree of
 * {@link MapNode}s, usable for mind map export, JSON / SVG files and browsing.
 * <p>
 * With a sense splitting solver a split is recorded as well: a new leaf for the
 * sense (id below the ids of the word leaves) joined with the node of the topic
 * that received it (id at or above the number of words, the join does not
 * change the number of topics). The leaf of the word that was split keeps its
 * place, with its frequency reduced to the remaining occurrences. Later
 * reunifications of two senses are not recorded; the frequencies of the sense
 * leaves are those at the time of the split.
 *
 * @param <T>
 *            the word type
 */
public class MindMapSolutionReporter<T> implements TGSenseSolutionListener<T> {

	/**
	 * All nodes in creation order (leaves first, then merges).
	 */
	private final List<MapNode<T>> allNodes;
	/**
	 * Current root nodes by topic index.
	 */
	private final TIntObjectMap<MapNode<T>> currentNodes;
	/**
	 * Maximum number of top words kept per node.
	 */
	private final int topWords;
	/**
	 * Whether top words moved to a parent are removed from the child.
	 */
	private final boolean removeWordFromChildAtPull;
	/**
	 * Improvement ratio from which merged children are marked.
	 */
	private final double markRatioLevel;
	/**
	 * Topic count at or below which marking is considered.
	 */
	private final double minMarkTopics;
	private final ImprovementAssessor assessor;
	/**
	 * Number of initial topics (words); the ids of split nodes start here.
	 */
	private int maxTopics;
	/**
	 * Number of splits recorded so far.
	 */
	private int splits;
	/**
	 * Number of senses per word index.
	 */
	private final TIntIntMap sensesOfWord;

	/**
	 * Creates a reporter.
	 *
	 * @param topWords
	 *            number of top words per node
	 * @param removeWordFromChildAtPull
	 *            remove words from children when they are pulled up to the parent
	 * @param markRatioLevel
	 *            improvement ratio from which merged children are marked
	 * @param minMarkTopics
	 *            marking only happens at or below this number of topics
	 */
	public MindMapSolutionReporter(int topWords, boolean removeWordFromChildAtPull, double markRatioLevel,
			int minMarkTopics) {
		currentNodes = new TIntObjectHashMap<MapNode<T>>();
		this.topWords = topWords;
		this.removeWordFromChildAtPull = removeWordFromChildAtPull;
		this.markRatioLevel = markRatioLevel;
		this.minMarkTopics = minMarkTopics;
		this.assessor = new ImprovementAssessor(5);
		this.allNodes = new ArrayList<MapNode<T>>();
		this.sensesOfWord = new TIntIntHashMap();
	}

	/**
	 * Returns the current top-level nodes by topic index.
	 *
	 * @return map from topic index to node
	 */
	public TIntObjectMap<MapNode<T>> getCurrentNodes() {
		return currentNodes;
	}

	/**
	 * Returns all nodes created so far, in creation order.
	 *
	 * @return list of nodes
	 */
	public List<MapNode<T>> getAllNodes() {
		return allNodes;
	}

	/**
	 * The recorded hierarchy (a snapshot of {@link #getAllNodes()}).
	 *
	 * @return the hierarchy
	 */
	public TopicHierarchy<T> getHierarchy() {
		return new TopicHierarchy<T>(allNodes);
	}

	/**
	 * The number of sense splits recorded.
	 *
	 * @return the number of sense leaves
	 */
	public int getNumberOfSplits() {
		return splits;
	}

	@Override
	public void initalizing(double percentage) {
	}

	@Override
	public void initialized(TGSolution<T> initialSolution) {
		TIntCollection[] t = initialSolution.getTopics();
		for (int i = 0; i < t.length; i++) {
			if (t[i] != null) {
				int wordId = t[i].iterator().next();

				List<WordInfo<T>> list = new ArrayList<WordInfo<T>>();
				list.add(new WordInfo<T>(wordId, initialSolution.getGlobalWordFrequency(wordId), initialSolution
						.getVocab().getWord(wordId)));
				MapNode<T> node = new MapNode<T>(-i, null, null, list, initialSolution.getTotalLogLikelhood(), 0, 0,
						initialSolution.getTopicFrequency(i));
				currentNodes.put(i, node);
				allNodes.add(node);
			}
		}
	}

	@Override
	public void beforeInitialization(int maxTopics, int documents) {
		this.maxTopics = maxTopics;
		this.splits = 0;
		sensesOfWord.clear();
		currentNodes.clear();
		allNodes.clear();
	}

	@Override
	public void updatedSolution(int newTopicIndex, int oldTopicIndex, double improvement, int t1Size, int t2Size,
			TGSolution<T> solution) {
		boolean mark = false;
		Double value = assessor.addImprovement(improvement);
		if (solution.getNumberOfTopics() <= minMarkTopics && value != null) {
			if (value >= markRatioLevel) {
				mark = true;
			}
		}

		MapNode<T> child1 = currentNodes.remove(newTopicIndex);
		MapNode<T> child2 = currentNodes.remove(oldTopicIndex);

		MapNode<T> parent = createParent(solution.getNumberOfTopics(), child1, child2, newTopicIndex, improvement,
				solution);
		currentNodes.put(newTopicIndex, parent);
		allNodes.add(parent);

		if (mark) {
			child1.setMarked(true);
			child2.setMarked(true);
		}
	}

	@Override
	public void splitSense(int topicIndex, int wordIndex, int senseIndex, int documents, double improvement,
			TGSenseSolution<T> solution) {
		splits++;
		int sense = sensesOfWord.adjustOrPutValue(wordIndex, 1, 1);
		T word = solution.getVocab().getWord(wordIndex);
		List<WordInfo<T>> list = new ArrayList<WordInfo<T>>();
		list.add(new WordInfo<T>(senseIndex, solution.getGlobalWordFrequency(senseIndex), word, sense, documents));
		MapNode<T> leaf = new MapNode<T>(-(maxTopics + splits - 1), null, null, list,
				solution.getTotalLogLikelhood(), 0, 0, solution.getGlobalWordFrequency(senseIndex));
		allNodes.add(leaf);

		MapNode<T> topic = currentNodes.remove(topicIndex);
		MapNode<T> parent = createParent(maxTopics + splits - 1, topic, leaf, topicIndex, improvement, solution);
		currentNodes.put(topicIndex, parent);
		allNodes.add(parent);

		// The split word keeps its leaf with the remaining occurrences.
		int remainder = solution.getTopicForWord(wordIndex);
		MapNode<T> node = remainder < 0 ? null : currentNodes.get(remainder);
		if (node != null && node.isLeaf()) {
			WordInfo<T> info = node.getWordInfo();
			if (info != null && info.getWordId() == wordIndex) {
				node.setWordInfo(new WordInfo<T>(wordIndex, solution.getGlobalWordFrequency(wordIndex), word));
			}
		}
	}

	/**
	 * Creates the node of two joined children with the top words pulled from the children.
	 */
	private MapNode<T> createParent(int id, MapNode<T> child1, MapNode<T> child2, int topicIndex,
			double improvement, TGSolution<T> solution) {
		List<WordInfo<T>> topList = new ArrayList<WordInfo<T>>();
		List<WordInfo<T>> child1List = child1.getTopTopicWordInfos();
		List<WordInfo<T>> child2List = child2.getTopTopicWordInfos();
		if (removeWordFromChildAtPull) {
			for (int i = 0; i < topWords; i++) {
				pullFromChildren(topList, child1, child2);
			}
		} else {
			int i1 = 0, i2 = 0;
			for (int i = 0; i < topWords; i++) {
				if (i1 < child1List.size() && i2 < child2List.size()) {
					if (child1List.get(i1).getFrequency() > child2List.get(i2).getFrequency()) {
						topList.add(child1List.get(i1++));
					} else {
						topList.add(child2List.get(i2++));
					}
				} else {
					if (i1 < child1List.size()) {
						topList.add(child1List.get(i1++));
					} else if (i2 < child2List.size()) {
						topList.add(child2List.get(i2++));
					}
				}
			}
		}

		MapNode<T> parent = new MapNode<T>(id, child1List.isEmpty() ? null : child1, child2List.isEmpty() ? null
				: child2, topList, solution.getTotalLogLikelhood(), solution.getTopicLogLikelihoods()[topicIndex],
				improvement, solution.getTopicFrequency(topicIndex));
		child1.setParent(parent);
		child2.setParent(parent);
		return parent;
	}

	/**
	 * Moves the most frequent top word of the two children (and recursively of their descendants) to the target list.
	 *
	 * @param target
	 *            list receiving the word
	 * @param child1
	 *            first child, may be null
	 * @param child2
	 *            second child, may be null
	 */
	private void pullFromChildren(List<WordInfo<T>> target, MapNode<T> child1, MapNode<T> child2) {
		List<WordInfo<T>> child1List = child1 == null ? null : child1.getTopTopicWordInfos();
		List<WordInfo<T>> child2List = child2 == null ? null : child2.getTopTopicWordInfos();
		if (child1List != null && !child1List.isEmpty()) {
			if (child2List != null && !child2List.isEmpty()) {
				if (child1List.get(0).getFrequency() > child2List.get(0).getFrequency()) {
					doWordPull(target, child1);
				} else {
					doWordPull(target, child2);
				}
			} else {
				if (child1List != null) {
					doWordPull(target, child1);
				}
			}
		} else {
			if (child2List != null && !child2List.isEmpty()) {
				doWordPull(target, child2);
			}
		}
	}

	/**
	 * Removes the first word of the child into the target and refills the child's list from its own children.
	 *
	 * @param target
	 *            list receiving the word
	 * @param child
	 *            child to take the word from
	 */
	private void doWordPull(List<WordInfo<T>> target, MapNode<T> child) {
		List<WordInfo<T>> childList = child.getTopTopicWordInfos();
		target.add(childList.remove(0));
		pullFromChildren(child.getTopTopicWordInfos(), child.getLeftNode(), child.getRightNode());
	}

	@Override
	public void done() {
	}
}
