package org.hhn.topicgrouper.tg.report.store;

import gnu.trove.TIntCollection;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.List;

import org.hhn.topicgrouper.doc.DocumentProvider.Vocab;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;
import org.hhn.topicgrouper.tg.TGSolution;

/**
 * {@link TGSolution} view on a recorded {@link TopicHierarchy}; the number of
 * topics can be changed to replay earlier solutions. The elements of the topics
 * are the word ids of the leaves: for a hierarchy with senses these are word
 * types like in {@link org.hhn.topicgrouper.tg.TGSenseSolution} (the sense leaves
 * have ids of their own, the vocabulary only knows the words).
 *
 * @param <T>
 *            the word type
 */
public class MapNodeTGSolution<T> implements TGSolution<T> {
	private final TopicHierarchy<T> hierarchy;
	/**
	 * Current number of topics.
	 */
	private int nTopics;
	/**
	 * Word ids per topic for the current number of topics.
	 */
	private TIntList[] topics;
	/**
	 * Node ids of the current topics.
	 */
	private int[] topicIds;
	/**
	 * Log likelihoods of the current topics.
	 */
	private double[] logLikelihoods;
	/**
	 * Total log likelihood of the current topics.
	 */
	private double totalLikelihood;
	/**
	 * Current topics by node id.
	 */
	private final TIntObjectMap<MapNode<T>> idToNode;
	/**
	 * Topic id per word id for the current solution.
	 */
	private final TIntIntMap wordToTopicIndex;
	/**
	 * Frequency per word id (type).
	 */
	private final TIntIntMap wordToFrequency;
	/**
	 * Vocabulary rebuilt from the word leaves.
	 */
	private final MyVocab<T> vocab;

	/**
	 * Creates a solution view; the last node is taken as root.
	 *
	 * @param allNodes
	 *            all nodes in creation order
	 */
	public MapNodeTGSolution(List<MapNode<T>> allNodes) {
		this(new TopicHierarchy<T>(allNodes));
	}

	/**
	 * Creates a solution view.
	 *
	 * @param hierarchy
	 *            the hierarchy
	 */
	public MapNodeTGSolution(TopicHierarchy<T> hierarchy) {
		this.hierarchy = hierarchy;
		idToNode = new TIntObjectHashMap<MapNode<T>>();
		wordToTopicIndex = new TIntIntHashMap();
		wordToFrequency = new TIntIntHashMap();
		vocab = new MyVocab<T>();
		for (MapNode<T> node : hierarchy.getNodes()) {
			WordInfo<T> info = node.getWordInfo();
			if (info != null) {
				if (!info.isSense()) {
					vocab.putEntry(info.getWord(), info.getWordId());
				}
				wordToFrequency.put(info.getWordId(), info.getFrequency());
			}
		}
	}

	/**
	 * The hierarchy this view is based on.
	 *
	 * @return the hierarchy
	 */
	public TopicHierarchy<T> getHierarchy() {
		return hierarchy;
	}

	/**
	 * Returns the maximum number of topics (the number of grouped words).
	 *
	 * @return maximum number of topics
	 */
	public int getMaxTopics() {
		return hierarchy.getMaxTopics();
	}

	/**
	 * Switches to the solution with n topics.
	 *
	 * @param n
	 *            number of topics, between 1 and getMaxTopics()
	 * @throws IllegalArgumentException
	 *             if n is out of range
	 */
	public void setNumberOfTopics(int n) {
		List<MapNode<T>> nodes = hierarchy.getTopicsAt(n);
		this.nTopics = n;
		topics = new TIntList[n];
		topicIds = new int[n];
		logLikelihoods = new double[n];
		idToNode.clear();
		wordToTopicIndex.clear();
		totalLikelihood = 0;
		int i = 0;
		List<MapNode<T>> leaves = new ArrayList<MapNode<T>>();
		for (MapNode<T> node : nodes) {
			topics[i] = new TIntArrayList();
			topicIds[i] = node.getId();
			logLikelihoods[i] = node.getLikelihood();
			totalLikelihood += logLikelihoods[i];
			leaves.clear();
			hierarchy.collectLeaves(node, leaves);
			for (MapNode<T> leaf : leaves) {
				WordInfo<T> info = leaf.getWordInfo();
				if (info != null) {
					topics[i].add(info.getWordId());
					wordToTopicIndex.put(info.getWordId(), topicIds[i]);
				}
			}
			idToNode.put(topicIds[i], node);
			i++;
		}
	}

	/**
	 * Returns the topic nodes of the solution with the given number of topics.
	 *
	 * @param topics
	 *            number of topics
	 * @return the nodes forming that solution
	 */
	public List<MapNode<T>> getNodesByHistory(int topics) {
		return hierarchy.getTopicsAt(topics);
	}

	@Override
	public int getNumberOfTopics() {
		return nTopics;
	}

	@Override
	public TIntCollection[] getTopics() {
		return topics;
	}

	@Override
	public int[] getTopicIds() {
		return topicIds;
	}

	@Override
	public int getTopicFrequency(int topicIndex) {
		return idToNode.get(topicIndex).getTopicFrequency();
	}

	@Override
	public int getSize() {
		int size = 0;
		for (MapNode<T> root : hierarchy.getRoots()) {
			size += root.getTopicFrequency();
		}
		return size;
	}

	@Override
	public int getTopicForWord(int wordIndex) {
		return wordToTopicIndex.containsKey(wordIndex) ? wordToTopicIndex.get(wordIndex) : -1;
	}

	@Override
	public int getGlobalWordFrequency(int wordIndex) {
		return wordToFrequency.get(wordIndex);
	}

	@Override
	public double getTotalLogLikelhood() {
		return totalLikelihood;
	}

	@Override
	public Vocab<T> getVocab() {
		return vocab;
	}

	@Override
	public double[] getTopicLogLikelihoods() {
		return logLikelihoods;
	}

	/**
	 * Vocabulary that makes putEntry publicly accessible.
	 *
	 * @param <T>
	 *            the word type
	 */
	public static class MyVocab<T> extends DefaultVocab<T> {
		/** Creates an empty vocabulary. */
		public MyVocab() {
		}

		@Override
		public void putEntry(T word, int index) {
			super.putEntry(word, index);
		}
	}
}
