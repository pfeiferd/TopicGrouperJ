package org.hhn.topicgrouper.tg.impl;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.util.ParallelRunner;

/**
 * Topic Grouper with an efficient heap based agglomerative clustering: every
 * topic keeps a priority queue of all join candidates. This is fast, but needs memory
 * quadratic in the vocabulary size. See {@link LowMemTopicGrouper} for large vocabularies.
 * <p>
 * The queues are indexed heaps ({@link JoinQueue}): the candidate of a topic
 * for a given partner is found directly and removed in logarithmic time, so a
 * join costs <code>O(T log T)</code> queue work for <code>T</code> topics plus
 * the recomputation of the <code>T</code> candidates of the joined topic. The
 * recomputations (and the initial candidates of all pairs) are distributed
 * over the threads of the solver, each thread updating the queues of the
 * partners it handles; the result does not depend on the number of threads.
 *
 * @param <T> the type of the words
 */
public class EHACTopicGrouper<T> extends AbstractTopicGrouper<T> {
	/** Per topic: the join candidates with all other topics, best first; <code>null</code> for vanished topics. */
	protected final JoinQueue[] allJcs;
	/** Scratch: per partner topic, the recomputed candidate of the topic being refreshed. */
	protected final JoinCandidate[] rowSlots;

	/**
	 * Creates a single threaded solver.
	 *
	 * @param minWordFrequency only words occurring at least this often (&gt;= 1) in the documents are grouped
	 * @param documentProvider the training documents
	 * @param minTopics the algorithm stops when this number of topics is reached
	 */
	public EHACTopicGrouper(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics) {
		this(minWordFrequency, documentProvider, minTopics, 1);
	}

	/**
	 * Creates the solver.
	 *
	 * @param minWordFrequency only words occurring at least this often (&gt;= 1) in the documents are grouped
	 * @param documentProvider the training documents
	 * @param minTopics the algorithm stops when this number of topics is reached
	 * @param threads the number of threads to use, including the calling one
	 */
	@SuppressWarnings("unchecked")
	public EHACTopicGrouper(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics, int threads) {
		super(minWordFrequency, documentProvider, minTopics, threads);
		allJcs = new EHACTopicGrouper.JoinQueue[maxTopics];
		rowSlots = new JoinCandidate[maxTopics];
	}

	/**
	 * The candidates of one topic: a heap ordered by improvement that also
	 * knows the candidate for every partner topic and the heap position of
	 * every candidate.
	 */
	protected class JoinQueue extends MyPriorityQueue<JoinCandidate> {
		/** Per partner topic: its candidate, <code>null</code> if none. */
		protected final JoinCandidate[] byPartner;

		/**
		 * Builds the queue over the given candidates in linear time.
		 *
		 * @param candidates the candidates (the array becomes the heap array)
		 * @param size the number of candidates
		 */
		public JoinQueue(JoinCandidate[] candidates, int size) {
			super(candidates, size);
			byPartner = new JoinCandidate[maxTopics];
			for (int i = 0; i < size; i++) {
				byPartner[candidates[i].j] = candidates[i];
			}
		}

		@Override
		protected void moved(JoinCandidate e, int index) {
			e.heapIndex = index;
		}

		/**
		 * The candidate for a partner topic.
		 *
		 * @param partner the partner topic
		 * @return the candidate, <code>null</code> if there is none
		 */
		public JoinCandidate get(int partner) {
			return byPartner[partner];
		}

		/**
		 * Removes the candidate for a partner topic.
		 *
		 * @param partner the partner topic
		 * @return the removed candidate, <code>null</code> if there was none
		 */
		public JoinCandidate removePartner(int partner) {
			JoinCandidate c = byPartner[partner];
			if (c != null) {
				byPartner[partner] = null;
				removeAt(c.heapIndex);
			}
			return c;
		}

		/**
		 * Adds a candidate (there must be none for its partner yet).
		 *
		 * @param c the candidate
		 */
		public void put(JoinCandidate c) {
			byPartner[c.j] = c;
			offer(c);
		}

		@Override
		public JoinCandidate poll() {
			JoinCandidate c = super.poll();
			if (c != null) {
				byPartner[c.j] = null;
			}
			return c;
		}
	}

	// Produces a huge memory footprint by creating so many jcs...
	protected void createInitialJoinCandidates(
			final TGSolutionListener<T> solutionListener) {
		final int n = maxTopics;
		final JoinCandidate[][] slots = new JoinCandidate[n][];
		for (int i = 0; i < n; i++) {
			slots[i] = new JoinCandidate[Math.max(1, n - 1)];
		}
		// Rows in batches so that the progress can be reported; the rows of a
		// batch are distributed over the threads (row i has n - i - 1 pairs,
		// small chunks balance that).
		int batch = Math.max(threads * 8, n / 50);
		for (int start = 0; start < n; start += batch) {
			final int end = Math.min(n, start + batch);
			runner.run(end - start, 2, new ParallelRunner.RangeTask() {
				public void run(int from, int to) {
					// The long rows (low i) first, so that the short ones fill the gaps.
					for (int k = from; k < to; k++) {
						int i = (end - 1) - k;
						computeInitialRow(i, slots);
					}
				}
			});
			// The pairs done so far: all rows up to end.
			double done = (double) end * (2.0 * n - end - 1) / 2;
			solutionListener.initalizing(done / ((double) n * (n - 1) / 2));
		}
		runner.run(n, new ParallelRunner.RangeTask() {
			public void run(int from, int to) {
				for (int i = from; i < to; i++) {
					allJcs[i] = new JoinQueue(slots[i], n - 1);
				}
			}
		});
	}

