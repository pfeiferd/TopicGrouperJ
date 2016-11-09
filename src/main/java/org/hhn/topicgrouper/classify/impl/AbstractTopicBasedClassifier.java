package org.hhn.topicgrouper.classify.impl;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.doc.Document;

public abstract class AbstractTopicBasedClassifier<T, L> implements
		SupervisedDocumentClassifier<T, L> {
	protected final TIntIntMap topicIndicesBack;
	protected int[] topicIndices;

	public AbstractTopicBasedClassifier() {
		topicIndicesBack = new TIntIntHashMap();
	}

	protected void updateTopicIndices() {
		topicIndices = getTopicIndices();
		topicIndicesBack.clear();
		for (int i = 0; i < topicIndices.length; i++) {
			this.topicIndicesBack.put(topicIndices[i], i);
		}
	}

	protected void computeTopicFrequency(Document<T> d, double[] v, boolean add) {
		if (!add) {
			Arrays.fill(v, 0);
		}
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int wordIndex = it.next();
			int fr = d.getWordFrequency(wordIndex);
			v[this.topicIndicesBack.get(getTopicIndex(wordIndex))] += fr;
		}
	}

	protected abstract int[] getTopicIndices();

	protected abstract int getTopicIndex(int wordIndex);
}
