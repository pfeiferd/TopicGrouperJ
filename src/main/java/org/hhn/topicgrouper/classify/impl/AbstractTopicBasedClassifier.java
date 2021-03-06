package org.hhn.topicgrouper.classify.impl;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.List;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

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

	protected int getNTopics() {
		return topicIndices.length;
	}

	protected void computeTopicFrequency(Document<T> d, double[] v) {
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int wordIndex = it.next();
			int fr = d.getWordFrequency(wordIndex);
			int ti = getTopicIndex(wordIndex);
			if (ti != Integer.MIN_VALUE) {
				v[this.topicIndicesBack.get(ti)] += fr;
			}
		}
	}

	protected double[] computeTopicFrequencyTest(Document<T> d) {
		double[] ftd = new double[getNTopics()];
		computeTopicFrequency(d, ftd);
		return ftd;
	}

	protected int computeDocumentFrequency(List<Document<T>> ds, int topicIndex) {
		int df = 0;
		for (Document<T> d : ds) {
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int wordIndex = it.next();
				int fr = d.getWordFrequency(wordIndex);
				int ti = getTopicIndex(wordIndex);
				if (ti != Integer.MIN_VALUE) {
					if (fr > 0 && ti == topicIndex) {
						df++;
						break;
					}
				}
			}
		}
		return df;
	}

	@Override
	public double[] test(LabelingDocumentProvider<T, L> testProvider,
			double[] res) {
		int hits = 0;
		int tests = 0;
		int usedLabels = 0;
		double macroAvg = 0;
		if (res == null) {
			res = new double[2];
		}

		for (L label : testProvider.getAllLabels()) {
			int labelHits = 0;
			int labelTests = 0;
			for (LabeledDocument<T, L> dt : testProvider
					.getDocumentsWithLabel(label)) {
				L clabel = classify(dt);
				if (clabel != null && clabel.equals(label)) {
					labelHits++;
					hits++;
				}
				labelTests++;
				tests++;
			}
			usedLabels += labelTests == 0 ? 0 : 1;
			macroAvg += labelTests == 0 ? 0 : ((double) labelHits) / labelTests;
		}

		res[0] = ((double) hits) / tests;
		res[1] = macroAvg / usedLabels;
		return res;
	}

	protected abstract int[] getTopicIndices();

	protected abstract int getTopicIndex(int wordIndex);
}
