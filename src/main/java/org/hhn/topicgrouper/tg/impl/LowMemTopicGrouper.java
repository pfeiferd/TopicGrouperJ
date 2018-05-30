package org.hhn.topicgrouper.tg.impl;

import gnu.trove.TIntCollection;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.PriorityQueue;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;

public class LowMemTopicGrouper<T> extends AbstractTopicGrouper<T> {
	protected final PriorityQueue<MyJoinCandidate> allJcs;
	private final Collection<MyJoinCandidate> addLaterCache;

	protected final HomonymHandler homonymHandler;
	private int jcUpdates;
	private int mainLoopCount;

	private boolean deferJCUpdates;

	public LowMemTopicGrouper(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics) {
		this(minWordFrequency, documentProvider, minTopics, 0);
	}

	public LowMemTopicGrouper(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics, double hEpsilon) {
		super(minWordFrequency, documentProvider, minTopics);
		allJcs = new PriorityQueue<MyJoinCandidate>(maxTopics);
		addLaterCache = new ArrayList<MyJoinCandidate>();

		this.deferJCUpdates = true;

		if (hEpsilon > 0) {
			homonymHandler = createHomonymHandler(hEpsilon);
		} else {
			homonymHandler = null;
		}
	}

	public void setDeferJCUpdates(boolean deferJCUpdates) {
		this.deferJCUpdates = deferJCUpdates;
	}

	public boolean isDeferJCUpdates() {
		return deferJCUpdates;
	}

	protected TGSolution<T> createSolution() {
		return new DefaultTGSolution() {
			@Override
			public TIntCollection getHomonymns() {
				return getHomonyms();
			}						
		};
	}

	protected void createInitialJoinCandidates(
			TGSolutionListener<T> solutionListener) {
		int initMax = maxTopics * (maxTopics - 1) / 2;
		int initCounter = 0;

		MyJoinCandidate[] joinCandidates = new MyJoinCandidate[maxTopics];

		for (int i = 0; i < maxTopics; i++) {
			for (int j = i + 1; j < maxTopics; j++) {
				double newLikelihood = computeTwoWordLogLikelihood(i, j,
						topics[i].get(0), topics[j].get(0));

				double newImprovement = newLikelihood - topicLogLikelihoods[i]
						- topicLogLikelihoods[j];

				MyJoinCandidate jc = joinCandidates[i];
				if (jc == null) {
					jc = joinCandidates[i] = new MyJoinCandidate();
				}
				if (newImprovement > jc.improvement) {
					jc.init(i, j, newLikelihood, newImprovement);
				}

				jc = joinCandidates[j];
				if (jc == null) {
					jc = joinCandidates[j] = new MyJoinCandidate();
				}
				if (newImprovement > jc.improvement) {
					jc.init(j, i, newLikelihood, newImprovement);
				}

				initCounter++;
				if (initCounter % 100000 == 0) {
					solutionListener.initalizing(((double) initCounter)
							/ initMax);
				}
			}
		}
		allJcs.addAll(Arrays.asList(joinCandidates));
	}

	protected void groupTopics(TGSolutionListener<T> solutionListener) {
		nTopics[0] = maxTopics;
		jcUpdates = 0;
		for (mainLoopCount = 0; nTopics[0] > minTopics; mainLoopCount++) {
			// Get the best join candidate
			MyJoinCandidate jc = getBestJoinCandidate();
			// Check if jc is invalid
			if (jc.j == -1) {
				// Recompute the best join candidate for jc and sort it in in
				// the right place.
				updateJoinCandidateForTopic(jc);
				allJcs.add(jc);
				jcUpdates++;
			} else {
				int t1Size = 0, t2Size = 0;
				if (!handleHomonymicTopic(jc)) {
					t1Size = topicSizes[jc.i];
					t2Size = topicSizes[jc.j];
					// Join the topics at position jc.i
					topics[jc.i].addAll(topics[jc.j]);
					topicUnionFind.union(jc.j, jc.i);
					topicSizes[jc.i] += t2Size;
					sumWordFrTimesLogWordFrByTopic[jc.i] += sumWordFrTimesLogWordFrByTopic[jc.j];
					// Compute likelihood for joined topic
					totalLogLikelihood -= topicLogLikelihoods[jc.i];
					topicLogLikelihoods[jc.i] = jc.logLikelihood;
					totalLogLikelihood += topicLogLikelihoods[jc.i];
					int[] a = topicFrequencyPerDocuments[jc.i];
					int[] b = topicFrequencyPerDocuments[jc.j];
					for (int i = 0; i < a.length; i++) {
						a[i] += b[i];
					}
					// Topic at position jc.j is gone
					topics[jc.j] = null;
					totalLogLikelihood -= topicLogLikelihoods[jc.j];
					topicLogLikelihoods[jc.j] = 0;
					topicSizes[jc.j] = 0;

					nTopics[0]--;

					solutionListener.updatedSolution(jc.i, jc.j,
							jc.improvement, t1Size, t2Size, solution);
				}
				updateJoinCandidates(jc);
			}
		}
	}

