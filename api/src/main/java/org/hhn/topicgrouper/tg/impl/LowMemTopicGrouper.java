package org.hhn.topicgrouper.tg.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.util.ParallelRunner;

/**
 * Topic Grouper that only stores the best join partner per topic (memory linear in
 * the vocabulary size). Outdated candidates are recomputed lazily. The
 * recomputations of the candidates after a join (and the initial candidates)
 * are distributed over the threads of the solver; the result does not depend
 * on the number of threads.
 *
 * @param <T> the type of the words
 */
public class LowMemTopicGrouper<T> extends AbstractTopicGrouper<T> {
	/** One candidate per current topic (its best join partner), best first. */
	protected final PriorityQueue<MyJoinCandidate> allJcs;
	private final Collection<MyJoinCandidate> addLaterCache;

	private int jcUpdates;
	private int mainLoopCount;

	private boolean deferJCUpdates;

	/**
	 * Creates a single threaded solver with deferred candidate updates.
	 *
	 * @param minWordFrequency only words occurring at least this often (&gt;= 1) in the documents are grouped
	 * @param documentProvider the training documents
	 * @param minTopics the algorithm stops when this number of topics is reached
	 */
	public LowMemTopicGrouper(int minWordFrequency, DocumentProvider<T> documentProvider, int minTopics) {
		this(minWordFrequency, documentProvider, minTopics, 1);
	}

	/**
	 * Creates the solver with deferred candidate updates.
	 *
	 * @param minWordFrequency only words occurring at least this often (&gt;= 1) in the documents are grouped
	 * @param documentProvider the training documents
	 * @param minTopics the algorithm stops when this number of topics is reached
	 * @param threads the number of threads to use, including the calling one
	 */
	public LowMemTopicGrouper(int minWordFrequency, DocumentProvider<T> documentProvider, int minTopics,
			int threads) {
		super(minWordFrequency, documentProvider, minTopics, threads);
		allJcs = new PriorityQueue<MyJoinCandidate>(Math.max(1, maxTopics));
		addLaterCache = new ArrayList<MyJoinCandidate>();

		this.deferJCUpdates = true;
	}

	/**
	 * Whether outdated join candidates are recomputed only when they reach the top of the queue (default: true).
	 *
	 * @param deferJCUpdates true to defer, false to recompute immediately
	 */
	public void setDeferJCUpdates(boolean deferJCUpdates) {
		this.deferJCUpdates = deferJCUpdates;
	}

	/**
	 * See {@link #setDeferJCUpdates(boolean)}.
	 *
	 * @return whether outdated join candidates are recomputed lazily
	 */
	public boolean isDeferJCUpdates() {
		return deferJCUpdates;
	}

	protected TGSolution<T> createSolution() {
		return new DefaultTGSolution();
	}

	protected void createInitialJoinCandidates(final TGSolutionListener<T> solutionListener) {
		final int n = maxTopics;
		final MyJoinCandidate[] joinCandidates = new MyJoinCandidate[n];
		final List<MyJoinCandidate[]> partials = Collections.synchronizedList(new ArrayList<MyJoinCandidate[]>());
		// Rows in batches so that the progress can be reported; every chunk of
		// rows collects the best partner it saw for every topic, the chunks are
		// merged afterwards (ties go to the lower partner index, like in a
		// sequential scan).
		int batch = Math.max(threads * 8, n / 50);
		for (int start = 0; start < n; start += batch) {
			final int end = Math.min(n, start + batch);
			runner.run(end - start, 2, new ParallelRunner.RangeTask() {
				public void run(int from, int to) {
					MyJoinCandidate[] best = new MyJoinCandidate[n];
					for (int k = from; k < to; k++) {
						int i = (end - 1) - k;
						computeInitialRow(i, best);
					}
					partials.add(best);
				}
			});
			for (MyJoinCandidate[] best : partials) {
				for (int i = 0; i < n; i++) {
					if (best[i] != null) {
						MyJoinCandidate jc = joinCandidates[i];
						if (jc == null) {
							jc = joinCandidates[i] = new MyJoinCandidate();
						}
						if (best[i].improvement > jc.improvement
								|| (best[i].improvement == jc.improvement && best[i].j < jc.j)) {
							jc.init(i, best[i].j, best[i].logLikelihood, best[i].improvement);
						}
					}
				}
			}
			partials.clear();
			double done = (double) end * (2.0 * n - end - 1) / 2;
			solutionListener.initalizing(done / ((double) n * (n - 1) / 2));
		}
		// Entries stay null if there are less than two topics.
		for (MyJoinCandidate jc : joinCandidates) {
			if (jc != null) {
				allJcs.add(jc);
			}
		}
	}

