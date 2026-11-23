package org.hhn.topicgrouper.eval.classify.impl;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;


/**
 * Rocchio-style classifier: documents are represented as tf-idf vectors over topics, each
 * label is represented by its normalized summed document vector, and a document is
 * assigned to the label with the highest cosine similarity.
 *
 * @param <T> the word type of the documents
 * @param <L> the label type
 */
public abstract class AbstractTopicBasedTfIdfClassifier<T, L> extends AbstractTopicBasedClassifier<T, L> {
	/**
	 * Inverse document frequency per topic position.
	 */
	private final TIntDoubleMap idf;
	// Label vectors
	private final Map<L, double[]> lvs;
	private double[] vHelp;

	/**
	 * Creates an untrained classifier.
	 */
	public AbstractTopicBasedTfIdfClassifier() {
		idf = new TIntDoubleHashMap();
		lvs = new HashMap<L, double[]>();
	}

	/**
	 * Computes the idf values and the normalized label vectors.
	 *
	 * @param provider provider of the labeled training documents
	 */
	public void train(LabelingDocumentProvider<T, L> provider) {
		idf.clear();
		lvs.clear();
		updateTopicIndices();

		List<Document<T>> ds = provider.getDocuments();
		for (int i = 0; i < topicIndices.length; i++) {
			int df = computeDocumentFrequency(ds, topicIndices[i]);
			// Using idf (whith log) improves accuracy considerably (by about 4%)
			// A topic occurring in no training document gets weight 0 (instead
			// of infinity, which would turn the vectors into NaN).
			idf.put(i, df == 0 ? 0 : Math.log(((double) ds.size()) / df));
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

	/**
	 * Computes the tf-idf vector of a document.
	 *
	 * @param d the document
	 * @param v vector of length {@link #getNTopics()} that receives the result (must be zeroed by the caller)
	 */
	protected void computeDocumentVector(Document<T> d, double[] v) {
		computeTopicFrequency(d, v);
		for (int i = 0; i < v.length; i++) {
			v[i] = v[i] * idf.get(i);
		}
	}

	/**
	 * Scales a vector to unit Euclidean length in place; a zero vector is left unchanged.
	 *
	 * @param v the vector to normalize
	 */
	protected void normalize(double[] v) {
		double sum = 0;
		for (int i = 0; i < v.length; i++) {
			sum += v[i] * v[i];
		}
		if (sum == 0) {
			return;
		}
		for (int i = 0; i < v.length; i++) {
			v[i] = v[i] / Math.sqrt(sum);
		}
	}

	/**
	 * Returns the label whose vector has the highest dot product (cosine similarity, as the label vectors are normalized) with the tf-idf vector of the document.
	 *
	 * @param d the document to classify
	 * @return the best label, or <code>null</code> if the classifier is untrained
	 */
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
