package org.hhn.topicgrouper.tg.impl;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSenseSolution;
import org.hhn.topicgrouper.tg.TGSenseSolutionListener;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;

/**
 * Topic Grouper that can give a word several senses (homonym handling).
 * <p>
 * The joins are chosen exactly as in {@link EHACTopicGrouper}. Whenever the
 * chosen join merges two single-word topics {w} and {v}, the occurrences of w
 * in the documents that also contain v become a new sense w<sub>1</sub> of the
 * word, and the new topic is {v, w<sub>1</sub>}; w keeps its other documents
 * and stays a single-word topic, which is joined (or split again) later. The
 * split only happens if the co-occurrence is selective: with n documents of w,
 * m of them containing v, D documents in all and d<sub>v</sub> documents
 * containing v, the lift m D / (n d<sub>v</sub>) = P(v | w) / P(v) must be at
 * least <code>minSenseLift</code>. A word occurring in most documents has a
 * lift of about 1 with every partner and therefore never gets a sense.
 * Otherwise the plain join is done.
 * <p>
 * When two topics are joined that both contain a type (sense or remainder) of
 * the same word, the two types become one again. This happens after the join
 * has been chosen and does not influence the choice. The derivation and the
 * evaluation are in <code>doc/topic-grouper.md</code>.
 * <p>
 * The solution handed to listeners is a {@link TGSenseSolution}; listeners that
 * implement {@link TGSenseSolutionListener} are told about the new senses. With
 * an infinite <code>minSenseLift</code> the solver behaves exactly like
 * {@link EHACTopicGrouper}. A split costs O(|D<sub>w</sub>|) plus the update of
 * the join candidates of the two topics involved, like an ordinary join.
 *
 * @param <T>
 *            the type of the words
 */
public class SenseSplittingTopicGrouper<T> extends EHACTopicGrouper<T> {
	/** Default of the minimum lift of a split. */
	public static final double DEFAULT_MIN_SENSE_LIFT = 2;

	private final double minSenseLift;

	// Per original word: frequency of the remaining (not split off) occurrences.
	private int[] wordFr;
	// Per sense (index = word type - number of words):
	private final TIntList senseWord = new TIntArrayList();
	private final TIntList senseSlot = new TIntArrayList();
	private final TIntList senseFr = new TIntArrayList();
	private final List<int[]> senseDocs = new ArrayList<int[]>();
	// Per topic slot: original word -> its type in the topic, only for words with senses (null if none).
	private TIntIntHashMap[] splitTypes;
	private int merges;

	// Per document of the word to split: whether it goes to the new sense.
	private boolean[] toSense;
	private boolean[] savedToSense;

	/**
	 * Creates a solver with {@link #DEFAULT_MIN_SENSE_LIFT}.
	 *
	 * @param minWordFrequency
	 *            only words occurring at least this often are grouped
	 * @param documentProvider
	 *            the training documents
	 * @param minTopics
	 *            the number of topics at which the algorithm stops
	 */
	public SenseSplittingTopicGrouper(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics) {
		this(minWordFrequency, documentProvider, minTopics, DEFAULT_MIN_SENSE_LIFT);
	}

	/**
	 * Creates a solver with the given minimum lift of a split.
	 *
	 * @param minWordFrequency
	 *            only words occurring at least this often are grouped
	 * @param documentProvider
	 *            the training documents
	 * @param minTopics
	 *            the number of topics at which the algorithm stops
	 * @param minSenseLift
	 *            minimum lift of a split (&gt;= 0; 0 accepts every split,
	 *            {@link Double#POSITIVE_INFINITY} switches splitting off)
	 */
	public SenseSplittingTopicGrouper(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics, double minSenseLift) {
		this(minWordFrequency, documentProvider, minTopics, minSenseLift, 1);
	}

