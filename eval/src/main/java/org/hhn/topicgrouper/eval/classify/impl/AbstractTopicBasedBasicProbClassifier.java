package org.hhn.topicgrouper.eval.classify.impl;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.validation.PeakValueOptimizer;

/**
 * Basic probabilistic classifier over topic frequencies: a document gets the label
 * with the highest log prior plus the topic frequencies weighted by the log
 * class-conditional topic probabilities. These probabilities are either smoothed by
 * interpolation with the topic prior ({@link #getPt()}, if lambda &gt; 0) or by
 * Laplace smoothing.
 *
 * @param <T> the word type of the documents
 * @param <L> the label type
 */
public abstract class AbstractTopicBasedBasicProbClassifier<T, L> extends
		AbstractTopicBasedClassifier<T, L> {
	/**
	 * The labels seen in training; the position is the label's index in {@link #ptc} and {@link #logpc}.
	 */
	protected final List<L> labels;
	/**
	 * Per label: element 0 holds the sum of the topic counts of the label's documents (words
	 * without topic are not counted), elements 1..n the counts per topic (position + 1).
	 */
	protected final List<TDoubleList> ptc;
	/**
	 * Log prior probability of each label (relative number of training documents).
	 */
	protected final TDoubleList logpc;

	private double smoothingLambda;

	/**
	 * Creates an untrained classifier.
	 *
	 * @param lambda smoothing weight in [0, 1] for interpolating with the topic prior; with 0 (or if no prior is available) Laplace smoothing is used
	 */
	public AbstractTopicBasedBasicProbClassifier(double lambda) {
		ptc = new ArrayList<TDoubleList>();
		logpc = new TDoubleArrayList();
		labels = new ArrayList<L>();
		smoothingLambda = lambda;
	}

	/**
	 * The smoothing weight, as passed to the constructor or found by {@link #optimizeLambda(double, double, LabelingDocumentProvider, int, boolean)}.
	 *
	 * @return the current smoothing weight lambda
	 */
	public double getSmoothingLambda() {
		return smoothingLambda;
	}

	/**
	 * Estimates label priors and per-label topic counts from the training documents.
	 *
	 * @param provider provider of the labeled training documents
	 */
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
			Arrays.fill(sum, 0);
			for (LabeledDocument<T, L> d : labeledDocs) {
				computeTopicFrequency(d, sum);
			}
			// The topic distribution of the label is normalized over the
			// topics, so the total must not include words without topic.
			double total = 0;
			for (int t = 0; t < ntopics; t++) {
				total += sum[t];
			}
			pt.add(total);
			for (int t = 0; t < ntopics; t++) {
				pt.add(sum[t]);
			}
			l++;
		}
	}

	/**
	 * Sets the smoothing lambda to the value in the given interval that maximizes the
	 * classification accuracy on the given documents.
	 *
	 * @param minLambda lower bound of the search interval
	 * @param maxLambda upper bound of the search interval
	 * @param provider labeled documents used to evaluate candidate values
	 * @param steps number of optimization steps
	 * @param micro if <code>true</code> optimize micro-averaged accuracy, otherwise macro-averaged
	 */
	public void optimizeLambda(double minLambda, double maxLambda,
			final LabelingDocumentProvider<T, L> provider, int steps,
			final boolean micro) {
		PeakValueOptimizer peakValueOptimizer = new PeakValueOptimizer() {
			public double test(double value) {
				smoothingLambda = value;
				return AbstractTopicBasedBasicProbClassifier.this
						.test(provider, null)[micro ? 0 : 1];
			}
		};
		smoothingLambda = peakValueOptimizer.optimizeLambda(minLambda,
				maxLambda, steps);
	}

	/**
	 * Returns the label with the highest log posterior (log prior plus sum of topic frequency times log topic probability).
	 *
	 * @param d the document to classify
	 * @return the best label, or <code>null</code> if the classifier is untrained
	 */
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

	/**
	 * Computes the topic frequency vector of a test document.
	 *
	 * @param d the document
	 * @return a new vector of length {@link #getNTopics()}
	 */
	protected double[] computeTopicFrequencyTest(Document<T> d) {
		double[] ftd = new double[getNTopics()];
		computeTopicFrequency(d, ftd);

		return ftd;
	}

	/**
	 * Computes the smoothed log probability of a topic given a label.
	 *
	 * @param l index of the label
	 * @param topic position of the topic (vector dimension)
	 * @return log P(topic | label)
	 */
	protected double getLogPtc(int l, int topic) {
		TDoubleList pt = ptc.get(l);
		double total = pt.get(0);
		double sumt = pt.get(topic + 1);

		double[] ptopic = getPt();
		if (smoothingLambda > 0 && ptopic != null) {
			return Math.log((sumt / total) * (1 - smoothingLambda)
					+ smoothingLambda * ptopic[topic]);
		} else {
			return Math.log((sumt + 1) / (total + 1 * getNTopics()));
		}
	}

	/**
	 * Supplies the topic prior p(t) that the class-conditional topic probabilities are interpolated with.
	 *
	 * @return the prior topic probabilities used for smoothing, or <code>null</code> (default) if none are available
	 */
	protected double[] getPt() {
		return null;
	}
}
