package org.hhn.topicgrouper.nb.impl;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public abstract class AbstractTopicBasedTfIdfClassifier<T, L> {
	private final TIntDoubleMap idf;
	// Label vectors
	private final Map<L, double[]> lvs;
	private final TIntIntMap topicIndicesBack;
	private int[] topicIndices;
	private double[] vHelp;

	public AbstractTopicBasedTfIdfClassifier() {
		idf = new TIntDoubleHashMap();
		lvs = new HashMap<L, double[]>();
		topicIndicesBack = new TIntIntHashMap();
	}

	public void train(LabelingDocumentProvider<T, L> provider) {
		idf.clear();
		lvs.clear();
		topicIndicesBack.clear();
		topicIndices = getTopicIndices();
		for (int i = 0; i < topicIndices.length; i++) {
			this.topicIndicesBack.put(topicIndices[i], i);
		}
		
		List<Document<T>> ds = provider.getDocuments();
		for (int i = 0; i < topicIndices.length; i++) {
			int df = 0;
			for (Document<T> d : ds) {
				TIntIterator it = d.getWordIndices().iterator();
				while (it.hasNext()) {
					int wordIndex = it.next();
					int fr = d.getWordFrequency(wordIndex);
					if (fr > 0 && getTopicIndex(wordIndex) == topicIndices[i]) {
						df++;
						break;
					}
				}
			}
			idf.put(i, Math.log(((double) ds.size()) / df));
		}

		int nDocs = provider.getDocuments().size();
		vHelp = new double[topicIndices.length];
		for (L label : provider.getAllLabels()) {
			double[] lv = new double[topicIndices.length];
			List<LabeledDocument<T, L>> labeledDocs = provider
					.getDocumentsWithLabel(label);
			for (LabeledDocument<T, L> d : labeledDocs) {
				computeDV(d, vHelp);
				for (int i = 0; i < lv.length; i++) {
					lv[i] += vHelp[i] / nDocs;
				}
			}

			lvs.put(label, lv);
		}
	}
	
	protected void computeDV(Document<T> d, double[] v) {
		Arrays.fill(v, 0);
		computTopicFrequency(d, v);
		double sum = 0;
		for (int i = 0; i < v.length; i++) {
			v[i] *= idf.get(i);
			sum += v[i] * v[i];
		}
		for (int i = 0; i < v.length; i++) {
			v[i] /= Math.sqrt(sum);
		}		
	}
	
	protected void computTopicFrequency(Document<T> d, double[] v) {
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int wordIndex = it.next();
			int fr = d.getWordFrequency(wordIndex);
			v[this.topicIndicesBack.get(getTopicIndex(wordIndex))] += fr;
		}		
	}
	
	public L classify(Document<T> d) {
		computeDV(d, vHelp);
		
		double bestValue = 0;
		L bestLabel = null;
		
		for (L label : lvs.keySet()) {
			double[] lv = lvs.get(label);
			double sum = 0;
			for (int i = 0; i < lv.length; i++) {
				sum += lv[i] * vHelp[i];
			}
			if (sum >= bestValue) {
				bestValue = sum;
				bestLabel = label;
			}
		}
		return bestLabel;
	}

	// protected double log(double x) {
	// if (x == 0) {
	// throw new IllegalStateException("log(0) undefined");
	// }
	// return Math.log(x);
	// }

	protected abstract int getTopicIndex(int wordIndex);

	protected abstract int[] getTopicIndices();
}
