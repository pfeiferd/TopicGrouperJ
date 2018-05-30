package org.hhn.topicgrouper.classify.impl;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;


public abstract class AbstractTopicBasedTfIdfClassifier<T, L> extends AbstractTopicBasedClassifier<T, L> {
	private final TIntDoubleMap idf;
	// Label vectors
	private final Map<L, double[]> lvs;
	private double[] vHelp;

	public AbstractTopicBasedTfIdfClassifier() {
		idf = new TIntDoubleHashMap();
		lvs = new HashMap<L, double[]>();
	}

	public void train(LabelingDocumentProvider<T, L> provider) {
		idf.clear();
		lvs.clear();
		updateTopicIndices();

		List<Document<T>> ds = provider.getDocuments();
		for (int i = 0; i < topicIndices.length; i++) {
			int df = computeDocumentFrequency(ds, topicIndices[i]);
			// Using idf (whith log) improves accuracy considerably (by about 4%)
			idf.put(i, Math.log(((double) ds.size()) / df));
		}

		vHelp = new double[topicIndices.length];
		for (L label : provider.getAllLabels()) {
			double[] lv = new double[topicIndices.length];
			List<LabeledDocument<T, L>> labeledDocs = provider
					.getDocumentsWithLabel(label);
			for (LabeledDocument<T, L> d : labeledDocs) {
				Arrays.fill(vHelp, 0);
				computeDocumentVector(d, vHelp);
				for (int i = 0; i < lv.length; i++) {
					lv[i] = lv[i] + vHelp[i];
				}
			}
			// This is important such that classes with many documen (= large lv)
			// do not get a preference.
			normalize(lv);

			lvs.put(label, lv);
		}
	}

	protected void computeDocumentVector(Document<T> d, double[] v) {
		computeTopicFrequency(d, v);
		for (int i = 0; i < v.length; i++) {
			v[i] = v[i] * idf.get(i);
		}
	}

	protected void normalize(double[] v) {
		double sum = 0;
		for (int i = 0; i < v.length; i++) {
			sum += v[i] * v[i];
		}
		for (int i = 0; i < v.length; i++) {
			v[i] = v[i] / Math.sqrt(sum);
		}
	}

	public L classify(Document<T> d) {
		double[] v = computeTopicFrequencyTest(d);
		for (int i = 0; i < v.length; i++) {
			v[i] = v[i] * idf.get(i);
		}

		double bestValue = 0;
		L bestLabel = null;

		// Uses cosine-similarity.
		// (normalizing d ist not necessary for comparison - just a constant...)
		for (L label : lvs.keySet()) {
			double[] lv = lvs.get(label);
			double sum = 0;
			for (int i = 0; i < lv.length; i++) {
				sum += lv[i] * v[i];
			}
			if (sum >= bestValue) {
				bestValue = sum;
				bestLabel = label;
			}
		}
		return bestLabel;
	}
}
