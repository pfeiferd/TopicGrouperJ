package org.hhn.topicgrouper.classify.impl.tg;

import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.tg.report.store.MapNode;

public class TGIGMixNBClassifier2<T, L> extends
		AbstractTopicBasedNBClassifier<T, L> {
	private int[] topicIds;
	private final TIntObjectMap<NodeAndScore<T>> topicIdToNode;
	private final TIntIntMap shortenedWordToTopicMap;
	private final List<MapNode<T>> allNodes;
	private final TIntObjectMap<NodeAndScore<T>> wordIndexToNode;

	private final LabelingDocumentProvider<T, L> provider;

	public TGIGMixNBClassifier2(List<MapNode<T>> allNodes,
			LabelingDocumentProvider<T, L> provider, File saveListFile)
			throws IOException, ClassNotFoundException {
		super(0);
		this.allNodes = allNodes;
		this.provider = provider;

		shortenedWordToTopicMap = new TIntIntHashMap();
		wordIndexToNode = new TIntObjectHashMap<NodeAndScore<T>>();
		topicIdToNode = new TIntObjectHashMap<NodeAndScore<T>>();

		computeIGTopics(provider, saveListFile);
	}

	protected TIntObjectMap<NodeAndScore<T>> computeIGTopics(
			LabelingDocumentProvider<T, L> provider, File saveListFile)
			throws IOException, ClassNotFoundException {
		if (saveListFile != null && saveListFile.canRead()) {
			ObjectInputStream is = new ObjectInputStream(new FileInputStream(
					saveListFile));
			List<NodeAndScore<T>> l = (List<NodeAndScore<T>>) is.readObject();
			for (NodeAndScore<T> ns : l) {
				MapNode<T> node = ns.node;
				topicIdToNode.put(node.getId(), ns);
				if (node.getLeftNode() == null && node.getRightNode() == null) {
					wordIndexToNode.put(node.getTopTopicWordInfos().get(0)
							.getWordId(), ns);
				}
			}
			is.close();
		} else {
			for (MapNode<T> node : allNodes) {
				NodeAndScore<T> ns = new NodeAndScore<T>(node);
				topicIdToNode.put(node.getId(), ns);
				if (node.getLeftNode() == null && node.getRightNode() == null) {
					wordIndexToNode.put(node.getTopTopicWordInfos().get(0)
							.getWordId(), ns);
				}
			}

			List<NodeAndScore<T>> l = new ArrayList<TGIGMixNBClassifier2.NodeAndScore<T>>();
			TObjectIntMap<L> posMap = new TObjectIntHashMap<L>();
			TObjectIntMap<L> negMap = new TObjectIntHashMap<L>();
			TIntObjectIterator<NodeAndScore<T>> it = topicIdToNode.iterator();
			int c = 0;
			while (it.hasNext()) {
				it.advance();
				NodeAndScore<T> ns = it.value();
				ns.score = computeScore(ns.node.getId(), provider, posMap,
						negMap);
				l.add(ns);
				System.out.println(c++);
			}
			if (saveListFile != null) {
				ObjectOutputStream os = new ObjectOutputStream(
						new FileOutputStream(saveListFile));
				os.writeObject(l);
				os.close();
			}
		}
		return topicIdToNode;
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
		MapNode<T> node = wordIndexToNode.get(wordIndex).node;
		while (node != null) {
			if (node.getId() == topicIndex) {
				return true;
			}
			node = node.getParent();
		}

		return false;
	}

	public void setMaxTopicsAlt(int n) {
		TIntObjectIterator<NodeAndScore<T>> it = topicIdToNode.iterator();
		List<NodeAndScore<T>> mergableNodes = new ArrayList<TGIGMixNBClassifier2.NodeAndScore<T>>();
		while (it.hasNext()) {
			it.advance();
			NodeAndScore<T> ns = it.value();
			ns.collapsed = ns.node.getLeftNode() == null
					&& ns.node.getRightNode() == null;
			if (!ns.collapsed) {
				NodeAndScore<T> ns1 = topicIdToNode.get(ns.node.getLeftNode()
						.getId());
				NodeAndScore<T> ns2 = topicIdToNode.get(ns.node.getRightNode()
						.getId());

				double a = ns1.node.getTopicFrequency();
				double b = ns2.node.getTopicFrequency();
				double sum = a + b;

				ns.igDiffScore = a / sum * ns1.score + b / sum * ns2.score
						- ns.score;
			}
		}
		it = topicIdToNode.iterator();
		while (it.hasNext()) {
			it.advance();
			NodeAndScore<T> ns = it.value();
			if (!ns.collapsed) {
				NodeAndScore<T> ns1 = topicIdToNode.get(ns.node.getLeftNode()
						.getId());
				NodeAndScore<T> ns2 = topicIdToNode.get(ns.node.getRightNode()
						.getId());
				if (ns1.collapsed && ns2.collapsed) {
					mergableNodes.add(ns);
				}
			}
		}
		Collections.sort(mergableNodes, comp);
		int max = provider.getVocab().getNumberOfWords() - n;
		for (int i = 0; i < max; i++) {
			NodeAndScore<T> ns = mergableNodes.remove(mergableNodes.size() - 1);
			ns.collapsed = true;
			MapNode<T> parent = ns.node.getParent();
			if (parent != null) {
				if (ns.getSibling(topicIdToNode).collapsed) {
					NodeAndScore<T> parentNs = topicIdToNode
							.get(parent.getId());
					int pos = Collections.binarySearch(mergableNodes, parentNs,
							comp);
					if (pos < 0) {
						pos = -pos - 1;
					}
					mergableNodes.add(parentNs);
				}
			}
		}

		topicIds = new int[n];
		it = topicIdToNode.iterator();
		int i = 0;
		while (it.hasNext()) {
			it.advance();
			NodeAndScore<T> ns = it.value();
			if (ns.collapsed) {
				MapNode<T> parent = ns.node.getParent();
				if (parent == null
						|| !topicIdToNode.get(parent.getId()).collapsed) {
					topicIds[i++] = ns.node.getId();
				}
			}
		}

		updateShortenedWordMap();
	}

	public void setMaxTopics(int n) {
		TIntObjectIterator<NodeAndScore<T>> it = topicIdToNode.iterator();
		while (it.hasNext()) {
			it.advance();
			NodeAndScore<T> ns = it.value();
			ns.pruned = false;
		}
		List<NodeAndScore<T>> prunableNodes = new ArrayList<TGIGMixNBClassifier2.NodeAndScore<T>>();
		it = wordIndexToNode.iterator();
		while (it.hasNext()) {
			it.advance();
			prunableNodes.add(it.value());
		}
		Collections.sort(prunableNodes);

		while (prunableNodes.size() > n) {
			// Prune
			NodeAndScore<T> ns = prunableNodes.remove(prunableNodes.size() - 1);
			ns.pruned = true;

			MapNode<T> parent = ns.node.getParent();
			NodeAndScore<T> parentNs = topicIdToNode.get(parent.getId());

			NodeAndScore<T> sibling = ns.getSibling(topicIdToNode);
			if (sibling.pruned) {
				int pos = Collections.binarySearch(prunableNodes, parentNs);
				if (pos < 0) {
					pos = -pos - 1;
				}
				prunableNodes.add(pos, parentNs);
			}
		}

		topicIds = new int[n];
		for (int i = 0; i < prunableNodes.size(); i++) {
			topicIds[i] = prunableNodes.get(i).node.getId();
		}

		updateShortenedWordMap();
	}

	private void updateShortenedWordMap() {
		// TIntList topicWords = new TIntArrayList();
		// for (int i = 0; i < topicIds.length; i++) {
		// topicWords.clear();
		// collectTopicWords(topicIdToNode.get(topicIds[i]).node, topicWords);
		// for (int j = 0; j < topicWords.size(); j++) {
		// System.out
		// .print(provider.getVocab().getWord(topicWords.get(j)));
		// System.out.print(" ");
		// }
		// System.out.println();
		// }
		// System.out.println("------");
		shortenedWordToTopicMap.clear();
		TIntObjectIterator<NodeAndScore<T>> it = wordIndexToNode.iterator();
		while (it.hasNext()) {
			it.advance();
			for (int i = 0; i < topicIds.length; i++) {
				if (topicContainsWord(topicIds[i], it.key())) {
					shortenedWordToTopicMap.put(it.key(), topicIds[i]);
					break;
				}
			}
		}
	}

	public double averageTopicLength() {
		int sum = 0;
		for (int i = 0; i < topicIds.length; i++) {
			sum += MapNode.countTopicWords(topicIdToNode.get(topicIds[i]).node);
		}
		return ((double) sum) / topicIds.length;
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
		private double score;
		private double igDiffScore;
		private boolean pruned;
		private boolean collapsed;

		public NodeAndScore(MapNode<T> node) {
			this.node = node;
			this.pruned = false;
			this.collapsed = false;
		}

		public NodeAndScore<T> getSibling(
				TIntObjectMap<NodeAndScore<T>> topicIdToNode) {
			if (node.getParent() != null) {
				NodeAndScore<T> parentNs = topicIdToNode.get(node.getParent()
						.getId());
				if (parentNs.node.getLeftNode() == node) {
					return topicIdToNode.get(parentNs.node.getRightNode()
							.getId());
				} else {
					return topicIdToNode.get(parentNs.node.getLeftNode()
							.getId());
				}
			}
			return null;
		}

		@Override
		public int compareTo(NodeAndScore<T> o) {
			if (o.score == score) {
				return 0;
			}
			return o.score < score ? 1 : -1;
		}
	}

	private Comparator<NodeAndScore<T>> comp = new Comparator<NodeAndScore<T>>() {
		@Override
		public int compare(NodeAndScore<T> o1, NodeAndScore<T> o2) {
			if (o1.igDiffScore == o2.igDiffScore) {
				return 0;
			}
			return o1.igDiffScore > o2.igDiffScore ? 1 : -1;
		}
	};
}
