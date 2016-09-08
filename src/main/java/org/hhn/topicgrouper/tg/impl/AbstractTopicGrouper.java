package org.hhn.topicgrouper.tg.impl;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.util.UnionFind;

public abstract class AbstractTopicGrouper<T> implements TGSolver<T> {
	protected final int minWordFrequency;
	protected final DocumentProvider<T> documentProvider;
	protected final int[] documentSizes;
	protected final double[] logDocumentSizes;
	protected final List<Document<T>> documents;

	protected final int nWords;
	protected final int maxTopics;

	protected final TIntList[] topics;
	protected final UnionFind topicUnionFind;
	protected final int[] wordToInitialTopic;
	protected final int[] topicSizes;
	protected final double[] topicLogLikelihoods;
	protected double totalLogLikelihood;
	protected final int[] nTopics;
	protected final TGSolution<T> solution;
	protected final int[][] topicFrequencyPerDocuments;
	protected final double[] sumWordFrTimesLogWordFrByTopic;

	// key is word index, value is list of documents with word, list entry
	// consists of document index and word frequency in respective document
	protected final TIntObjectMap<List<DocIndexAndWordFr>> invertedIndex;

	protected final int minTopics;
	protected final DocIndexAndWordFr searchDummy = new DocIndexAndWordFr();

	protected final HomonymHandler homonymHandler;
	private int deferredJCRecomputations;

	public AbstractTopicGrouper(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics) {
		this(minWordFrequency, documentProvider, minTopics, 0);
	}

	public AbstractTopicGrouper(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics, double hEpsilon) {
		this.minWordFrequency = minWordFrequency;
		this.documentProvider = documentProvider;
		this.documents = documentProvider.getDocuments();
		this.documentSizes = getDocumentSizes();
		this.logDocumentSizes = getLogDocumentSizes();

		if (hEpsilon > 0) {
			homonymHandler = createHomonymHandler(hEpsilon);
		} else {
			homonymHandler = null;
		}

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
		createJoinCandidateList(minTopics);
		topicUnionFind = new UnionFind(maxTopics);
		topics = new TIntList[maxTopics];
		topicSizes = new int[maxTopics];
		topicLogLikelihoods = new double[maxTopics];
		nTopics = new int[1];

		topicFrequencyPerDocuments = new int[maxTopics][];
		sumWordFrTimesLogWordFrByTopic = new double[maxTopics];

		invertedIndex = createInvertedIndex();

		solution = createSolution();
	}

