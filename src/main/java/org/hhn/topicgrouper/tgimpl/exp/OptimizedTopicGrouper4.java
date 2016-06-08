package org.hhn.topicgrouper.tgimpl.exp;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solution;
import org.hhn.topicgrouper.tgimpl.AbstractTopicGrouper;
import org.hhn.topicgrouper.util.UnionFind;

public class OptimizedTopicGrouper4<T> extends AbstractTopicGrouper<T> {
	protected final int nWords;

	protected final TIntList[] topics;
	// TODO: The union find and the topicSize-array are redundant.
	// Switch to union find only at a later time.
	protected final UnionFind topicUnionFind;
	protected final int[] topicSizes;
	protected final JoinCandidate[] joinCandidates;
	protected final double[] topicLikelihoods;
	protected double totalLikelihood;
	protected final int[] nTopics;
	protected final Solution<T> solution2;
	protected final int[][] topicFrequencyPerDocuments;
	protected final double[] sumWordFrTimesLogWordFrByTopic;
	protected final double[] onePlusLambdaDivDocSizes;

	// key is word index, value is list of documents with word, list entry
	// consists of document index and word frequency in respective document
	protected TIntObjectMap<List<DocIndexAndWordFr>> invertedIndex;

	protected final int minTopics;

	public OptimizedTopicGrouper4(int minWordFrequency, double lambda,
			DocumentProvider<T> documentProvider, int minTopics) {
		super(minWordFrequency, lambda, documentProvider);

		if (minWordFrequency < 1) {
			throw new IllegalArgumentException("minWordFrequency must be >= 1");
		}

		this.minTopics = Math.max(1, minTopics);
		nWords = documentProvider.getNumberOfWords();
		topicUnionFind = new UnionFind(nWords);
		topics = new TIntList[nWords];
		topicSizes = new int[nWords];
		joinCandidates = new JoinCandidate[nWords];
		topicLikelihoods = new double[nWords];
		nTopics = new int[1];

		topicFrequencyPerDocuments = new int[nWords][];
		for (int i = 0; i < topicFrequencyPerDocuments.length; i++) {
			topicFrequencyPerDocuments[i] = new int[documents.size()];
			for (int j = 0; j < documents.size(); j++) {
				topicFrequencyPerDocuments[i][j] = documents.get(j)
						.getWordFrequency(i);
			}
		}

		sumWordFrTimesLogWordFrByTopic = new double[documentProvider
				.getNumberOfWords()];
		for (int i = 0; i < sumWordFrTimesLogWordFrByTopic.length; i++) {
			int wordFr = documentProvider.getWordFrequency(i);
			if (wordFr > 0) {
				sumWordFrTimesLogWordFrByTopic[i] = wordFr * Math.log(wordFr);
			}
		}

		onePlusLambdaDivDocSizes = new double[documents.size()];
		for (int i = 0; i < documents.size(); i++) {
			onePlusLambdaDivDocSizes[i] = 1 + lambda / documentSizes[i];
		}

		invertedIndex = createInvertedIndex();

		solution2 = createSolution();
	}

