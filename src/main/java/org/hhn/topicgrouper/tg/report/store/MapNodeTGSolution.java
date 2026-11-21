package org.hhn.topicgrouper.tg.report.store;

import gnu.trove.TIntCollection;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import org.hhn.topicgrouper.doc.DocumentProvider.Vocab;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;
import org.hhn.topicgrouper.tg.TGSolution;

/**
 * TGSolution view on a recorded MapNode history; the number of topics can be changed to replay earlier solutions.
 * @param <T> the word type
 */
public class MapNodeTGSolution<T> implements TGSolution<T> {
	/**
	 * Last node of the history, the root of the hierarchy.
	 */
	private final MapNode<T> root;
	/**
	 * All nodes in creation order.
	 */
	private final List<MapNode<T>> allNodes;
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
	 * Global frequency per word id.
	 */
	private final TIntIntMap wordToFrequency;
	/**
	 * Vocabulary rebuilt from the leaf nodes.
	 */
	private final MyVocab<T> vocab;
	/**
	 * Maximum number of topics, i.e. the number of words.
	 */
	private final int maxTopics;

	/**
	 * Creates a solution view; the last node is taken as root.
	 * @param allNodes all nodes in creation order
	 */
	public MapNodeTGSolution(List<MapNode<T>> allNodes) {
		this.allNodes = allNodes;
		root = allNodes.get(allNodes.size() - 1);
		idToNode = new TIntObjectHashMap<MapNode<T>>();
		wordToTopicIndex = new TIntIntHashMap();
		wordToFrequency = new TIntIntHashMap();
		vocab = new MyVocab<T>();
		collectVocab(root);
		maxTopics = allNodes.size() - vocab.getNumberOfWords();
	}
	
	/**
	 * Returns the maximum number of topics (the vocabulary size).
	 * @return maximum number of topics
	 */
	public int getMaxTopics() {
		return maxTopics;
	}
	
	/**
	 * Fills the vocabulary and word frequencies from the leaves below the node.
	 * @param node the node, may be null
	 */
	protected void collectVocab(MapNode<T> node) {
		if (node == null) {
			return;
		}
		if (node.getLeftNode() == null && node.getRightNode() == null) {
			WordInfo<T> wordInfo = node.getTopTopicWordInfos().get(0);
			vocab.putEntry(wordInfo.getWord(), wordInfo.getWordId());
			wordToFrequency.put(wordInfo.getWordId(), wordInfo.getFrequency());
		} else {
			collectVocab(node.getLeftNode());
			collectVocab(node.getRightNode());
		}
	}


	/**
	 * Switches to the solution with n topics.
	 * @param n number of topics, between 1 and getMaxTopics()
	 * @throws IllegalArgumentException if n is out of range
	 */
	public void setNumberOfTopics(int n) {
		if (n < 1 || n > maxTopics) {
			throw new IllegalArgumentException();
		}
		this.nTopics = n;
		List<MapNode<T>> nodes = getNodesByHistory(n);
		topics = new TIntList[n];
		topicIds = new int[n];
		logLikelihoods = new double[n];
		idToNode.clear();
		wordToTopicIndex.clear();
		totalLikelihood = 0;
		int i = 0;
		for (MapNode<T> node : nodes) {
			topics[i] = new TIntArrayList();
			topicIds[i] = node.getId();
			logLikelihoods[i] = node.getLikelihood();
			totalLikelihood += logLikelihoods[i];
			collectTopicWords(node, topics[i]);
			for (int j = 0; j < topics[i].size(); j++) {
				wordToTopicIndex.put(topics[i].get(j), topicIds[i]);				
			}
			idToNode.put(topicIds[i], node);
			i++;
		}
	}

	/**
	 * Adds the word ids of all leaves below the node.
	 * @param node the node, may be null
	 * @param topicWords collection receiving the ids
	 */
	protected void collectTopicWords(MapNode<T> node, TIntCollection topicWords) {
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
	 * Returns the topic nodes of the solution with the given number of topics.
	 * @param topics number of topics
	 * @return the nodes forming that solution
	 */
	public List<MapNode<T>> getNodesByHistory(int topics) {
		List<MapNode<T>> res = new ArrayList<MapNode<T>>();
		if (allNodes != null) {
			int base = allNodes.size() - topics;
			TIntSet invalidIds = new TIntHashSet();
			for (int i = base; res.size() != topics; i--) {
				MapNode<T> node = allNodes.get(i);
				if (!invalidIds.contains(node.getId())) {
					res.add(node);
					markDeps(node, invalidIds);
				}
			}
		}
		return res;
	}

	private void markDeps(MapNode<T> node, TIntSet invalidIds) {
		if (node == null) {
			return;
		}
		invalidIds.add(node.getId());
		markDeps(node.getLeftNode(), invalidIds);
		markDeps(node.getRightNode(), invalidIds);
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
		return root.getTopicFrequency();
	}

	@Override
	public int getTopicForWord(int wordIndex) {
		return wordToTopicIndex.get(wordIndex);
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
	 * @param <T> the word type
	 */
	public static class MyVocab<T> extends DefaultVocab<T> {
		@Override
		public void putEntry(T word, int index) {
			super.putEntry(word, index);
		}
	}
	
	/**
	 * Loads a serialized MapNode list from a file.
	 * @param file file written by object serialization
	 * @param <T> the word type
	 * @return the node list
	 * @throws RuntimeException if reading fails
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<MapNode<T>> loadFile(File file) {
		try {
			ObjectInputStream oi = new ObjectInputStream(new FileInputStream(
					file));
			List<MapNode<T>> res = (List<MapNode<T>>) oi.readObject();
			oi.close();
			return res;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
