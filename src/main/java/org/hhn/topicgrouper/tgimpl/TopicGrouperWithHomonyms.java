package org.hhn.topicgrouper.tgimpl;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import org.hhn.topicgrouper.base.DocumentProvider;

/*
 * This is purely experimental (and doesn't even work well)
 */
public class TopicGrouperWithHomonyms<T> extends OptimizedTopicGrouper<T> {
	private final double hEpsilon;
	private final double alpha;
	private final TIntList homonymList;

	public TopicGrouperWithHomonyms(int minWordFrequency, double lambda,
			boolean checkHomonyms, double hEpsilon, double alpha,
			DocumentProvider<T> documentProvider, int minTopics) {
		super(minWordFrequency, lambda,  documentProvider, minTopics);
		this.hEpsilon = hEpsilon;
		this.alpha = alpha;
		homonymList = new TIntArrayList();		
	}
	
	protected int getHomonymicTopic(JoinCandidate jc) {
		if (topics[jc.i].size() == 1) {
			for (JoinCandidate jc2 : joinCandidates) {
				if (jc2.j == jc.i && topics[jc2.i] != null) {
					if (jc.i >= 400) {
						System.out.println("stop");
					}
					if (checkHomonymicImprovement(jc, jc2, jc.j, jc2.i)) {
						if (alpha >= 0 || checkLikelihoodDelta(jc2.i, jc.j)) {
							return jc.i;
						}
					}
				}
			}
		}
		if (topics[jc.j].size() == 1) {
			for (JoinCandidate jc2 : joinCandidates) {
				if (jc2.i == jc.j && topics[jc2.j] != null) {
					if (jc.j >= 400) {
						System.out.println("stop");
					}
					if (checkHomonymicImprovement(jc, jc2, jc.i, jc2.j)) {
						if (alpha >= 0 || checkLikelihoodDelta(jc2.j, jc.i)) {
							return jc.j;
						}
					}
				}
				if (jc2.j == jc.j && topics[jc2.i] != null) {
					if (jc.j >= 400) {
						System.out.println("stop");
					}
					if (checkHomonymicImprovement(jc, jc2, jc.i, jc2.i)) {
						if (alpha >= 0 || checkLikelihoodDelta(jc2.i, jc.i)) {
							return jc.j;
						}
					}
				}
			}
		}
		return -1;
	}

	protected void handleHomonym(int tid, JoinCandidate jc) {
		int homonym = topics[tid].get(0); // Actually homonym = tid
		// would be just as
		// good.
		homonymList.add(homonym);
		if (tid == jc.i) {
			topics[jc.i] = topics[jc.j];
			topicSizes[jc.i] = topicSizes[jc.j];
			sumWordFrTimesLogWordFrByTopic[jc.i] = sumWordFrTimesLogWordFrByTopic[jc.j];
			topicFrequencyPerDocuments[jc.i] = topicFrequencyPerDocuments[jc.j];
			totalLikelihood -= topicLikelihoods[jc.i];
			topicLikelihoods[jc.i] = topicLikelihoods[jc.j];
			totalLikelihood += topicLikelihoods[jc.i];
		} else {
			// Do nothing.
		}
		/*
		 * We do NOT do documentSize correction for optimization reasons: In
		 * theory things below would have to be done, but also ALL jc's would
		 * have to be recomputed cause of the changed doc sizes and dependent
		 * computations. This would be way too enefficient. Note that
		 * (typically) document sizes change only marginally when removing the
		 * homonymic word. The hope is that approach is robust enough to
		 * compensate for it.
		 * 
		 * for (int i = 0; i < documentSizes.length; i++) { int fr =
		 * documents[i].getWordFrequency(homonym); if (fr > 0) {
		 * documentSizes[i] -= fr; if (documentSizes[i] > 0) {
		 * logDocumentSizes[i] = Math .log(documentSizes[i]); }
		 * onePlusLambdaDivDocSizes[i] = 1 + lambda / documentSizes[i]; } }
		 */
	}
	
	private boolean checkHomonymicImprovement(JoinCandidate j1,
			JoinCandidate j2, int ta, int tb) {
		if (Math.abs(j1.improvement / Math.log(topicSizes[ta]) - j2.improvement
				/ Math.log(topicSizes[tb])) < hEpsilon) {
			return true;
		}
		// if (Math.log(Math.exp(j1.improvement) / topicSizes[ta] -
		// Math.exp(j2.improvement) / topicSizes[tb]) < hEpsilon) {
		// return true;
		// }
		return false;
	}

	private boolean checkLikelihoodDelta(int i, int j) {
		double newLikelihood = computeTwoTopicLogLikelihood(i, j);
		double newImprovement = newLikelihood - topicLikelihoods[i]
				- topicLikelihoods[j];
		return newImprovement
				/ (Math.log(topicSizes[i]) * Math.log(topicSizes[j])) < alpha;
	}
	
}
