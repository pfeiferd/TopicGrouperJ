package org.hhn.topicgrouper.tgimpl.exp;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Collections;
import java.util.List;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

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
		super(minWordFrequency, lambda, documentProvider, minTopics);
		this.hEpsilon = hEpsilon;
		this.alpha = alpha;
		homonymList = new TIntArrayList();
	}

	protected int getHomonymicTopic(JoinCandidate jc) {
		if (topics[jc.i].size() == 2 && topics[jc.j].size() == 1) {
			double a = computeTwoWordLogLikelihoodAlt(topics[jc.i].get(0), topics[jc.j].get(0));
			double b = computeTwoWordLogLikelihoodAlt(topics[jc.i].get(1), topics[jc.j].get(0));

			if (Math.abs(a - b) > hEpsilon) {
				System.out.println("Incoherent 1: " + topics[jc.i] + " " + topics[jc.j] + " Level: " + Math.abs(a - b));
				return jc.j;
			}
			else {
				System.out.println("ok.");
			}
		}
		else if (topics[jc.j].size() == 2 && topics[jc.i].size() == 1) {
			double a = computeTwoWordLogLikelihoodAlt(topics[jc.j].get(0), topics[jc.i].get(0));
			double b = computeTwoWordLogLikelihoodAlt(topics[jc.j].get(1), topics[jc.i].get(0));

			if (Math.abs(a - b) > hEpsilon) {
				System.out.println("Incoherent 2: " + topics[jc.i] + " " + topics[jc.j]);
				return jc.i;
			}
			else {
				System.out.println("ok.");
			}
		}
		// if (!documentProvider.getWord(topics[jc.i].get(0)).equals("south")) {
		// return -1;
		// }
		// for (JoinCandidate jc2 : joinCandidates) {
		// if (jc2.j == jc.i && topics[jc2.i] != null) {
		// if (checkHomonymicImprovement(jc, jc2, jc.j, jc2.i)) {
		// if (alpha >= 0 || checkLikelihoodDelta(jc2.i, jc.j)) {
		// return jc.i;
		// }
		// }
		// }
		// }
		// }
		// if (topics[jc.j].size() == 1) {
		// if (!documentProvider.getWord(topics[jc.j].get(0)).equals("south")) {
		// return -1;
		// }
		// for (JoinCandidate jc2 : joinCandidates) {
		// if (jc2.i == jc.j && topics[jc2.j] != null) {
		// if (jc.j >= 400) {
		// System.out.println("stop");
		// }
		// if (checkHomonymicImprovement(jc, jc2, jc.i, jc2.j)) {
		// if (alpha >= 0 || checkLikelihoodDelta(jc2.j, jc.i)) {
		// return jc.j;
		// }
		// }
		// }
		// if (jc2.j == jc.j && topics[jc2.i] != null) {
		// if (jc.j >= 400) {
		// System.out.println("stop");
		// }
		// if (checkHomonymicImprovement(jc, jc2, jc.i, jc2.i)) {
		// if (alpha >= 0 || checkLikelihoodDelta(jc2.i, jc.i)) {
		// return jc.j;
		// }
		// }
		// }
		// }
		// }
		return -1;
	}
	
	protected double computeTwoWordLogLikelihoodAlt(int word1,
			int word2) {
		double sum = 0;
		List<DocIndexAndWordFr> l1 = invertedIndex.get(word1);
		List<DocIndexAndWordFr> l2 = invertedIndex.get(word2);
		if (l1 != null && l2 != null) {
			for (DocIndexAndWordFr entry1 : l1) {
				searchDummy.docIndex = entry1.docIndex;
				int posEntry2 = Collections.binarySearch(l2, searchDummy);
				if (posEntry2 >= 0) {
					DocIndexAndWordFr entry2 = l2.get(posEntry2);
					if (documentSizes[entry1.docIndex] > 0) {
						int fr = entry1.wordFr + entry2.wordFr;
						sum += onePlusLambdaDivDocSizes[entry1.docIndex]
								* fr
								* (Math.log(fr) - logDocumentSizes[entry1.docIndex]);
					}

				} else {
					if (documentSizes[entry1.docIndex] > 0) {
						sum += onePlusLambdaDivDocSizes[entry1.docIndex]
								* entry1.wordFr
								* (Math.log(entry1.wordFr) - logDocumentSizes[entry1.docIndex]);
					}
				}
			}
			for (DocIndexAndWordFr entry2 : l2) {
				searchDummy.docIndex = entry2.docIndex;
				int posEntry1 = Collections.binarySearch(l1, searchDummy);
				if (posEntry1 < 0) {
					if (documentSizes[entry2.docIndex] > 0) {
						sum += onePlusLambdaDivDocSizes[entry2.docIndex]
								* entry2.wordFr
								* (Math.log(entry2.wordFr) - logDocumentSizes[entry2.docIndex]);
					}
				}
			}
		}
		
		int fr1 = documentProvider.getWordFrequency(word1);
		int fr2 = documentProvider.getWordFrequency(word2);

		sum += fr1 * Math.log(fr1);
		sum += fr2 * Math.log(fr2);
		int sizeSum = fr1 + fr2;
		sum -= (sizeSum) * Math.log(sizeSum);

		return sum;
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
		 * computations. This would be way too inefficient. Note that
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
		return Math.abs(j1.likelihood - j2.likelihood) < hEpsilon;
		// if (Math.log(Math.exp(j1.improvement) / topicSizes[ta] -
		// Math.exp(j2.improvement) / topicSizes[tb]) < hEpsilon) {
		// return true;
		// }
	}

	private boolean checkLikelihoodDelta(int i, int j) {
		double newLikelihood = computeTwoTopicLogLikelihood(i, j);
		double newImprovement = newLikelihood - topicLikelihoods[i]
				- topicLikelihoods[j];
		return newImprovement
				/ (Math.log(topicSizes[i]) * Math.log(topicSizes[j])) < alpha;
	}

}
