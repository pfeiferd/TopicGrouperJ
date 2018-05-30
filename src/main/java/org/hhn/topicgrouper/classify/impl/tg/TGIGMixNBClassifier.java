package org.hhn.topicgrouper.classify.impl.tg;

import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.tg.report.store.MapNode;

public class TGIGMixNBClassifier<T, L> extends
		AbstractTopicBasedNBClassifier<T, L> {
	private int[] topicIds;
	private final int[] bestIGTopicsSorted;
	private final TIntIntMap wordToTopicMap;
	private final TIntIntMap shortenedWordToTopicMap;
	private final List<MapNode<T>> allNodes;
	private final TIntObjectMap<MapNode<T>> wordIndexToNode;

	public TGIGMixNBClassifier(List<MapNode<T>> allNodes,
			LabelingDocumentProvider<T, L> provider, File saveListFile)
			throws IOException, ClassNotFoundException {
		super(0);
		this.allNodes = allNodes;

		wordToTopicMap = new TIntIntHashMap();
		shortenedWordToTopicMap = new TIntIntHashMap();
		wordIndexToNode = new TIntObjectHashMap<MapNode<T>>();
		for (MapNode<T> node : allNodes) {
			if (node.getLeftNode() == null && node.getRightNode() == null) {
				wordIndexToNode.put(node.getTopTopicWordInfos().get(0)
						.getWordId(), node);
			}
		}

		bestIGTopicsSorted = computeBestIGTopicsSorted(provider,
				wordToTopicMap, saveListFile);
	}

	protected int[] computeBestIGTopicsSorted(
			LabelingDocumentProvider<T, L> provider, TIntIntMap wordToTopicMap,
			File saveListFile) throws IOException, ClassNotFoundException {
		List<NodeAndScore<T>> l;
		if (saveListFile != null && saveListFile.canRead()) {
			ObjectInputStream is = new ObjectInputStream(new FileInputStream(
					saveListFile));
			l = (List<NodeAndScore<T>>) is.readObject();
		} else {
			l = new ArrayList<TGIGMixNBClassifier.NodeAndScore<T>>();
			TObjectIntMap<L> posMap = new TObjectIntHashMap<L>();
			TObjectIntMap<L> negMap = new TObjectIntHashMap<L>();

			for (MapNode<T> node : allNodes) {
				double score = computeScore(node.getId(), provider, posMap,
						negMap);
				l.add(new NodeAndScore<T>(node, score));
			}
			if (saveListFile != null) {
				ObjectOutputStream os = new ObjectOutputStream(
						new FileOutputStream(saveListFile));
				os.writeObject(l);
				os.close();
			}
		}

		Collections.sort(l);

		TIntList topicWords = new TIntArrayList();
		for (int i = 0; i < l.size();) {
			MapNode<T> node = l.get(i).getNode();
			topicWords.clear();
			node.collectTopicWords(topicWords);
			if (topicWords.size() > 10) {
				l.remove(i);
			} else {
				TIntIterator it = topicWords.iterator();
				while (it.hasNext()) {
					int wordIndex = it.next();
					wordToTopicMap.put(wordIndex, node.getId());
					for (int j = i + 1; j < l.size();) {
						MapNode<T> node2 = l.get(j).getNode();
						if (topicContainsWord(node2.getId(), wordIndex)) {
							l.remove(j);
						} else {
							j++;
						}
					}
				}
				i++;
			}
		}

		int[] res = new int[l.size()];
		for (int i = 0; i < l.size(); i++) {
			MapNode<T> node = l.get(i).getNode();
			res[i] = node.getId();
		}

		try {
			PrintStream ps = new PrintStream(new FileOutputStream(new File(
					"./target/mixout.txt")));
			for (NodeAndScore<T> ns : l) {
				topicWords.clear();
				MapNode.collectTopicWords(ns.getNode(), topicWords);
				ps.print("Topic id: " + ns.getNode().getId() + " ");
				ps.println(topicWords);
				for (int i = 0; i < topicWords.size(); i++) {
					ps.print(provider.getVocab().getWord(topicWords.get(i)));
					ps.print(" ");
					ps.print(provider.getWordFrequency(topicWords.get(i)));
					ps.print(" ");
				}
				ps.println();
			}
			ps.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		return res;
	}


	// The smaller the better...
	protected double computeScore(int topicIndex,
			LabelingDocumentProvider<T, L> documentProvider,
			TObjectIntMap<L> posMap, TObjectIntMap<L> negMap) {
		// Entropy (here same as Information Gain as score.)

		posMap.clear();
		negMap.clear();
		int wPosCount = 0;
		int wNegCount = 0;
		int docs = 0;
		int labels = documentProvider.getAllLabels().size();

		for (LabeledDocument<T, L> d : documentProvider.getLabeledDocuments()) {
			if (containsTopic(d, topicIndex)) {
				wPosCount++;
				int count = posMap.get(d.getLabel());
				if (count == Constants.DEFAULT_INT_NO_ENTRY_VALUE) {
					count = 0;
				}
				posMap.put(d.getLabel(), count + 1);
			} else {
				wNegCount++;
				int count = negMap.get(d.getLabel());
				if (count == Constants.DEFAULT_INT_NO_ENTRY_VALUE) {
					count = 0;
				}
				negMap.put(d.getLabel(), count + 1);
			}
			docs++;
		}
		double sumPos = 0;
		double sumNeg = 0;
		for (L label : documentProvider.getAllLabels()) {
			double pLPosW = ((double) (posMap.get(label) + 1))
					/ (wPosCount + labels); // Laplace smoothing
			sumPos += pLPosW == 0 ? 0 : pLPosW * Math.log(pLPosW);
			double pLNegW = ((double) (negMap.get(label) + 1))
					/ (wNegCount + labels); // Laplace smoothing
			sumNeg += pLNegW == 0 ? 0 : pLNegW * Math.log(pLNegW);
		}
		return -(sumPos * wPosCount / docs) - (sumNeg * wNegCount / docs);
	}

	protected boolean containsTopic(Document<T> d, int topicIndex) {
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int wordIndex = it.next();
			int fr = d.getWordFrequency(wordIndex);
			if (fr > 0 && topicContainsWord(topicIndex, wordIndex)) {
				return true;
			}
		}

		return false;
	}

	protected boolean topicContainsWord(int topicIndex, int wordIndex) {
		MapNode<T> node = wordIndexToNode.get(wordIndex);
		while (node != null) {
			if (node.getId() == topicIndex) {
				return true;
			}
			node = node.getParent();
		}

		return false;
	}

	public void setMaxTopics(int n) {
		topicIds = Arrays.copyOf(bestIGTopicsSorted, n);
		shortenedWordToTopicMap.clear();
		TIntIntIterator it = wordToTopicMap.iterator();
		while (it.hasNext()) {
			it.advance();
			int topicId = it.value();
			for (int i = 0; i < topicIds.length; i++) {
				if (topicIds[i] == topicId) {
					shortenedWordToTopicMap.put(it.key(), topicId);
					break;
				}
			}
		}
	}

	@Override
	protected int[] getTopicIndices() {
		return topicIds;
	}

	@Override
	protected int getTopicIndex(int wordIndex) {
		if (!shortenedWordToTopicMap.containsKey(wordIndex)) {
			return Integer.MIN_VALUE;
		}
		return shortenedWordToTopicMap.get(wordIndex);
	}

	@SuppressWarnings("serial")
	private static class NodeAndScore<T> implements
			Comparable<NodeAndScore<T>>, Serializable {
		private final MapNode<T> node;
		private final double score;

		public NodeAndScore(MapNode<T> node, double score) {
			this.node = node;
			this.score = score;
		}

		public MapNode<T> getNode() {
			return node;
		}

		@SuppressWarnings("unused")
		public double getScore() {
			return score;
		}

		@Override
		public int compareTo(NodeAndScore<T> o) {
			if (o.score == score) {
				return 0;
			}
			return o.score < score ? 1 : -1;
		}
	}
}