	/**
	 * Creates a solver with the given minimum lift of a split and number of threads.
	 *
	 * @param minWordFrequency
	 *            only words occurring at least this often are grouped
	 * @param documentProvider
	 *            the training documents
	 * @param minTopics
	 *            the number of topics at which the algorithm stops
	 * @param minSenseLift
	 *            minimum lift of a split (&gt;= 0; 0 accepts every split,
	 *            {@link Double#POSITIVE_INFINITY} switches splitting off)
	 * @param threads
	 *            the number of threads to use, including the calling one
	 */
	public SenseSplittingTopicGrouper(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics, double minSenseLift, int threads) {
		super(minWordFrequency, documentProvider, minTopics, threads);
		if (!(minSenseLift >= 0)) {
			throw new IllegalArgumentException("minSenseLift must be >= 0");
		}
		this.minSenseLift = minSenseLift;
	}

	/**
	 * The threshold a split must reach.
	 *
	 * @return the minimum lift of a split
	 */
	public double getMinSenseLift() {
		return minSenseLift;
	}

	/**
	 * The number of senses created.
	 *
	 * @return the number of splits performed by the last run
	 */
	public int getNumberOfSplits() {
		return senseWord.size();
	}

	/**
	 * The number of senses that vanished again.
	 *
	 * @return the number of times two types of a word became one again in the last run
	 */
	public int getNumberOfMerges() {
		return merges;
	}

	@Override
	protected TGSolution<T> createSolution() {
		return new SenseSolution();
	}

	/** @return the frequency of a word type (word or sense) */
	private int typeFr(int type) {
		return type < nWords ? wordFr[type] : senseFr.get(type - nWords);
	}

	@Override
	protected void createInitialJoinCandidates(TGSolutionListener<T> solutionListener) {
		wordFr = new int[nWords];
		for (int i = 0; i < nWords; i++) {
			wordFr[i] = documentProvider.getWordFrequency(i);
		}
		senseWord.clear();
		senseSlot.clear();
		senseFr.clear();
		senseDocs.clear();
		splitTypes = new TIntIntHashMap[nWords];
		merges = 0;
		toSense = new boolean[documents.size()];
		savedToSense = new boolean[documents.size()];
		super.createInitialJoinCandidates(solutionListener);
	}

	/**
	 * Marks the documents of the single-word topic a that contain the word of the
	 * single-word topic b in {@link #toSense} (in the order of the inverted index
	 * of the word).
	 *
	 * @return the log likelihood change of moving them into b, or NaN if no
	 *         split is done (all or none of the documents contain the partner, or
	 *         the lift is too small)
	 */
	private double markDocuments(int a, int b) {
		int word = topics[a].get(0);
		int[] rowA = topicFrequencyPerDocuments[a];
		int[] rowB = topicFrequencyPerDocuments[b];
		int n = 0;
		int m = 0;
		double x = 0;
		double gain = 0;
		for (DocIndexAndWordFr e : invertedIndex.get(word)) {
			int d = e.docIndex;
			if (rowA[d] > 0) {
				boolean withPartner = rowB[d] > 0;
				toSense[n++] = withPartner;
				if (withPartner) {
					m++;
					x += rowA[d];
					gain += xLogX(rowA[d] + rowB[d]) - xLogX(rowA[d]) - xLogX(rowB[d]);
				}
			}
		}
		if (m == 0 || m == n) {
			return Double.NaN;
		}
		if (minSenseLift > 0) {
			int partner = topics[b].get(0);
			int dv = 0;
			for (DocIndexAndWordFr e : invertedIndex.get(partner)) {
				if (rowB[e.docIndex] > 0) {
					dv++;
				}
			}
			double lift = m * (double) documentSizes.length / ((double) n * dv);
			if (lift < minSenseLift) {
				return Double.NaN;
			}
		}
		double nB = topicSizes[b];
		return gain - (xLogX(nB + x) - xLogX(nB) - xLogX(x));
	}

	/**
	 * The log likelihood of the topic from its current counts (over its document
	 * list); also refreshes its document part.
	 */
	private double computeTopicLogLikelihood(int topic) {
		int[] row = topicFrequencyPerDocuments[topic];
		int[] docs = topicDocs[topic];
		int n = topicDocsSize[topic];
		double sum = 0;
		for (int k = 0; k < n; k++) {
			int d = docs[k];
			if (row[d] > 0 && documentSizes[d] > 0) {
				sum += row[d] * (Math.log(row[d]) - logDocumentSizes[d]);
			}
		}
		topicDocPart[topic] = sum;
		return sum + sumWordFrTimesLogWordFrByTopic[topic] - xLogX(topicSizes[topic]);
	}

