package org.hhn.topicgrouper.classify.impl;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public abstract class AbstractTopicBasedNBClassifier<T, L> extends AbstractTopicBasedClassifier<T, L> {
	private final List<L> labels;
	private final List<TDoubleList> logptc;
	private final TDoubleList logpc;
	
	private final double smoothingLambda;

	public AbstractTopicBasedNBClassifier(double lambda) {
		logptc = new ArrayList<TDoubleList>();
		logpc = new TDoubleArrayList();
		labels = new ArrayList<L>();
		this.smoothingLambda = lambda;
	}

	public void train(LabelingDocumentProvider<T, L> provider) {
		logptc.clear();
		logpc.clear();
		labels.clear();
		updateTopicIndices();

		int ntopics = getNTopics();
		int nDocs = provider.getDocuments().size();
		double lambdaSum = smoothingLambda * ntopics;
		double[] sum = new double[ntopics];
		
		int l = 0;		
		for (L label : provider.getAllLabels()) {
			labels.add(label);
			List<LabeledDocument<T, L>> labeledDocs = provider
					.getDocumentsWithLabel(label);
			TDoubleList pt = logptc.size() <= l ? null : logptc.get(l);
			if (pt == null) {
				pt = new TDoubleArrayList();
				logptc.add(pt);
			}
			else {
				pt.clear();
			}
			logpc.add(Math.log(((double) labeledDocs.size()) / nDocs));
			int total = 0;
			Arrays.fill(sum, 0);
			for (LabeledDocument<T, L> d : labeledDocs) {
				computeTopicFrequency(d, sum, true);
				total += d.getSize();
			}

			for (int t = 0; t < ntopics; t++) {
				pt.add(Math.log((sum[t] + smoothingLambda) / (total + lambdaSum)));
			}
			l++;
		}
	}
	
	protected int getNTopics() {
		return topicIndices.length;
	}

	public L classify(Document<T> d) {
		double bestValue = Double.NEGATIVE_INFINITY;
		L bestLabel = null;
		int ntopics = getNTopics();
		double[] ftd = new double[ntopics];
		computeTopicFrequencyTest(d, ftd, true);
		int l = 0;
		for (L label : labels) {
			double sum = logpc.get(l);
			for (int t = 0; t < ntopics; t++) {
				sum += logptc.get(l).get(t) * ftd[t];
			}
			if (sum >= bestValue) {
				bestValue = sum;
				bestLabel = label;
			}
			l++;
		}
		return bestLabel;
	}
}