	protected void updateJoinCandidates(MyJoinCandidate jc) {
		// Save old j-index of jc, cause the join candidate with jc.i == j must
		// be deleted still.
		// jc.i does not need to be saved cause it does not change under the
		// following method call.
		int j = jc.j;
		// Recompute the best join partner for joined topic
		updateJoinCandidateForTopic(jc);
		// Add the new best join partner for topic[jc.i]
		allJcs.add(jc);

		addLaterCache.clear();
		Iterator<MyJoinCandidate> it = allJcs.iterator();
		while (it.hasNext()) {
			MyJoinCandidate jc2 = it.next();
			if (jc2.i == j) {
				it.remove();
			} else if (jc2 != jc
			// The following commented out optimization would require to show
			// that
			//
			// delta_h(s, t) < x and delta_h(s, w) < x ==> delta_h(s, t \cup
			// {w}) <
			// x
			//
			// Judging by the algorithm, the criterion is not violated. But
			// proving it seems hard.
			/* && (jc2.j == jc.i || jc2.j == j || jc2.j == -1) */) {
				double newLikelihood = computeTwoTopicLogLikelihood(jc.i, jc2.i);
				double newImprovement = newLikelihood
						- topicLogLikelihoods[jc2.i]
						- topicLogLikelihoods[jc.i];
				if (newImprovement > jc2.improvement
						|| (newImprovement == jc2.improvement && jc.i < jc2.j)) {
					it.remove();
					jc2.init(jc.i, newLikelihood, newImprovement);

					// Show me where the criterion from above is violated:
					// if (jc2.j != jc.i && jc2.j != j) {
					// System.out.println("Stop!");
					// }
					addLaterCache.add(jc2);
				} else if (jc2.j == jc.i || jc2.j == j) {
					if (!deferJCUpdates) {
						it.remove();
						updateJoinCandidateForTopic(jc2);
						addLaterCache.add(jc2);
						jcUpdates++;
					} else {
						jc2.j = -1;
					}
				}
			}
		}
		allJcs.addAll(addLaterCache);
	}

	protected MyJoinCandidate getBestJoinCandidate() {
		return allJcs.poll();
	}

	public int getJCUpdates() {
		return jcUpdates;
	}

	public int getMainLoopCount() {
		return mainLoopCount;
	}

	protected void updateJoinCandidateForTopic(MyJoinCandidate jc) {
		double bestImprovement = Double.NEGATIVE_INFINITY;
		double bestLikelihood = 0;
		int bestJ = -1;
		for (int j = 0; j < maxTopics; j++) {
			if (j != jc.i && topics[j] != null) {
				double newLikelihood = computeTwoTopicLogLikelihood(jc.i, j);
				double newImprovement = newLikelihood
						- topicLogLikelihoods[jc.i] - topicLogLikelihoods[j];
				if (newImprovement > bestImprovement
						|| (newImprovement == bestImprovement && j < bestJ)) {
					bestImprovement = newImprovement;
					bestLikelihood = newLikelihood;
					bestJ = j;
				}
			}
		}
		jc.init(bestJ, bestLikelihood, bestImprovement);
	}

	//
	// Homonym handling (Experimental)
	//

	protected HomonymHandler createHomonymHandler(double hEpsilon) {
		return new HomonymHandler(hEpsilon);
	}

	protected TIntCollection getHomonyms() {
		return homonymHandler == null ? null : homonymHandler.getHomonyms();
	}

	protected boolean handleHomonymicTopic(MyJoinCandidate jc) {
		return homonymHandler != null ? homonymHandler.handleHomonymicTopic(jc)
				: false;
	}

	protected static class MyJoinCandidate extends JoinCandidate {
		public int i;
		
		public MyJoinCandidate() {
		}

		public MyJoinCandidate(int i, int j, double logLikelihood,
				double improvement) {
			init(i, j, logLikelihood, improvement);
		}
		
		public void init(int i, int j, double logLikelihood,
				double improvement) {
			super.init(j, logLikelihood, improvement);
			this.i = i;
		}

		@Override
		public int compareTo(JoinCandidate o) {
			return compareTo(o, i, ((MyJoinCandidate)o).i);
		}