	private void putSplitType(int slot, int word, int type) {
		if (splitTypes[slot] == null) {
			splitTypes[slot] = new TIntIntHashMap();
		}
		splitTypes[slot].put(word, type);
	}

	/**
	 * Moves the occurrences of the word of the single-word topic a in the
	 * documents marked in {@link #toSense} into b as a new sense.
	 */
	private void applySplit(int a, int b, TGSolutionListener<T> solutionListener, double gain) {
		int word = topics[a].get(0);
		int[] rowA = topicFrequencyPerDocuments[a];
		int[] rowB = topicFrequencyPerDocuments[b];
		TIntList docs = new TIntArrayList();
		int x = 0;
		int n = 0;
		for (DocIndexAndWordFr e : invertedIndex.get(word)) {
			int d = e.docIndex;
			if (rowA[d] > 0) {
				if (toSense[n]) {
					docs.add(d);
					rowB[d] += rowA[d];
					x += rowA[d];
					rowA[d] = 0;
				}
				n++;
			}
		}
		addTopicDocs(b, docs.toArray(), docs.size());
		totalLogLikelihood -= topicLogLikelihoods[a] + topicLogLikelihoods[b];
		topicSizes[a] -= x;
		topicSizes[b] += x;
		sumWordFrTimesLogWordFrByTopic[a] = xLogX(topicSizes[a]);
		sumWordFrTimesLogWordFrByTopic[b] += xLogX(x);
		topicLogLikelihoods[a] = computeTopicLogLikelihood(a);
		topicLogLikelihoods[b] = computeTopicLogLikelihood(b);
		totalLogLikelihood += topicLogLikelihoods[a] + topicLogLikelihoods[b];

		int sense = nWords + senseWord.size();
		topics[b].add(sense);
		putSplitType(a, word, word);
		putSplitType(b, word, sense);
		senseWord.add(word);
		senseSlot.add(b);
		senseFr.add(x);
		senseDocs.add(docs.toArray());
		wordFr[word] -= x;

		if (solutionListener instanceof TGSenseSolutionListener) {
			((TGSenseSolutionListener<T>) solutionListener).splitSense(b, word,
					sense, docs.size(), gain, (TGSenseSolution<T>) solution);
		}
	}

	/**
	 * After topic gone has been joined into jci: makes one type of every two types
	 * of the same word. The remaining word (the remainder) is kept if it is one of
	 * them, otherwise the older sense.
	 *
	 * @return the log likelihood gain of the merges
	 */
	private double mergeSharedTypes(int jci, int gone) {
		TIntIntHashMap mg = splitTypes[gone];
		splitTypes[gone] = null;
		double gain = 0;
		if (mg == null) {
			return gain;
		}
		TIntIntIterator it = mg.iterator();
		while (it.hasNext()) {
			it.advance();
			int word = it.key();
			int other = it.value();
			TIntIntHashMap mj = splitTypes[jci];
			if (mj == null || !mj.containsKey(word)) {
				putSplitType(jci, word, other);
				continue;
			}
			int mine = mj.get(word);
			int keep = Math.min(mine, other); // the word itself (< nWords) or the older sense
			int drop = Math.max(mine, other);
			int a = typeFr(keep);
			int b = typeFr(drop);
			int ds = drop - nWords;
			if (keep < nWords) {
				wordFr[keep] += b;
			} else {
				int ks = keep - nWords;
				senseFr.set(ks, a + b);
				int[] d1 = senseDocs.get(ks);
				int[] d2 = senseDocs.get(ds);
				int[] d = Arrays.copyOf(d1, d1.length + d2.length);
				System.arraycopy(d2, 0, d, d1.length, d2.length);
				Arrays.sort(d);
				senseDocs.set(ks, d);
			}
			senseFr.set(ds, 0);
			senseDocs.set(ds, new int[0]);
			topics[jci].remove(drop);
			double g = xLogX(a + b) - xLogX(a) - xLogX(b);
			sumWordFrTimesLogWordFrByTopic[jci] += g;
			gain += g;
			mj.put(word, keep);
			merges++;
		}
		return gain;
	}

