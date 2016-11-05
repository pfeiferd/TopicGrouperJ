package org.hhn.topicgrouper.classify.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public abstract class AbstractTopicBasedNBClassifier<T, L> implements SupervisedDocumentClassifier<T, L> {
	private final Map<L, Map<Integer, Double>> logptc;
	private final Map<L, Double> logpc;
	
	private final double smoothingLambda;

	public AbstractTopicBasedNBClassifier(double lambda) {
		logptc = new HashMap<L, Map<Integer, Double>>();
		logpc = new HashMap<L, Double>();
		this.smoothingLambda = lambda;
	}

	public void train(LabelingDocumentProvider<T, L> provider) {
		logptc.clear();
		logpc.clear();

		int ntopics = getNTopics();
		int nDocs = provider.getDocuments().size();
		double lambdaSum = smoothingLambda * ntopics;
		for (L label : provider.getAllLabels()) {
			List<LabeledDocument<T, L>> labeledDocs = provider
					.getDocumentsWithLabel(label);
			Map<Integer, Double> m = new HashMap<Integer, Double>();
			logptc.put(label, m);
			logpc.put(label, log(((double) labeledDocs.size()) / nDocs));
			double[] sum = new double[ntopics];
			for (LabeledDocument<T, L> d : labeledDocs) {
				double[] ftd = getFtd(d);
				for (int t = 0; t < ntopics; t++) {
					sum[t] += ftd[t];
				}
			}

			for (int t = 0; t < ntopics; t++) {
				m.put(t, log((sum[t] + smoothingLambda) / (nDocs + lambdaSum)));
			}
		}
	}

	public L classify(Document<T> d) {
		double bestValue = Double.NEGATIVE_INFINITY;
		L bestLabel = null;
		int ntopics = getNTopics();
		double[] ftd = getFtd(d);
		for (L label : logptc.keySet()) {
			double sum = logpc.get(label);
			for (int t = 0; t < ntopics; t++) {
				sum += logptc.get(label).get(t) * ftd[t];
			}
			if (sum >= bestValue) {
				bestValue = sum;
				bestLabel = label;
			}
		}
		return bestLabel;
	}

	protected double log(double x) {
		if (x == 0) {
			throw new IllegalStateException("log(0) undefined");
		}
		return Math.log(x);
	}

	protected abstract double[] getFtd(Document<T> d);

	protected abstract int getNTopics();
}