	/**
	 * Computes the candidates of the initial topic <code>i</code> with all
	 * topics <code>j &gt; i</code> and keeps, for <code>i</code> and every
	 * <code>j</code>, the best one seen so far in <code>best</code> (ties go to
	 * the lower partner index).
	 *
	 * @param i the row
	 * @param best per topic its best candidate so far, entries may be null
	 */
	protected void computeInitialRow(int i, MyJoinCandidate[] best) {
		int word1 = topics[i].get(0);
		for (int j = i + 1; j < maxTopics; j++) {
			double newLikelihood = computeTwoWordLogLikelihood(i, j, word1, topics[j].get(0));
			double newImprovement = newLikelihood - topicLogLikelihoods[i] - topicLogLikelihoods[j];
			keepBest(best, i, j, newLikelihood, newImprovement);
			keepBest(best, j, i, newLikelihood, newImprovement);
		}
	}

	private void keepBest(MyJoinCandidate[] best, int i, int j, double logLikelihood, double improvement) {
		MyJoinCandidate jc = best[i];
		if (jc == null) {
			jc = best[i] = new MyJoinCandidate();
		}
		if (improvement > jc.improvement || (improvement == jc.improvement && j < jc.j)) {
			jc.init(i, j, logLikelihood, improvement);
		}
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
				addTopicCounts(jc.i, jc.j);
				// Topic at position jc.j is gone
				topics[jc.j] = null;
				totalLogLikelihood -= topicLogLikelihoods[jc.j];
				topicLogLikelihoods[jc.j] = 0;
				topicSizes[jc.j] = 0;

				nTopics[0]--;

				solutionListener.updatedSolution(jc.i, jc.j, jc.improvement, t1Size, t2Size, solution);
				updateJoinCandidates(jc);
			}
		}
	}

	/**
	 * Updates the queue after the join described by the candidate: recomputes the
	 * candidate of the joined topic, removes the candidate of the vanished topic,
	 * and for every other topic either adopts the joined topic as new best partner
	 * if that is better, or marks (or recomputes, see
	 * {@link #setDeferJCUpdates(boolean)}) the candidate if its partner was one of
	 * the joined topics.
	 *
	 * @param jc the candidate that was just joined (<code>i</code> is the surviving topic)
	 */
	protected void updateJoinCandidates(final MyJoinCandidate jc) {
		// Save old j-index of jc, cause the join candidate with jc.i == j must
		// be deleted still.
		// jc.i does not need to be saved cause it does not change under the
		// following method call.
		final int j = jc.j;
		// Recompute the best join partner for joined topic
		updateJoinCandidateForTopic(jc);

		final MyJoinCandidate[] all = allJcs.toArray(new MyJoinCandidate[allJcs.size()]);
		final double[] likelihoods = new double[all.length];
		final double[] improvements = new double[all.length];
		runner.run(all.length, new ParallelRunner.RangeTask() {
			public void run(int from, int to) {
				for (int k = from; k < to; k++) {
					MyJoinCandidate jc2 = all[k];
					if (jc2.i != j) {
						likelihoods[k] = computeTwoTopicLogLikelihood(jc.i, jc2.i);
						improvements[k] = likelihoods[k] - topicLogLikelihoods[jc2.i] - topicLogLikelihoods[jc.i];
					}
				}
			}
		});
		allJcs.clear();
		// Add the new best join partner for topic[jc.i]
		allJcs.add(jc);
		addLaterCache.clear();
		for (int k = 0; k < all.length; k++) {
			MyJoinCandidate jc2 = all[k];
			if (jc2.i == j) {
				continue;
			}
			// The following commented out optimization would require to show
			// that
			//
			// delta_h(s, t) < x and delta_h(s, w) < x ==> delta_h(s, t \cup
			// {w}) <
			// x
			//
			// Judging by the algorithm, the criterion is not violated. But
			// proving it seems hard.
			/* && (jc2.j == jc.i || jc2.j == j || jc2.j == -1) */
			if (improvements[k] > jc2.improvement || (improvements[k] == jc2.improvement && jc.i < jc2.j)) {
				jc2.init(jc.i, likelihoods[k], improvements[k]);
			} else if (jc2.j == jc.i || jc2.j == j) {
				if (!deferJCUpdates) {
					updateJoinCandidateForTopic(jc2);
					jcUpdates++;
				} else {
					jc2.j = -1;
				}
			}
			addLaterCache.add(jc2);
		}
		allJcs.addAll(addLaterCache);
	}

	/**
	 * Removes and returns the head of the queue (which may be outdated, see {@link MyJoinCandidate}).
	 *
	 * @return the best join candidate
	 */
	protected MyJoinCandidate getBestJoinCandidate() {
		return allJcs.poll();
	}

	/**
	 * The number of join candidate recomputations during the last run.
	 *
	 * @return the number of recomputations
	 */
	public int getJCUpdates() {
		return jcUpdates;
	}

	/**
	 * The number of iterations of the main loop during the last run (joins plus
	 * recomputations of outdated candidates).
	 *
	 * @return the number of iterations
	 */
	public int getMainLoopCount() {
		return mainLoopCount;
	}

	/**
	 * Recomputes the best join partner of the candidate's topic <code>i</code>
	 * over all current topics (ties go to the lower index).
	 *
	 * @param jc the candidate to update
	 */
	protected void updateJoinCandidateForTopic(final MyJoinCandidate jc) {
		final List<MyJoinCandidate> partials = Collections.synchronizedList(new ArrayList<MyJoinCandidate>());
		runner.run(maxTopics, new ParallelRunner.RangeTask() {
			public void run(int from, int to) {
				MyJoinCandidate best = new MyJoinCandidate();
				best.j = -1;
				for (int j = from; j < to; j++) {
					if (j != jc.i && topics[j] != null) {
						double newLikelihood = computeTwoTopicLogLikelihood(jc.i, j);
						double newImprovement = newLikelihood - topicLogLikelihoods[jc.i] - topicLogLikelihoods[j];
						if (newImprovement > best.improvement || (newImprovement == best.improvement && j < best.j)) {
							best.init(jc.i, j, newLikelihood, newImprovement);
						}
					}
				}
				if (best.j != -1) {
					partials.add(best);
				}
			}
		});
		double bestImprovement = Double.NEGATIVE_INFINITY;
		double bestLikelihood = 0;
		int bestJ = -1;
		for (MyJoinCandidate best : partials) {
			if (best.improvement > bestImprovement || (best.improvement == bestImprovement && best.j < bestJ)) {
				bestImprovement = best.improvement;
				bestLikelihood = best.logLikelihood;
				bestJ = best.j;
			}
		}
		jc.init(bestJ, bestLikelihood, bestImprovement);
	}

	/**
	 * A join candidate that also knows its own topic <code>i</code>; <code>j == -1</code> marks an outdated candidate.
	 */
	protected static class MyJoinCandidate extends JoinCandidate {
		/** The index of the topic the candidate belongs to. */
		public int i;

		/** Creates a candidate with improvement negative infinity. */
		public MyJoinCandidate() {
		}

		/**
		 * Creates an initialized candidate.
		 *
		 * @param i the index of the topic the candidate belongs to
		 * @param j the index of the partner topic
		 * @param logLikelihood the log likelihood contribution of the joined topic
		 * @param improvement the change of the total log likelihood caused by the join
		 */
		public MyJoinCandidate(int i, int j, double logLikelihood, double improvement) {
			init(i, j, logLikelihood, improvement);
		}

		/**
		 * Sets all fields.
		 *
		 * @param i the index of the topic the candidate belongs to
		 * @param j the index of the partner topic
		 * @param logLikelihood the log likelihood contribution of the joined topic
		 * @param improvement the change of the total log likelihood caused by the join
		 */
		public void init(int i, int j, double logLikelihood, double improvement) {
			super.init(j, logLikelihood, improvement);
			this.i = i;
		}

		@Override
		public int compareTo(JoinCandidate o) {
			return compareTo(o, i, ((MyJoinCandidate) o).i);
		}

		@Override
		public String toString() {
			return "[l:" + logLikelihood + " imp:" + improvement + " i:" + i + " j:" + j + "]";
		}
	}
}
