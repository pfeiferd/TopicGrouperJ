package org.hhn.topicgrouper.eval.classify.impl.tg;

import org.hhn.topicgrouper.eval.classify.impl.AbstractTopicBasedBasicProbClassifier;
import org.hhn.topicgrouper.tg.TGSolution;

/**
 * Probabilistic classifier whose topics are the topics (word groups) of a TopicGrouper solution.
 * The relative topic frequencies of the solution serve as prior for smoothing.
 *
 * @param <T> the word type of the documents
 * @param <L> the label type
 */
public class TGBasicProbClassifier<T, L> extends
		AbstractTopicBasedBasicProbClassifier<T, L> {
	/**
	 * The topic grouper solution defining the topics.
	 */
	protected final TGSolution<T> solution;
	/**
	 * The topic ids of the solution.
	 */
	protected final int[] topicIds;
	/**
	 * Relative frequency of each topic in the solution, in the order of {@link #topicIds}.
	 */
	protected final double[] pt;

	/**
	 * Creates the classifier.
	 *
	 * @param lambda smoothing weight, see {@link AbstractTopicBasedBasicProbClassifier}
	 * @param solution the topic grouper solution
	 */
	public TGBasicProbClassifier(double lambda, TGSolution<T> solution) {
		super(lambda);
		this.solution = solution;
		topicIds = solution.getTopicIds();
		pt = new double[topicIds.length];
		for (int i = 0; i < pt.length; i++) {
			pt[i] = ((double) solution.getTopicFrequency(topicIds[i]))
					/ solution.getSize();
		}
	}

	/**
	 * @return the topic ids of the solution
	 */
	@Override
	protected int[] getTopicIndices() {
		return topicIds;
	}

	/**
	 * @param wordIndex the word index
	 * @return the topic of the word according to the solution
	 */
	@Override
	protected int getTopicIndex(int wordIndex) {
		return solution.getTopicForWord(wordIndex);
	}

	/**
	 * @return the relative topic frequencies of the solution
	 */
	@Override
	protected double[] getPt() {
		return pt;
	}
}