	protected TIntObjectMap<List<DocIndexAndWordFr>> createInvertedIndex() {
		TIntObjectMap<List<DocIndexAndWordFr>> invertedIndex = new TIntObjectHashMap<List<DocIndexAndWordFr>>();
		for (int i = 0; i < documents.size(); i++) {
			Document<T> d = documents.get(i);
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int wordIndex = it.next();
				List<DocIndexAndWordFr> value = invertedIndex.get(wordIndex);
				if (value == null) {
					value = new ArrayList<DocIndexAndWordFr>();
					invertedIndex.put(wordIndex, value);
				}
				DocIndexAndWordFr entry = new DocIndexAndWordFr();
				entry.docIndex = i;
				entry.wordFr = d.getWordFrequency(wordIndex);
				int position = Collections.binarySearch(value, entry);
				value.add(-position - 1, entry);
			}
		}
		return invertedIndex;
	}

	protected Solution<T> createSolution() {
		return new Solution<T>() {
			@Override
			public TIntCollection[] getTopicsAlt() {
				return topics;
			}

			@Override
			public int getTopicForWord(int wordIndex) {
				return topicUnionFind.find(wordIndex);
			}

			@Override
			public T getWord(int wordIndex) {
				return OptimizedTopicGrouper4.this.documentProvider
						.getWord(wordIndex);
			}

			@Override
			public int getIndex(T word) {
				return OptimizedTopicGrouper4.this.documentProvider
						.getIndex(word);
			}

			@Override
			public int getGlobalWordFrequency(int wordIndex) {
				return OptimizedTopicGrouper4.this.documentProvider
						.getWordFrequency(wordIndex);
			}

			@Override
			public int getTopicFrequency(int topicIndex) {
				return topicSizes[topicIndex];
			}

			@Override
			public List<TIntCollection> getTopics() {
				return null;
			}

			@Override
			public double[] getTopicLikelihoods() {
				return topicLikelihoods;
			}

			@Override
			public double getTotalLikelhood() {
				return totalLikelihood;
			}

			@Override
			public TIntCollection getHomonymns() {
				return null;
			}

			@Override
			public int getNumberOfTopics() {
				return nTopics[0];
			}
		};
	}

	protected double computeOneWordTopicLogLikelihood(int wordIndex) {
		double sum = 0;
		for (int i = 0; i < documents.size(); i++) {
			Document<T> d = documents.get(i);
			double wordFrPerDoc = d.getWordFrequency(wordIndex);
			if (wordFrPerDoc > 0 && documentSizes[i] > 0) {
				sum += onePlusLambdaDivDocSizes[i] * wordFrPerDoc
						* (Math.log(wordFrPerDoc) - logDocumentSizes[i]);
			}
		}
		return sum;
	}

	protected final DocIndexAndWordFr searchDummy = new DocIndexAndWordFr();

	protected double computeTwoWordLogLikelihood(int word1, int word2) {
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

		sum += sumWordFrTimesLogWordFrByTopic[word1];
		sum += sumWordFrTimesLogWordFrByTopic[word2];
		int sizeSum = topicSizes[word1] + topicSizes[word2];
		sum -= (sizeSum) * Math.log(sizeSum);

		return sum;
	}

	protected double computeTwoTopicLogLikelihood(int topic1, int topic2) {
		double sum = 0;
		int[] frTopicPerDocument1 = topicFrequencyPerDocuments[topic1];
		int[] frTopicPerDocument2 = topicFrequencyPerDocuments[topic2];
		for (int i = 0; i < documents.size(); i++) {
			if ((frTopicPerDocument1[i] > 0 || frTopicPerDocument2[i] > 0)
					&& documentSizes[i] > 0) {
				int fr = frTopicPerDocument1[i] + frTopicPerDocument2[i];
				sum += onePlusLambdaDivDocSizes[i] * fr
						* (Math.log(fr) - logDocumentSizes[i]);
			}
		}

		sum += sumWordFrTimesLogWordFrByTopic[topic1];
		sum += sumWordFrTimesLogWordFrByTopic[topic2];
		int sizeSum = topicSizes[topic1] + topicSizes[topic2];
		sum -= (sizeSum) * Math.log(sizeSum);

		return sum;
	}

	@Override
	public void solve(SolutionListener<T> solutionListener) {
		// Initialization
		totalLikelihood = 0;
		int maxTopics = 0;
		for (int i = 0; i < nWords; i++) {
			// Only generate topics for elements occurring often in enough
			// across all entries
			if (documentProvider.getWordFrequency(i) >= minWordFrequency) {
				double likelihood = computeOneWordTopicLogLikelihood(i);
				if (likelihood != 0) {
					TIntList topic = new TIntArrayList();
					// Topic with that single element (for a start)
					topic.add(i);
					// at position i
					topics[i] = topic;
					topicSizes[i] = documentProvider.getWordFrequency(i);
					topicLikelihoods[i] = likelihood;
					totalLikelihood += topicLikelihoods[i];
					maxTopics++;
				}
			}
		}

		solutionListener.beforeInitialization(maxTopics, documentSizes.length);

		int initMax = nWords * (nWords - 1) / 2;
		int initCounter = 0;

		for (int i = 0; i < nWords - 1; i++) {
			// topics[i] may be null if the element's frequency is below
			// frThreshold
			if (topics[i] != null) {
				double bestImprovement = Double.NEGATIVE_INFINITY;
				double bestLikelihood = 0;
				int bestJ = -1;
				for (int j = i + 1; j < nWords; j++) {
					if (topics[j] != null) {
						double newLikelihood = computeTwoWordLogLikelihood(i, j);
						double newImprovement = newLikelihood
								- topicLikelihoods[i] - topicLikelihoods[j];
						if (newImprovement > bestImprovement) {
							bestImprovement = newImprovement;
							bestLikelihood = newLikelihood;
							bestJ = j;
						}
					}
					initCounter++;
					if (initCounter % 100000 == 0) {
						solutionListener.initalizing(((double) initCounter)
								/ initMax);
					}
				}
				// Invariant: topic[j] is the best join partner for topic[i]
				// with j > i!
				if (bestJ != -1) {
					JoinCandidate jc = new JoinCandidate();
					jc.improvement = bestImprovement;
					jc.likelihood = bestLikelihood;
					jc.i = i;
					jc.j = bestJ;
					joinCandidates[i] = jc;
				}
			}
		}

		solutionListener.initialized(solution2);

		nTopics[0] = maxTopics;
		while (nTopics[0] > minTopics) {
			// Get the best join candidate
			bubbleSort(joinCandidates, nTopics[0]);
			int max = ((nTopics[0] / 2) / 2) * 2; // TODO
			for (int jcIndex = 0; jcIndex < max; jcIndex++) {
				JoinCandidate jc = joinCandidates[jcIndex];
				if (jc != null) {
					// joinCandidates[jcIndex] = null;
					if (topics[jc.i] != null && topics[jc.j] != null) {
						int t1Size = 0, t2Size = 0;
						int tid = getHomonymicTopic(jc);
						if (tid == -1) {
							t1Size = topicSizes[jc.i];
							t2Size = topicSizes[jc.j];
							// Join the topics at position jc.i
							topics[jc.i].addAll(topics[jc.j]);
							topicUnionFind.union(jc.j, jc.i);
							topicSizes[jc.i] += t2Size;
							sumWordFrTimesLogWordFrByTopic[jc.i] += sumWordFrTimesLogWordFrByTopic[jc.j];
							// Compute likelihood for joined topic
							totalLikelihood -= topicLikelihoods[jc.i];
							topicLikelihoods[jc.i] = jc.likelihood;
							totalLikelihood += topicLikelihoods[jc.i];
							int[] a = topicFrequencyPerDocuments[jc.i];
							int[] b = topicFrequencyPerDocuments[jc.j];
							for (int i = 0; i < a.length; i++) {
								a[i] += b[i];
							}
						} else {
							handleHomonym(tid);
						}
						// Topic at position jc.j is gone
						topics[jc.j] = null;
						totalLikelihood -= topicLikelihoods[jc.j];
						topicLikelihoods[jc.j] = 0;
						topicSizes[jc.j] = 0;

						nTopics[0]--;

						solutionListener.updatedSolution(jc.i, jc.j,
								jc.improvement, t1Size, t2Size, solution2);
					} else if (topics[jc.i] != null && topics[jc.j] == null) {
						if (!updateJoinCandidateForTopic(jc)) {
							joinCandidates[jcIndex] = null;
						}
					} else {
						joinCandidates[jcIndex] = null;
					}
				}
			}
			for (int jcIndex = 0; jcIndex < max / 2; jcIndex++) {
				JoinCandidate jc = joinCandidates[jcIndex];
				// joinCandidates[jcIndex] = null;
				if (jc != null) {
					// Check for all jc2 with jc2.i < jc.i if jc is a better
					// join
					// partner than the old one
					for (int i = max / 2 + 1; i < nTopics[0] - 1; i++) {
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
					// Recompute the best join partner for joined topic
					if (!updateJoinCandidateForTopic(jc)) {
						joinCandidates[jcIndex] = null;
					}
				}
			}
		}
	}

	protected int getHomonymicTopic(JoinCandidate jc) {
		return -1;
	}

	protected void handleHomonym(int tid) {
	}

	protected boolean updateJoinCandidateForTopic(JoinCandidate jc) {
		// Recompute the best join partner for joined topic
		double bestImprovement = Double.NEGATIVE_INFINITY;
		double bestLikelihood = 0;
		int bestJ = -1;
		for (int j = jc.i + 1; j < nWords; j++) {
			if (topics[j] != null) {
				double newLikelihood = computeTwoTopicLogLikelihood(jc.i, j);
				double newImprovement = newLikelihood - topicLikelihoods[jc.i]
						- topicLikelihoods[j];
				if (newImprovement > bestImprovement) {
					bestImprovement = newImprovement;
					bestLikelihood = newLikelihood;
					bestJ = j;
				}
			}
		}
		if (bestJ != -1) {
			jc.improvement = bestImprovement;
			jc.likelihood = bestLikelihood;
			jc.j = bestJ;
			return true;
		}
		return false;
	}

	protected static class JoinCandidate {
		protected double likelihood;
		protected double improvement;

		// Important invariant i < j!
		// Kept throughout the algorithm...
		protected int i;
		protected int j;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof JoinCandidate) {
				JoinCandidate jc = (JoinCandidate) obj;
				return jc.likelihood == likelihood
						&& jc.improvement == improvement && jc.i == i
						&& jc.j == j;
			}
			return false;
		}

		@Override
		public String toString() {
			return "[l:" + likelihood + " imp:" + improvement + " i:" + i
					+ " j:" + j + "]";
		}
	}

	public static void bubbleSort(JoinCandidate[] x, int max) {
		boolean unsorted = true;
		JoinCandidate temp;

		while (unsorted) {
			unsorted = false;
			for (int i = max - 1; i > 0; i--) {
				if (x[i] != null
						&& (x[i - 1] == null || x[i].improvement > x[i - 1].improvement)) {
					temp = x[i];
					x[i] = x[i - 1];
					x[i - 1] = temp;
					unsorted = true;
				}
			}
		}
	}

	protected static class DocIndexAndWordFr implements
			Comparable<DocIndexAndWordFr> {
		protected int docIndex;
		protected int wordFr;

		public int compareTo(DocIndexAndWordFr o) {
			return docIndex - o.docIndex;
		}
	}
}
