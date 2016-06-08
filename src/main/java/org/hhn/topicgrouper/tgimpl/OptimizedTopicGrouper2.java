package org.hhn.topicgrouper.tgimpl;

import org.hhn.topicgrouper.base.DocumentProvider;

public class OptimizedTopicGrouper2<T> extends OptimizedTopicGrouper<T> {
	protected JoinCandidate[] joinCandidates;

	public OptimizedTopicGrouper2(int minWordFrequency, double lambda,
			DocumentProvider<T> documentProvider, int minTopics) {
		super(minWordFrequency, lambda, documentProvider, minTopics);
		joinCandidates = new JoinCandidate[maxTopics];
	}

	protected void addToJoinCandiates(int i, JoinCandidate jc) {
		joinCandidates[i] = jc;
	}

	@Override
	protected JoinCandidate getBestJoinCandidate() {
		bubbleSort(joinCandidates, nTopics[0]);
		return joinCandidates[0];
	}

	@Override
	protected void updateJoinCandidates(JoinCandidate jc) {
		// Recompute the best join partner for joined topic
		if (!updateJoinCandidateForTopic(jc)) {
			joinCandidates[0] = null;
		}

		// Check for all jc2 with jc2.i < jc.i if jc is a better join
		// partner than the old one
		for (int i = 1; i < nTopics[0] - 1; i++) {
			JoinCandidate jc2 = joinCandidates[i];
			if (jc2 != null && jc2.i < jc.i) {
				if (topics[jc2.i] != null) {
					// This refers to the old topic of jc.i, so the
					// new join partner for jc2 must be computed.
					if (jc2.j == jc.i) {
						updateJoinCandidateForTopic(jc2);
					} else {
						double newLikelihood = computeTwoTopicLogLikelihood(
								jc2.i, jc.i);
						double newImprovement = newLikelihood
								- topicLikelihoods[jc2.i]
								- topicLikelihoods[jc.i];
						if (newImprovement > jc2.improvement) {
							jc2.improvement = newImprovement;
							jc2.likelihood = newLikelihood;
							jc2.j = jc.i;
						}
					}
				}
			}
		}
	}

	@Override
	protected void handleInconsistentJoinCandidate(
			org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper.JoinCandidate jc) {
		if (topics[jc.i] != null && topics[jc.j] == null) {
			if (!updateJoinCandidateForTopic(jc)) {
				joinCandidates[0] = null;
			}
		} else {
			joinCandidates[0] = null;
		}
	}

	private static void bubbleSort(JoinCandidate[] x, int max) {
		boolean unsorted = true;
		JoinCandidate temp;

		while (unsorted) {
			unsorted = false;
			for (int i = max - 1; i > 0; i--) {
				if (x[i] != null && (x[i - 1] == null || /*
														 * x[i].compareTo(x[i-
														 * 1]) > 0
														 */
				x[i].improvement > x[i - 1].improvement)) {
					temp = x[i];
					x[i] = x[i - 1];
					x[i - 1] = temp;
					unsorted = true;
				}
			}
		}
	}
}
