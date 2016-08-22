package org.hhn.topicgrouper.tgimpl;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solution;
import org.hhn.topicgrouper.util.UnionFind;

public class JMLRTopicGrouper<T> extends AbstractTopicGrouper<T> {
	protected final int nWords;
	protected final int maxTopics;

	protected final TIntList[] topics;
	protected final UnionFind topicUnionFind;
	protected final int[] wordToInitialTopic;
	protected final int[] topicSizes;
	protected final int[] topicAges;
	protected final TreeSet<JoinCandidate> joinCandidates;
	protected final double[] topicLikelihoods;
	protected double totalLikelihood;
	protected final int[] nTopics;
	protected final Solution<T> solution;
	protected final int[][] topicFrequencyPerDocuments;
	protected final double[] sumWordFrTimesLogWordFrByTopic;

	// key is word index, value is list of documents with word, list entry
	// consists of document index and word frequency in respective document
	protected final TIntObjectMap<List<DocIndexAndWordFr>> invertedIndex;

	protected final int minTopics;
	protected final DocIndexAndWordFr searchDummy = new DocIndexAndWordFr();

	public JMLRTopicGrouper(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics) {
		super(minWordFrequency, documentProvider);

		if (minWordFrequency < 1) {
			throw new IllegalArgumentException("minWordFrequency must be >= 1");
		}

		this.minTopics = Math.max(1, minTopics);
		nWords = documentProvider.getNumberOfWords();

		wordToInitialTopic = new int[nWords];
		int counter = 0;
		for (int i = 0; i < nWords; i++) {
			if (documentProvider.getWordFrequency(i) >= minWordFrequency) {
				wordToInitialTopic[i] = counter;
				counter++;
			} else {
				wordToInitialTopic[i] = -1;
			}
		}
		maxTopics = counter;

		topicUnionFind = new UnionFind(maxTopics);
		topics = new TIntList[maxTopics];
		topicSizes = new int[maxTopics];
		topicAges = new int[maxTopics];
		joinCandidates = new TreeSet<JoinCandidate>();
		topicLikelihoods = new double[maxTopics];
		nTopics = new int[1];

		topicFrequencyPerDocuments = new int[maxTopics][];
		sumWordFrTimesLogWordFrByTopic = new double[maxTopics];

		invertedIndex = createInvertedIndex();

		solution = createSolution();
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

	protected int[] getTopicIds() {
		int[] topicIds = new int[nTopics[0]];
		int j = 0;
		for (int i = 0; i < topics.length; i++) {
			if (topics[i] != null) {
				topicIds[j++] = i;
			}
		}
		return topicIds;
	}

	protected Solution<T> createSolution() {
		return new Solution<T>() {
			@Override
			public TIntCollection[] getTopics() {
				return topics;
			}

			@Override
			public int getTopicForWord(int wordIndex) {
				int topic = wordToInitialTopic[wordIndex];
				return topic == -1 ? -1 : topicUnionFind.find(topic);
			}

			@Override
			public int[] getTopicIds() {
				return JMLRTopicGrouper.this.getTopicIds();
			}

			@Override
			public T getWord(int wordIndex) {
				return JMLRTopicGrouper.this.documentProvider
						.getWord(wordIndex);
			}

			@Override
			public int getIndex(T word) {
				return JMLRTopicGrouper.this.documentProvider.getIndex(word);
			}

			@Override
			public int getGlobalWordFrequency(int wordIndex) {
				return JMLRTopicGrouper.this.documentProvider
						.getWordFrequency(wordIndex);
			}

			@Override
			public int getTopicFrequency(int topicIndex) {
				return topicSizes[topicIndex];
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
				return JMLRTopicGrouper.this.getHomonyms();
			}

			@Override
			public int getNumberOfTopics() {
				return nTopics[0];
			}
		};
	}

	protected TIntCollection getHomonyms() {
		return null;
	}

	protected double computeOneWordTopicLogLikelihood(int wordIndex) {
		double sum = 0; // Coherence weight log(1).
		for (int i = 0; i < documents.size(); i++) {
			Document<T> d = documents.get(i);
			double wordFrPerDoc = d.getWordFrequency(wordIndex);
			if (wordFrPerDoc > 0 && documentSizes[i] > 0) {
				sum += wordFrPerDoc
						* (Math.log(wordFrPerDoc) - logDocumentSizes[i]);
			}
		}
		return sum;
	}

	protected double computeTwoWordLogLikelihood(int i, int j, int word1,
			int word2) {
		double sum = computeTwoWordLogLikelihoodHelp(word1, word2);
		sum += sumWordFrTimesLogWordFrByTopic[i];
		sum += sumWordFrTimesLogWordFrByTopic[j];
		int sizeSum = topicSizes[i] + topicSizes[j];
		sum -= (sizeSum) * Math.log(sizeSum);

		return sum;
	}

	protected double computeTwoWordLogLikelihoodHelp(int word1, int word2) {
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
						sum += fr
								* (Math.log(fr) - logDocumentSizes[entry1.docIndex]);
					}
				} else {
					if (documentSizes[entry1.docIndex] > 0) {
						sum += entry1.wordFr
								* (Math.log(entry1.wordFr) - logDocumentSizes[entry1.docIndex]);
					}
				}
			}
			for (DocIndexAndWordFr entry2 : l2) {
				searchDummy.docIndex = entry2.docIndex;
				int posEntry1 = Collections.binarySearch(l1, searchDummy);
				if (posEntry1 < 0) {
					if (documentSizes[entry2.docIndex] > 0) {
						sum += entry2.wordFr
								* (Math.log(entry2.wordFr) - logDocumentSizes[entry2.docIndex]);
					}
				}
			}
		}
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
				sum += fr * (Math.log(fr) - logDocumentSizes[i]);
			}
		}

		sum += sumWordFrTimesLogWordFrByTopic[topic1];
		sum += sumWordFrTimesLogWordFrByTopic[topic2];
		int sizeSum = topicSizes[topic1] + topicSizes[topic2];
		sum -= (sizeSum) * Math.log(sizeSum);

		return sum;
	}

	protected void createInitialTopics() {
		int counter = 0;
		for (int i = 0; i < nWords; i++) {
			// Only generate topics for elements occurring often in enough
			// across all entries
			int wordFr = documentProvider.getWordFrequency(i);
			if (wordFr >= minWordFrequency) {
				TIntList topic = new TIntArrayList();
				// Topic with that single element (for a start)
				topic.add(i);
				// at position i
				topics[counter] = topic;
				topicSizes[counter] = documentProvider.getWordFrequency(i);
				topicLikelihoods[counter] = computeOneWordTopicLogLikelihood(i);
				totalLikelihood += topicLikelihoods[counter];
				topicAges[counter] = maxTopics;

				topicFrequencyPerDocuments[counter] = new int[documents.size()];
				for (int j = 0; j < documents.size(); j++) {
					topicFrequencyPerDocuments[counter][j] = documents.get(j)
							.getWordFrequency(i);
				}

				sumWordFrTimesLogWordFrByTopic[counter] = wordFr
						* Math.log(wordFr);

				counter++;
			}
		}
	}

	protected void createInitialJoinCandidates(
			SolutionListener<T> solutionListener) {
		int initMax = maxTopics * (maxTopics - 1);
		int initCounter = 0;
		// double[] coherence = new double[1];

		for (int i = 0; i < maxTopics; i++) {
			// topics[i] may be null if the element's frequency is below
			// frThreshold
			double bestImprovement = Double.NEGATIVE_INFINITY;
			double bestLikelihood = 0;
			int bestJ = -1;

			for (int j = 0; j < maxTopics; j++) {
				if (j != i) {
					double newLikelihood = computeTwoWordLogLikelihood(i, j,
							topics[i].get(0), topics[j].get(0));

					double newImprovement = newLikelihood - topicLikelihoods[i]
							- topicLikelihoods[j];
					if (newImprovement > bestImprovement) {
						bestImprovement = newImprovement;
						bestLikelihood = newLikelihood;
						bestJ = j;
					}
					initCounter++;
					if (initCounter % 100000 == 0) {
						solutionListener.initalizing(((double) initCounter)
								/ initMax);
					}
				}
			}
			if (bestJ != -1) {
				JoinCandidate jc = new JoinCandidate();
				jc.improvement = bestImprovement;
				jc.likelihood = bestLikelihood;
				jc.i = i;
				jc.j = bestJ;
				addToJoinCandiates(i, jc);
			}
		}
	}

	protected void addToJoinCandiates(int i, JoinCandidate jc) {
		joinCandidates.add(jc);
	}

	@Override
	public void solve(SolutionListener<T> solutionListener) {
		// Initialization
		totalLikelihood = 0;
		solutionListener.beforeInitialization(maxTopics, documentSizes.length);
		createInitialTopics();

		createInitialJoinCandidates(solutionListener);

		solutionListener.initialized(solution);

		groupTopics(solutionListener);

		solutionListener.done();
	}

	protected JoinCandidate getBestJoinCandidate() {
		JoinCandidate jc = joinCandidates.last();
		joinCandidates.remove(jc);
		return jc;
	}

	private final List<JoinCandidate> addLaterCache = new ArrayList<JMLRTopicGrouper.JoinCandidate>();

	protected void updateJoinCandidates(JoinCandidate jc) {
		// Save old j-index of jc, cause the join candidate with jc.i == j must
		// be deleted still.
		// jc.i does not need to be saved cause it does not change under the
		// following method call.
		int j = jc.j;
		// Recompute the best join partner for joined topic
		updateJoinCandidateForTopic(jc);
		// Add the new best join partner for topic[jc.i]
		joinCandidates.add(jc);

		Iterator<JoinCandidate> it = joinCandidates.iterator();
		addLaterCache.clear();
		while (it.hasNext()) {
			JoinCandidate jc2 = it.next();
			if (jc2.i == j) {
				it.remove();
			} else if (jc2 != jc
					/*&& (jc2.j == jc.i || jc2.j == j || jc2.j == -1)*/) {
				double newLikelihood = computeTwoTopicLogLikelihood(jc2.i, jc.i);
				double newImprovement = newLikelihood - topicLikelihoods[jc2.i]
						- topicLikelihoods[jc.i];
				if (newImprovement >= jc2.improvement) {
					it.remove();
					jc2.improvement = newImprovement;
					jc2.likelihood = newLikelihood;
					jc2.j = jc.i;
					if (jc2.j != jc.i && jc2.j != j) {
						System.out.println("Stop!");
					}
					addLaterCache.add(jc2);
				} else if (jc2.j == jc.i || jc2.j == j) {
					jc2.j = -1;
				}
			}
		}
		joinCandidates.addAll(addLaterCache);
	}

	protected void groupTopics(SolutionListener<T> solutionListener) {
		nTopics[0] = maxTopics;
		while (nTopics[0] > minTopics) {
			// Get the best join candidate
			JoinCandidate jc = getBestJoinCandidate();
			// Check if jc is invalid
			if (jc.j == -1) {
				// Recompute the best join candidate for jc and sort it in in
				// the right place.
				updateJoinCandidateForTopic(jc);
				joinCandidates.add(jc);
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
					totalLikelihood -= topicLikelihoods[jc.i];
					topicLikelihoods[jc.i] = jc.likelihood;
					totalLikelihood += topicLikelihoods[jc.i];
					int[] a = topicFrequencyPerDocuments[jc.i];
					int[] b = topicFrequencyPerDocuments[jc.j];
					for (int i = 0; i < a.length; i++) {
						a[i] += b[i];
					}
					// Topic at position jc.j is gone
					topics[jc.j] = null;
					totalLikelihood -= topicLikelihoods[jc.j];
					topicLikelihoods[jc.j] = 0;
					topicSizes[jc.j] = 0;

					nTopics[0]--;
					topicAges[jc.i] = nTopics[0];

					solutionListener.updatedSolution(jc.i, jc.j,
							jc.improvement, t1Size, t2Size, solution);
				}
				updateJoinCandidates(jc);
			}
		}
	}

	protected boolean handleHomonymicTopic(JoinCandidate jc) {
		return false;
	}

	protected void updateJoinCandidateForTopic(JoinCandidate jc) {
		double bestImprovement = Double.NEGATIVE_INFINITY;
		double bestLikelihood = 0;
		int bestJ = -1;
		for (int j = 0; j < maxTopics; j++) {
			if (j != jc.i && topics[j] != null && 
					(jc.j != -1 || topicAges[jc.i] <= topicAges[j])) {
				double newLikelihood = computeTwoTopicLogLikelihood(jc.i, j);
				double newImprovement = newLikelihood - topicLikelihoods[jc.i]
						- topicLikelihoods[j];
				if (newImprovement > bestImprovement) {
//					if (jc.j == -1 && topicAges[jc.i] > topicAges[j] && newImprovement > jc.improvement) {
//						System.out.println("stop!");
//					}
					bestImprovement = newImprovement;
					bestLikelihood = newLikelihood;
					bestJ = j;
				}
			}
		}
		jc.improvement = bestImprovement;
		jc.likelihood = bestLikelihood;
		jc.j = bestJ;
	}

	protected static class JoinCandidate implements Comparable<JoinCandidate> {
		public double likelihood;
		public double improvement;
		public int i;
		public int j;

		@Override
		public int compareTo(JoinCandidate o) {
			if (improvement == o.improvement) {
				return 0;
			}
			return improvement > o.improvement ? 1 : -1;
		}

		@Override
		public String toString() {
			return "[l:" + likelihood + " imp:" + improvement + " i:" + i
					+ " j:" + j + "]";
		}
	}

	protected static class DocIndexAndWordFr implements
			Comparable<DocIndexAndWordFr> {
		public int docIndex;
		public int wordFr;

		public int compareTo(DocIndexAndWordFr o) {
			return docIndex - o.docIndex;
		}
	}
}
