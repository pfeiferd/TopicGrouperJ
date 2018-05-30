package org.hhn.topicgrouper.classify.impl;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.validation.PeakValueOptimizer;

public abstract class AbstractTopicBasedNBClassifier<T, L> extends
		AbstractTopicBasedClassifier<T, L> {
	protected final List<L> labels;
	protected final List<TDoubleList> ptc;
	protected final TDoubleList logpc;

	private double smoothingLambda;

	public AbstractTopicBasedNBClassifier(double lambda) {
		ptc = new ArrayList<TDoubleList>();
		logpc = new TDoubleArrayList();
		labels = new ArrayList<L>();
		smoothingLambda = lambda;
	}

	public double getSmoothingLambda() {
		return smoothingLambda;
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
			final LabelingDocumentProvider<T, L> provider, int steps,
			final boolean micro) {
		PeakValueOptimizer peakValueOptimizer = new PeakValueOptimizer() {
			public double test(double value) {
				smoothingLambda = value;
				return AbstractTopicBasedNBClassifier.this
						.test(provider, null)[micro ? 0 : 1];
			}
		};
		smoothingLambda = peakValueOptimizer.optimizeLambda(minLambda,
				maxLambda, steps);
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

	protected double[] computeTopicFrequencyTest(Document<T> d) {
		double[] ftd = new double[getNTopics()];
		computeTopicFrequency(d, ftd);

		return ftd;
	}

	protected double getLogPtc(int l, int topic) {
		TDoubleList pt = ptc.get(l);
		double total = pt.get(0);
		double sumt = pt.get(topic + 1);

		double[] ptopic = getPt();
		if (smoothingLambda > 0 && ptopic != null) {
			return Math.log((sumt / total) * (1 - smoothingLambda)
					+ smoothingLambda * Math.exp(ptopic[topic]));
		} else {
			return Math.log((sumt + 1) / (total + 1 * getNTopics()));
		}
	}

	protected double[] getPt() {
		return null;
	}
}