		@Override
		public String toString() {
			return "[l:" + logLikelihood + " imp:" + improvement + " i:"
					+ i + " j:" + j + "]";
		}
	}
	
	protected class HomonymHandler {
		private final double hEpsilon;
		private final TIntList homonymList;

		public HomonymHandler(double hEpsilon) {
			this.hEpsilon = hEpsilon;
			homonymList = new TIntArrayList();
		}

		protected boolean handleHomonymicTopic(MyJoinCandidate jc) {
			if (topics[jc.i].size() == 2 && topics[jc.j].size() <= 2) {
				if (handleHomonymCase(jc.i, jc.j)) {
					return true;
				}
			}
			if (topics[jc.j].size() == 2 && topics[jc.i].size() <= 2) {
				return handleHomonymCase(jc.j, jc.i);
			}
			return false;
		}

		// This could be unfolded
		protected boolean handleHomonymCase(int tid1, int tid2) {
			int size2 = topics[tid2].size();
			int word0 = topics[tid1].get(0);
			int word1 = topics[tid1].get(1);
			double a = 0;
			double b = 0;
			for (int i = 0; i < size2; i++) {
				a += computeTwoWordLogLikelihoodAlt(word0, topics[tid2].get(i));
				b += computeTwoWordLogLikelihoodAlt(word1, topics[tid2].get(i));
			}
			a /= size2;
			b /= size2;

			double relDiff = Math.abs(a - b) / (Math.abs(a) + Math.abs(b));
			if (relDiff > hEpsilon) {
				// System.out.print("Incoherent: ");
				// printTopic(topics[tid1]);
				// printTopic(topics[tid2]);
				// System.out.println();
				if (a < b) {
					toOneWordTopic(tid1, 1); // tid1 1 is homonym
				} else {
					toOneWordTopic(tid1, 0); // tid2 0 is homonym
				}
				return true;
			}
			return false;
		}

		// protected void printTopic(TIntList list) {
		// System.out.print("{");
		// for (int i = 0; i < list.size(); i++) {
		// System.out.print(documentProvider.getWord(list.get(i)));
		// System.out.print(" ");
		// }
		// System.out.print("} ");
		// }

		protected void toOneWordTopic(int tid, int pos) {
			int wordIndex = topics[tid].get(pos);
			// System.out.println("Homonym: " +
			// documentProvider.getVocab().getWord(wordIndex));
			//
			topics[tid].removeAt(pos);

			int otherWord = topics[tid].get(0);

			homonymList.add(wordIndex);
			int fr = documentProvider.getWordFrequency(wordIndex);
			topicSizes[tid] -= fr;
			double v = computeOneWordTopicLogLikelihood(otherWord);
			totalLogLikelihood += v - topicLogLikelihoods[tid];
			topicLogLikelihoods[tid] = v;

			for (int j = 0; j < documents.size(); j++) {
				topicFrequencyPerDocuments[tid][j] -= documents.get(j)
						.getWordFrequency(wordIndex);
			}

			sumWordFrTimesLogWordFrByTopic[tid] -= fr * Math.log(fr);
		}

		protected TIntCollection getHomonyms() {
			return homonymList;
		}

		protected double computeTwoWordLogLikelihoodAlt(int word1, int word2) {
			double sum = computeTwoWordLogLikelihoodHelp(word1, word2);

			int fr1 = documentProvider.getWordFrequency(word1);
			int fr2 = documentProvider.getWordFrequency(word2);

			sum += fr1 * Math.log(fr1);
			sum += fr2 * Math.log(fr2);
			int sizeSum = fr1 + fr2;
			sum -= (sizeSum) * Math.log(sizeSum);

			return sum;
		}

		// protected double computeTwoTopicCoherence(int topic1, int topic2) {
		// TIntList c2 = topics[topic2];
		// TIntList c1 = topics[topic1];
		// double sum = 0;
		// for (int i = 0; i < documents.size(); i++) {
		// int min = Integer.MAX_VALUE;
		// int max = 0;
		// Document<T> d = documents.get(i);
		// for (int j = 0; j < c1.size(); j++) {
		// int fr = d.getWordFrequency(c1.get(j));
		// if (fr < min) {
		// min = fr;
		// }
		// if (fr > max) {
		// max = fr;
		// }
		// }
		// for (int j = 0; j < c2.size(); j++) {
		// int fr = d.getWordFrequency(c2.get(j));
		// if (fr < min) {
		// min = fr;
		// }
		// if (fr > max) {
		// max = fr;
		// }
		// }
		// sum += Math.log(min + 1) - Math.log(max + 1);
		// }
		// return sum;
		// }
	}
}