	protected TGSolution<T> createSolution() {
		return new TGSolution<T>() {
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
				int[] topicIds = new int[nTopics[0]];
				int j = 0;
				for (int i = 0; i < topics.length; i++) {
					if (topics[i] != null) {
						topicIds[j++] = i;
					}
				}
				return topicIds;
			}

			@Override
			public T getWord(int wordIndex) {
				return AbstractTopicGrouper.this.documentProvider
						.getWord(wordIndex);
			}

			@Override
			public int getIndex(T word) {
				return AbstractTopicGrouper.this.documentProvider
						.getIndex(word);
			}

			@Override
			public int getGlobalWordFrequency(int wordIndex) {
				return AbstractTopicGrouper.this.documentProvider
						.getWordFrequency(wordIndex);
			}

			@Override
			public int getTopicFrequency(int topicIndex) {
				return topicSizes[topicIndex];
			}

			@Override
			public double[] getTopicLogLikelihoods() {
				return topicLogLikelihoods;
			}

			@Override
			public double getTotalLogLikelhood() {
				return totalLogLikelihood;
			}

			@Override
			public TIntCollection getHomonymns() {
				return AbstractTopicGrouper.this.getHomonyms();
			}

			@Override
			public int getNumberOfTopics() {
				return nTopics[0];
			}
		};
	}

	private int[] getDocumentSizes() {
		int[] documentSizes = new int[documents.size()];
		for (int i = 0; i < documents.size(); i++) {
			int sum = 0;
			TIntIterator indices = documents.get(i).getWordIndices().iterator();
			while (indices.hasNext()) {
				int index = indices.next();
				if (documentProvider.getWordFrequency(index) >= minWordFrequency) {
					sum += documents.get(i).getWordFrequency(index);
				}
			}
			documentSizes[i] = sum;
		}
		return documentSizes;
	}

	private double[] getLogDocumentSizes() {
		double[] logDocumentSizes = new double[documentSizes.length];

		for (int i = 0; i < documentSizes.length; i++) {
			if (documentSizes[i] > 0) {
				logDocumentSizes[i] = Math.log(documentSizes[i]);
			}
		}

		return logDocumentSizes;
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

	@Override
	public void solve(TGSolutionListener<T> solutionListener) {
		// Initialization
		totalLogLikelihood = 0;
		solutionListener.beforeInitialization(maxTopics, documentSizes.length);
		createInitialTopics();

		createInitialJoinCandidates(solutionListener);

		solutionListener.initialized(solution);

		groupTopics(solutionListener);

		solutionListener.done();
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
				topicLogLikelihoods[counter] = computeOneWordTopicLogLikelihood(i);
				totalLogLikelihood += topicLogLikelihoods[counter];

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

	protected void createInitialJoinCandidates(
			TGSolutionListener<T> solutionListener) {
		int initMax = maxTopics * (maxTopics - 1) / 2;
		int initCounter = 0;

		JoinCandidate[] joinCandidates = new JoinCandidate[maxTopics];

		for (int i = 0; i < maxTopics; i++) {
			for (int j = i + 1; j < maxTopics; j++) {
				double newLikelihood = computeTwoWordLogLikelihood(i, j,
						topics[i].get(0), topics[j].get(0));

				double newImprovement = newLikelihood - topicLogLikelihoods[i]
						- topicLogLikelihoods[j];

				JoinCandidate jc = joinCandidates[i];
				if (jc == null) {
					jc = joinCandidates[i] = new JoinCandidate();
				}
				if (newImprovement > jc.improvement) {
					jc.improvement = newImprovement;
					jc.i = i;
					jc.j = j;
					jc.logLikelihood = newLikelihood;
				}

				jc = joinCandidates[j];
				if (jc == null) {
					jc = joinCandidates[j] = new JoinCandidate();
				}
				if (newImprovement > jc.improvement) {
					jc.improvement = newImprovement;
					jc.i = j;
					jc.j = i;
					jc.logLikelihood = newLikelihood;
				}

				initCounter++;
				if (initCounter % 100000 == 0) {
					solutionListener.initalizing(((double) initCounter)
							/ initMax);
				}
			}
		}
		addAllToJoinCandiates(joinCandidates);
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

	protected void groupTopics(TGSolutionListener<T> solutionListener) {
		nTopics[0] = maxTopics;
		deferredJCRecomputations = 0;
		while (nTopics[0] > minTopics) {
			// Get the best join candidate
			JoinCandidate jc = getBestJoinCandidate();
			// Check if jc is invalid
			if (jc.j == -1) {
				// Recompute the best join candidate for jc and sort it in in
				// the right place.
				updateJoinCandidateForTopic(jc);
				addJoinCandidate(jc);
				deferredJCRecomputations++;
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

	public int getDeferredJCRecomputations() {
		return deferredJCRecomputations;
	}

	protected void updateJoinCandidateForTopic(JoinCandidate jc) {
		double bestImprovement = Double.NEGATIVE_INFINITY;
		double bestLikelihood = 0;
		int bestJ = -1;
		for (int j = 0; j < maxTopics; j++) {
			if (j != jc.i && topics[j] != null) {
				double newLikelihood = computeTwoTopicLogLikelihood(jc.i, j);
				double newImprovement = newLikelihood - topicLogLikelihoods[jc.i]
						- topicLogLikelihoods[j];
				if (newImprovement > bestImprovement) {
					bestImprovement = newImprovement;
					bestLikelihood = newLikelihood;
					bestJ = j;
				}
			}
		}
		jc.improvement = bestImprovement;
		jc.logLikelihood = bestLikelihood;
		jc.j = bestJ;
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

	protected void handleJoinCandidateUpdate(JoinCandidate jc,
			JoinCandidate jc2, int j) {
		if (jc2.i == j) {
			prepareRemoveJCPartner(jc2);
		} else if (jc2 != jc
		// The following commented out optimization would require to show
		// that
		//
		// delta_h(s, t) < x and delta_h(s, w) < x ==> delta_h(s, t \cup {w}) <
		// x
		//
		// Judging by the algorithm, the criterion is not violated. But
		// proving it seems hard.
		/* && (jc2.j == jc.i || jc2.j == j || jc2.j == -1) */) {
			double newLikelihood = computeTwoTopicLogLikelihood(jc2.i, jc.i);
			double newImprovement = newLikelihood - topicLogLikelihoods[jc2.i]
					- topicLogLikelihoods[jc.i];
			if (newImprovement > jc2.improvement) {
				prepareRemoveJoinCandidate(jc2);
				jc2.improvement = newImprovement;
				jc2.logLikelihood = newLikelihood;
				jc2.j = jc.i;

				// Show me where the criterion from above is violated:
				// if (jc2.j != jc.i && jc2.j != j) {
				// System.out.println("Stop!");
				// }
				addJoinCandidateLater(jc2);
			} else if (jc2.j == jc.i || jc2.j == j) {
				jc2.j = -1;
			}
		}
	}

	protected abstract void addAllToJoinCandiates(JoinCandidate[] joinCandidates);

	protected abstract void createJoinCandidateList(int maxTopics);

	protected abstract JoinCandidate getBestJoinCandidate();

	protected abstract void addJoinCandidate(JoinCandidate jc);

	protected abstract void prepareRemoveJoinCandidate(JoinCandidate jc);

	protected abstract void prepareRemoveJCPartner(JoinCandidate jc);

	protected abstract void addJoinCandidateLater(JoinCandidate jc);

	protected abstract void updateJoinCandidates(JoinCandidate jc);

	protected abstract void iterateOverJCsForUpdate(JoinCandidate jc, int j);

	protected static class JoinCandidate implements Comparable<JoinCandidate> {
		public double logLikelihood;
		public double improvement;
		public int i;
		public int j;

		public JoinCandidate() {
			improvement = Double.NEGATIVE_INFINITY;
		}

		@Override
		public int compareTo(JoinCandidate o) {
			if (improvement == o.improvement) {
				return i - o.i;
			}
			return improvement > o.improvement ? 1 : -1;
		}

		@Override
		public String toString() {
			return "[l:" + logLikelihood + " imp:" + improvement + " i:" + i
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

	//
	// Homonym handling
	//

	protected HomonymHandler createHomonymHandler(double hEpsilon) {
		return new HomonymHandler(hEpsilon);
	}

	protected TIntCollection getHomonyms() {
		return homonymHandler == null ? null : homonymHandler.getHomonyms();
	}

	protected boolean handleHomonymicTopic(JoinCandidate jc) {
		return homonymHandler != null ? homonymHandler.handleHomonymicTopic(jc)
				: false;
	}

	protected class HomonymHandler {
		private final double hEpsilon;
		private final TIntList homonymList;

		public HomonymHandler(double hEpsilon) {
			this.hEpsilon = hEpsilon;
			homonymList = new TIntArrayList();
		}

		protected boolean handleHomonymicTopic(JoinCandidate jc) {
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

		// private int counter = 0;

		protected void toOneWordTopic(int tid, int pos) {
			int wordIndex = topics[tid].get(pos);
			// System.out.println("Homonym: " +
			// documentProvider.getWord(wordIndex));
			// if (wordIndex < 400) {
			// counter++;
			// }
			// System.out.println("Errors: " + counter);
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
