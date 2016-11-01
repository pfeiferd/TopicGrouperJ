package org.hhn.topicgrouper.nb.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public abstract class AbstractTopicBasedNBClassifier<T, L> {
	private final Map<L, Map<Integer, Double>> pct;

	public AbstractTopicBasedNBClassifier() {
		pct = new HashMap<L, Map<Integer, Double>>();
	}

	public void train(LabelingDocumentProvider<T, L> provider) {
		pct.clear();
		int ntopics = getNTopics();
		int nDocs = provider.getDocuments().size();
		// Beware empty documents:
		for (LabeledDocument<T, L> d : provider.getLabeledDocuments()) {
			if (d.getSize() == 0) {
				nDocs--;
			}
		}
		double[] pt = computePt();
		for (L label : provider.getAllLabels()) {
			List<LabeledDocument<T, L>> labeledDocs = provider
					.getDocumentsWithLabel(label);
			Map<Integer, Double> m = new HashMap<Integer, Double>();
			pct.put(label, m);
			double[] sum = new double[ntopics];
			for (LabeledDocument<T, L> d : labeledDocs) {
				// Beware empty documents:
				if (d.getSize() > 0) {
					double[] ptd = computePtd(d);
					for (int t = 0; t < ntopics; t++) {
						sum[t] += ptd[t];
					}
				}
			}

			for (int t = 0; t < ntopics; t++) {
				double res = sum[t] / nDocs / pt[t];
				m.put(t, res);
			}
		}
	}

	public L classify(Document<T> d) {
		double bestValue = Double.NEGATIVE_INFINITY;
		L bestLabel = null;
		int ntopics = getNTopics();
		double[] ptd = computePtd(d);
		for (L label : pct.keySet()) {
			double sum = 0;
			for (int t = 0; t < ntopics; t++) {
				sum += log(pct.get(label).get(t)) + log(ptd[t]);
			}
			if (sum >= bestValue) {
				bestValue = sum;
				bestLabel = label;
			}
		}
		if (bestLabel == null) {
			System.out.println("stop");
		}
		return bestLabel;
	}
	
	protected double log(double x) {
		if (x == 0) {
			throw new IllegalStateException("log(0) undefined");
		}
		return Math.log(x);
	}

	protected abstract int getNTopics();

	protected abstract double[] computePtd(Document<T> d);

	protected abstract double[] computePt();
}
