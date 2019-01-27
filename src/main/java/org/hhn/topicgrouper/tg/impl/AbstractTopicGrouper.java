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
import org.hhn.topicgrouper.doc.DocumentProvider.Vocab;
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
	protected int totalSize;
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

	public AbstractTopicGrouper(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics) {
		this.minWordFrequency = minWordFrequency;
		this.documentProvider = documentProvider;
		this.documents = documentProvider.getDocuments();
		this.documentSizes = getDocumentSizes();
		this.logDocumentSizes = getLogDocumentSizes();

		if (minWordFrequency < 1) {
			throw new IllegalArgumentException("minWordFrequency must be >= 1");
		}

		this.minTopics = Math.max(1, minTopics);
		nWords = documentProvider.getVocab().getNumberOfWords();

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
		topicLogLikelihoods = new double[maxTopics];
		nTopics = new int[1];

		topicFrequencyPerDocuments = new int[maxTopics][];
		sumWordFrTimesLogWordFrByTopic = new double[maxTopics];

		invertedIndex = createInvertedIndex();

		solution = createSolution();
	}

	protected TGSolution<T> createSolution() {
		return new DefaultTGSolution();
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
				totalSize += topicSizes[counter];
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

	protected abstract void createInitialJoinCandidates(
			TGSolutionListener<T> solutionListener);

	protected abstract void groupTopics(TGSolutionListener<T> solutionListener);

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

	public double computeTopicWordLogLikelihood(int topic, int wordIndex) {
		int htopic = wordToInitialTopic[wordIndex];
		int wtopic = topicUnionFind.find(htopic);

		double joinedTopicLog;
		double singleTopicLog;
		double wordLog = computeOneWordTopicLogLikelihood(wordIndex);
		if (wtopic == topic) {
			return 0;
		} else {
			singleTopicLog = computeTopicWordLogLikelihoodHelp(wtopic, wordIndex, false);
			joinedTopicLog = computeTopicWordLogLikelihoodHelp(topic, wordIndex, true); // Topic joined with word...
			return joinedTopicLog - singleTopicLog;
		}

		//return joinedTopicLog - singleTopicLog - wordLog;
	}
	
	protected double computeTopicWordLogLikelihoodHelp(int topic, int wordIndex, boolean add) {
		double sum = 0;
		int sign = add ? 1 : -1;
		int[] frTopicPerDocument1 = topicFrequencyPerDocuments[topic];
		int frWordTotal = 0;
		for (int i = 0; i < documents.size(); i++) {
			int fr2 = documents.get(i).getWordFrequency(wordIndex);
			if ((frTopicPerDocument1[i] > 0 || fr2 > 0) && documentSizes[i] > 0) {
				int fr = frTopicPerDocument1[i] + sign * fr2;
				if (fr > 0) {
					sum += fr * (Math.log(fr) - logDocumentSizes[i]);
				}
			}
			frWordTotal += fr2;
		}

		sum += sumWordFrTimesLogWordFrByTopic[topic];
		sum += sign * frWordTotal * Math.log(frWordTotal);
		int sizeSum = topicSizes[topic] + sign * frWordTotal;
		sum -= (sizeSum) * Math.log(sizeSum);

		return sum;		
	}

	protected static class DocIndexAndWordFr implements
			Comparable<DocIndexAndWordFr> {
		public int docIndex;
		public int wordFr;

		public int compareTo(DocIndexAndWordFr o) {
			return docIndex - o.docIndex;
		}
	}

	public class DefaultTGSolution implements TGSolution<T> {
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
		public Vocab<T> getVocab() {
			return documentProvider.getVocab();
		}

		@Override
		public int getGlobalWordFrequency(int wordIndex) {
			return documentProvider.getWordFrequency(wordIndex);
		}

		@Override
		public int getTopicFrequency(int topicIndex) {
			return topicSizes[topicIndex];
		}

		@Override
		public int getSize() {
			return totalSize;
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
		public int getNumberOfTopics() {
			return nTopics[0];
		}
		
		public double computeTopicWordLogLikelihood(int topic, int wordIndex) {
			return AbstractTopicGrouper.this.computeTopicWordLogLikelihood(topic, wordIndex);
		}
	};

	protected static class JoinCandidate implements Comparable<JoinCandidate> {
		public int j;
		public double logLikelihood;
		public double improvement;

		public JoinCandidate() {
			improvement = Double.NEGATIVE_INFINITY;
		}

		public JoinCandidate(int j, double logLikelihood, double improvement) {
			init(j, logLikelihood, improvement);
		}

		public void init(int j, double logLikelihood, double improvement) {
			this.j = j;
			this.logLikelihood = logLikelihood;
			this.improvement = improvement;
		}

		@Override
		public int compareTo(JoinCandidate o) {
			if (this == o) {
				return 0;
			}
			if (improvement == o.improvement) {
				return j - o.j;
			}
			return improvement > o.improvement ? -1 : 1;
		}

		public int compareTo(JoinCandidate o, int i, int oi) {
			if (this == o) {
				return 0;
			}
			if (improvement == o.improvement) {
				if (i == oi) {
					return j - o.j;
				}
				return i - oi;
			}
			return improvement > o.improvement ? -1 : 1;
		}

		@Override
		public String toString() {
			return "[l:" + logLikelihood + " imp:" + improvement /* + " i:" + i */
					+ " j:" + j + "]";
		}
	}
}