	/**
	 * Computes the candidates of the initial topic <code>i</code> with all
	 * topics <code>j &gt; i</code> and stores them for both queues: in row
	 * <code>i</code> at position <code>j - 1</code>, in row <code>j</code> at
	 * position <code>i</code>.
	 *
	 * @param i the row
	 * @param slots the rows
	 */
	protected void computeInitialRow(int i, JoinCandidate[][] slots) {
		int word1 = topics[i].get(0);
		for (int j = i + 1; j < maxTopics; j++) {
			double newLogLikelihood = computeTwoWordLogLikelihood(i, j, word1, topics[j].get(0));
			double newImprovement = newLogLikelihood - topicLogLikelihoods[i] - topicLogLikelihoods[j];
			slots[i][j - 1] = new JoinCandidate(j, newLogLikelihood, newImprovement);
			slots[j][i] = new JoinCandidate(i, newLogLikelihood, newImprovement);
		}
	}

	protected void groupTopics(TGSolutionListener<T> solutionListener) {
		nTopics[0] = maxTopics;
		int[] jcia = new int[1];
		while (nTopics[0] > minTopics) {
			JoinCandidate jc = getBestJoinCandidate(jcia);
			int jci = jcia[0];
			int gone = jc.j;
			joinTopics(jci, gone, jc.logLikelihood, jc.improvement, solutionListener);
			refreshCandidates(jci, gone);
		}
	}

	/**
	 * Joins topic <code>gone</code> into topic <code>jci</code>: merges the
	 * words and counts, updates the totals and informs the listener. The queues
	 * are not touched except that the queue of <code>gone</code> is dropped.
	 *
	 * @param jci the surviving topic
	 * @param gone the vanishing topic
	 * @param logLikelihood the log likelihood of the joined topic
	 * @param improvement the change of the total log likelihood
	 * @param solutionListener the listener
	 */
	protected void joinTopics(int jci, int gone, double logLikelihood, double improvement,
			TGSolutionListener<T> solutionListener) {
		int tSizeI = topicSizes[jci];
		int tSizeJ = topicSizes[gone];

		totalLogLikelihood += (logLikelihood - topicLogLikelihoods[jci] - topicLogLikelihoods[gone]);

		topics[jci].addAll(topics[gone]);
		topicUnionFind.union(gone, jci);
		topicSizes[jci] += tSizeJ;
		sumWordFrTimesLogWordFrByTopic[jci] += sumWordFrTimesLogWordFrByTopic[gone];

		// Compute likelihood for joined topic
		topicLogLikelihoods[jci] = logLikelihood;
		addTopicCounts(jci, gone);
		// Topic at position gone is gone
		topics[gone] = null;
		allJcs[gone] = null;
		topicLogLikelihoods[gone] = 0;
		topicSizes[gone] = 0;

		nTopics[0]--;

		solutionListener.updatedSolution(jci, gone, improvement, tSizeI, tSizeJ, solution);
	}

	/**
	 * Recomputes the candidates of topic <code>x</code> with all other topics
	 * (in parallel): the queue of <code>x</code> is rebuilt and in the queue of
	 * every other topic the candidate for <code>x</code> is replaced and the
	 * candidate for <code>gone</code> (a topic that vanished) removed. The
	 * candidate objects are reused.
	 *
	 * @param x the topic whose candidates are outdated
	 * @param gone a vanished topic whose candidates are removed, -1 for none
	 */
	protected void refreshCandidates(final int x, final int gone) {
		final JoinQueue old = allJcs[x];
		runner.run(maxTopics, new ParallelRunner.RangeTask() {
			public void run(int from, int to) {
				for (int j = from; j < to; j++) {
					if (j != x && topics[j] != null) {
						double newLikelihood = computeTwoTopicLogLikelihood(x, j);
						double newImprovement = newLikelihood - topicLogLikelihoods[x] - topicLogLikelihoods[j];
						JoinQueue qj = allJcs[j];
						if (gone >= 0) {
							qj.removePartner(gone);
						}
						JoinCandidate mine = qj.removePartner(x);
						if (mine == null) {
							mine = new JoinCandidate();
						}
						mine.init(x, newLikelihood, newImprovement);
						qj.put(mine);
						JoinCandidate c = old == null ? null : old.get(j);
						if (c == null) {
							c = new JoinCandidate();
						}
						c.init(j, newLikelihood, newImprovement);
						rowSlots[j] = c;
					}
				}
			}
		});
		JoinCandidate[] row = new JoinCandidate[Math.max(1, nTopics[0] - 1)];
		int c = 0;
		for (int j = 0; j < maxTopics; j++) {
			if (j != x && topics[j] != null) {
				row[c++] = rowSlots[j];
			}
		}
		allJcs[x] = new JoinQueue(row, c);
	}

	/**
	 * Finds the best candidate among the heads of all queues and removes it from
	 * its queue.
	 *
	 * @param ia one element array that receives the index of the topic the candidate belongs to
	 * @return the best join candidate
	 */
	protected JoinCandidate getBestJoinCandidate(int[] ia) {
		JoinCandidate best = null;
		for (int i = 0; i < topics.length; i++) {
			if (allJcs[i] != null) {
				JoinCandidate jc = allJcs[i].peek();
				if (jc != null && (best == null || best.compareTo(jc, ia[0], i) > 0)) {
					best = jc;
					ia[0] = i;
				}
			}
		}
		allJcs[ia[0]].poll();
		return best;
	}
}