	/** Joins topic gone into topic jci; the candidate queues are not touched. */
	private void joinTopics(int jci, int gone, double improvement, TGSolutionListener<T> solutionListener) {
		double ll = computeTwoTopicLogLikelihood(jci, gone);
		int tSizeI = topicSizes[jci];
		int tSizeJ = topicSizes[gone];
		totalLogLikelihood += ll - topicLogLikelihoods[jci] - topicLogLikelihoods[gone];
		topics[jci].addAll(topics[gone]);
		topicUnionFind.union(gone, jci);
		topicSizes[jci] += tSizeJ;
		sumWordFrTimesLogWordFrByTopic[jci] += sumWordFrTimesLogWordFrByTopic[gone];
		topicLogLikelihoods[jci] = ll;
		addTopicCounts(jci, gone);
		// The join was chosen as usual; only now the types of a word in the joined topic become one.
		double gain = mergeSharedTypes(jci, gone);
		topicLogLikelihoods[jci] += gain;
		totalLogLikelihood += gain;
		topics[gone] = null;
		allJcs[gone] = null;
		topicLogLikelihoods[gone] = 0;
		topicSizes[gone] = 0;
		nTopics[0]--;
		solutionListener.updatedSolution(jci, gone, improvement, tSizeI, tSizeJ, solution);
	}

	@Override
	protected void groupTopics(TGSolutionListener<T> solutionListener) {
		nTopics[0] = maxTopics;
		int[] jcia = new int[1];
		while (nTopics[0] > minTopics) {
			JoinCandidate jc = getBestJoinCandidate(jcia);
			int jci = jcia[0];
			int other = jc.j;

			// Two single words: either may get the sense, the one with the larger gain does.
			int split = -1;
			double bestGain = Double.NEGATIVE_INFINITY;
			if (topics[jci].size() == 1 && topics[other].size() == 1) {
				for (int k = 0; k < 2; k++) {
					int s = k == 0 ? jci : other;
					double g = markDocuments(s, k == 0 ? other : jci);
					if (!Double.isNaN(g) && g > bestGain) {
						bestGain = g;
						split = s;
						System.arraycopy(toSense, 0, savedToSense, 0, toSense.length);
					}
				}
			}
			if (split >= 0) {
				int b = split == jci ? other : jci;
				System.arraycopy(savedToSense, 0, toSense, 0, toSense.length);
				applySplit(split, b, solutionListener, bestGain);
				refreshCandidates(split, -1);
				refreshCandidates(b, -1);
			} else {
				joinTopics(jci, other, jc.improvement, solutionListener);
				refreshCandidates(jci, other);
			}
		}
	}

	/** The live solution including the senses. */
	protected class SenseSolution extends DefaultTGSolution implements TGSenseSolution<T> {
		/** Creates the view; it holds no state of its own. */
		public SenseSolution() {
		}

		@Override
		public int getTopicForWord(int wordIndex) {
			if (wordIndex < nWords) {
				return super.getTopicForWord(wordIndex);
			}
			return topicUnionFind.find(senseSlot.get(wordIndex - nWords));
		}

		@Override
		public int getGlobalWordFrequency(int wordIndex) {
			return wordIndex < nWords ? wordFr[wordIndex] : senseFr.get(wordIndex - nWords);
		}

		public int getNumberOfWordTypes() {
			return nWords + senseWord.size();
		}

		public int getOriginalWord(int type) {
			return type < nWords ? type : senseWord.get(type - nWords);
		}

		public double getSenseShare(int type) {
			int total = documentProvider.getWordFrequency(getOriginalWord(type));
			return total == 0 ? 0 : ((double) getGlobalWordFrequency(type)) / total;
		}

		public int[] getSenseDocuments(int type) {
			return type < nWords ? null : senseDocs.get(type - nWords);
		}

		public int getNumberOfSplits() {
			return senseWord.size();
		}
	}
}
