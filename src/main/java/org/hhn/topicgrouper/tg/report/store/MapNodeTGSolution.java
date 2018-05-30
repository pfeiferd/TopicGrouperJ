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

public class MapNodeTGSolution<T> implements TGSolution<T> {
	private final MapNode<T> root;
	private final List<MapNode<T>> allNodes;
	private int nTopics;
	private TIntList[] topics;
	private int[] topicIds;
	private double[] logLikelihoods;
	private double totalLikelihood;
	private final TIntObjectMap<MapNode<T>> idToNode;
	private final TIntIntMap wordToTopicIndex;
	private final TIntIntMap wordToFrequency;
	private final MyVocab<T> vocab;
	private final int maxTopics;

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
	
	public int getMaxTopics() {
		return maxTopics;
	}
	
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
	public TIntCollection getHomonymns() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double[] getTopicLogLikelihoods() {
		return logLikelihoods;
	}
	
	public static class MyVocab<T> extends DefaultVocab<T> {
		@Override
		public void putEntry(T word, int index) {
			super.putEntry(word, index);
		}
	}
	
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
