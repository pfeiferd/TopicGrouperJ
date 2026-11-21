package org.hhn.topicgrouper.classify.impl.tg;

import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.tg.TGSolution;

/**
 * Naive Bayes classifier whose topics are the topics (word groups) of a TopicGrouper solution.
 * The relative topic frequencies of the solution serve as prior for smoothing.
 *
 * @param <T> the word type of the documents
 * @param <L> the label type
 */
public class TGNBClassifier<T, L> extends
		AbstractTopicBasedNBClassifier<T, L> {
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
	 * @param lambda smoothing weight, see {@link AbstractTopicBasedNBClassifier}
	 * @param solution the topic grouper solution
	 */
	public TGNBClassifier(double lambda, TGSolution<T> solution) {
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
