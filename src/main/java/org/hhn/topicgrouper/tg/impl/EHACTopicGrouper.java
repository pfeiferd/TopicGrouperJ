package org.hhn.topicgrouper.tg.impl;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSolutionListener;

public class EHACTopicGrouper<T> extends AbstractTopicGrouper<T> {
	protected final MyPriorityQueue<JoinCandidate>[] allJcs;
	protected final JoinCandidate[] jcSafe;

	@SuppressWarnings("unchecked")
	public EHACTopicGrouper(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics) {
		super(minWordFrequency, documentProvider, minTopics);
		allJcs = new MyPriorityQueue[maxTopics];
		jcSafe = new JoinCandidate[maxTopics];
	}

	// Produces a huge memory footprint by creating so many jcs...
	protected void createInitialJoinCandidates(
			TGSolutionListener<T> solutionListener) {
		int initMax = maxTopics * (maxTopics - 1) / 2;
		int initCounter = 0;

		for (int i = 0; i < maxTopics; i++) {
			allJcs[i] = new MyPriorityQueue<JoinCandidate>(maxTopics - 1);
		}

		for (int i = 0; i < maxTopics; i++) {
			for (int j = i + 1; j < maxTopics; j++) {
				double newLogLikelihood = computeTwoWordLogLikelihood(i, j,
						topics[i].get(0), topics[j].get(0));

				double newImprovement = newLogLikelihood
						- topicLogLikelihoods[i] - topicLogLikelihoods[j];

				JoinCandidate a = new JoinCandidate(j, newLogLikelihood,
						newImprovement);
				allJcs[i].add(a);

				JoinCandidate b = new JoinCandidate(i, newLogLikelihood,
						newImprovement);
				allJcs[j].add(b);

				initCounter++;
				if (initCounter % 100000 == 0) {
					solutionListener.initalizing(((double) initCounter)
							/ initMax);
				}
			}
		}
	}

	// This grouping avoids the generation of new objects cause memory is scarce
	// and the gc would get too "stressed".
	protected void groupTopics(TGSolutionListener<T> solutionListener) {
		nTopics[0] = maxTopics;
		int[] jcia = new int[1];
		while (nTopics[0] > minTopics) {
			JoinCandidate jc = getBestJoinCandidate(jcia);
			int jci = jcia[0];
			int tSizeI = topicSizes[jci];
			int tSizeJ = topicSizes[jc.j];

			// Join the topics at position jc.i
			totalLogLikelihood += (jc.logLikelihood - topicLogLikelihoods[jci] - topicLogLikelihoods[jc.j]);

			topics[jci].addAll(topics[jc.j]);
			topicUnionFind.union(jc.j, jci);
			topicSizes[jci] += tSizeJ;
			sumWordFrTimesLogWordFrByTopic[jci] += sumWordFrTimesLogWordFrByTopic[jc.j];

			// Compute likelihood for joined topic
			topicLogLikelihoods[jci] = jc.logLikelihood;
			int[] a = topicFrequencyPerDocuments[jci];
			int[] b = topicFrequencyPerDocuments[jc.j];
			for (int i = 0; i < a.length; i++) {
				a[i] += b[i];
			}
			// Topic at position jc.j is gone
			topics[jc.j] = null;
			allJcs[jc.j] = null;
			topicLogLikelihoods[jc.j] = 0;
			topicSizes[jc.j] = 0;

			nTopics[0]--;

			solutionListener.updatedSolution(jci, jc.j, jc.improvement, tSizeI,
					tSizeJ, solution);

			// Enabl reuse of jc objects by saving them first.
			allJcs[jci].toArray(jcSafe);
			allJcs[jci].clear();
			int nextO = 0;

			for (int j = 0; j < maxTopics; j++) {
				if (j != jci && topics[j] != null) {
					double newLikelihood = computeTwoTopicLogLikelihood(jci, j);
					double newImprovement = newLikelihood
							- topicLogLikelihoods[jci] - topicLogLikelihoods[j];

					// Used an old but saved object.
					JoinCandidate jc2 = jcSafe[nextO++];
					jc2.init(j, newLikelihood, newImprovement);
					allJcs[jci].offer(jc2);

					Object[] otherQueue = allJcs[j].getQueue();
					int otherQueueSize = allJcs[j].size();
					for (int i = 0; i < otherQueueSize; i++) {
						JoinCandidate jc3 = (JoinCandidate) otherQueue[i];
						if (jc3.j == jc.j) {
							allJcs[j].removeAt(i);
							break;
						}
					}
					otherQueueSize--;
					for (int i = 0; i < otherQueueSize; i++) {
						JoinCandidate jc3 = (JoinCandidate) otherQueue[i];
						if (jc3.j == jci) {
							allJcs[j].removeAt(i);
							// Reuse the object that just got removed.
							jc3.init(jci, newLikelihood, newImprovement);
							allJcs[j].offer(jc3);
							break;
						}
					}
				}
			}
		}
	}

	protected JoinCandidate getBestJoinCandidate(int[] ia) {
		JoinCandidate best = null;
		for (int i = 0; i < topics.length; i++) {
			if (allJcs[i] != null) {
				JoinCandidate jc = allJcs[i].peek();
				if (best == null || best.compareTo(jc, ia[0], i) > 0) {
					best = jc;
					ia[0] = i;
				}
			}
		}
		allJcs[ia[0]].poll();
		return best;
	}
}
