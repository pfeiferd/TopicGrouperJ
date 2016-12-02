package org.hhn.topicgrouper.classify.impl;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public abstract class AbstractTopicBasedNBClassifier<T, L> extends
		AbstractTopicBasedClassifier<T, L> {
	private final List<L> labels;
	private final List<TDoubleList> ptc;
	private final TDoubleList logpc;

	private double smoothingLambda;

	public AbstractTopicBasedNBClassifier(double lambda) {
		ptc = new ArrayList<TDoubleList>();
		logpc = new TDoubleArrayList();
		labels = new ArrayList<L>();
		smoothingLambda = lambda;
	}

	public void train(LabelingDocumentProvider<T, L> provider) {
		ptc.clear();
		logpc.clear();
		labels.clear();
		updateTopicIndices();

		int ntopics = getNTopics();
		int nDocs = provider.getDocuments().size();
		double[] sum = new double[ntopics];

		int l = 0;
		for (L label : provider.getAllLabels()) {
			labels.add(label);
			List<LabeledDocument<T, L>> labeledDocs = provider
					.getDocumentsWithLabel(label);
			TDoubleList pt = ptc.size() <= l ? null : ptc.get(l);
			if (pt == null) {
				pt = new TDoubleArrayList();
				ptc.add(pt);
			} else {
				pt.clear();
			}
			logpc.add(Math.log(((double) labeledDocs.size()) / nDocs));
			int total = 0;
			Arrays.fill(sum, 0);
			for (LabeledDocument<T, L> d : labeledDocs) {
				computeTopicFrequency(d, sum);
				total += d.getSize();
			}
			pt.add(total);
			for (int t = 0; t < ntopics; t++) {
				pt.add(sum[t]);
			}
			l++;
		}
	}

	public void optimizeLambda(double minLambda, double maxLambda,
			LabelingDocumentProvider<T, L> provider, int steps, boolean micro) {
		smoothingLambda = (minLambda + maxLambda) / 2;
		double middleRes = test(provider, micro);
		optimizeLambda(minLambda, maxLambda, provider, steps, micro, middleRes);
	}

	// Assumes that we have a function that monotically rises up to its max and then monotically falls again (or just one of the two).
	// We are tying to find the max.
	public void optimizeLambda(double minLambda, double maxLambda,
			LabelingDocumentProvider<T, L> provider, int steps, boolean micro,
			double res2) {
		//System.out.println(steps + " " + minLambda + " " + maxLambda + " " + res2);
		if (steps <= 0) {
			smoothingLambda = (minLambda + maxLambda) / 2;
			return;
		}
		double diff = maxLambda - minLambda;
		smoothingLambda = minLambda + diff * 0.25;
		double res1 = test(provider, micro);
		smoothingLambda = minLambda + diff * 0.75;
		double res3 = test(provider, micro);

		if (res1 < res2 && res2 < res3) {
			optimizeLambda((minLambda + maxLambda) / 2, maxLambda, provider, steps - 1, micro, res3);
		} else if (res1 > res2 && res2 > res3) {
			optimizeLambda(minLambda, (minLambda + maxLambda) / 2, provider, steps - 1, micro, res1);
		} else if (res1 < res2 && res2 > res3) {
			optimizeLambda(minLambda + diff * 0.25, minLambda + diff * 0.75, provider, steps - 1, micro, res2);
		} else if (res1 > res3) {
			//throw new IllegalStateException("unexpected case");
			optimizeLambda(minLambda, (minLambda + maxLambda) / 2, provider, steps - 1, micro, res1);
		} else {
			//throw new IllegalStateException("unexpected case");
			optimizeLambda((minLambda + maxLambda) / 2, maxLambda, provider, steps - 1, micro, res3);			
		}
	}

	public L classify(Document<T> d) {
		double bestValue = Double.NEGATIVE_INFINITY;
		L bestLabel = null;
		int ntopics = getNTopics();
		double[] ftd = computeTopicFrequencyTest(d);
		int l = 0;
		for (L label : labels) {
			double sum = logpc.get(l);
			for (int t = 0; t < ntopics; t++) {
				sum += getLogPtc(l, t) * ftd[t];
			}
			if (sum >= bestValue) {
				bestValue = sum;
				bestLabel = label;
			}
			l++;
		}
		return bestLabel;
	}

	protected double getLogPtc(int l, int topic) {
		TDoubleList pt = ptc.get(l);
		double total = pt.get(0);
		double sumt = pt.get(topic + 1);

		return Math.log((sumt + smoothingLambda)
				/ (total + smoothingLambda * getNTopics()));
	}
}
