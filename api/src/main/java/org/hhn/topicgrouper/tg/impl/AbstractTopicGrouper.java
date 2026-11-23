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
import org.hhn.topicgrouper.util.ParallelRunner;
import org.hhn.topicgrouper.util.UnionFind;

/**
 * Base class of the Topic Grouper algorithm: an agglomerative clustering of the
 * vocabulary. It starts with one topic per word and repeatedly joins the two topics
 * whose union yields the highest increase of the log likelihood of the training
 * documents. Subclasses decide how join candidates are managed
 * ({@link EHACTopicGrouper}: fast but memory hungry, {@link LowMemTopicGrouper}:
 * little memory).
 * <p>
 * Besides the per-topic arrays the base class maintains, for every topic, the
 * sorted list of the documents it occurs in ({@link #topicDocs}) and the
 * document dependent part of its log likelihood ({@link #topicDocPart}), so
 * that the log likelihood of a join of two topics costs time proportional to
 * the number of documents of the smaller topic instead of the number of all
 * documents ({@link #computeTwoTopicLogLikelihood(int, int)}). Subclasses must
 * use {@link #addTopicCounts(int, int)} to merge the counts of two topics.
 * <p>
 * A solver can use several threads: {@link #runner} (available while
 * {@link #solve(TGSolutionListener)} runs) distributes index ranges over
 * worker threads that are kept ready; the join candidate updates of the
 * subclasses use it. The result does not depend on the number of threads.
 *
 * @param <T> the type of the words
 */
public abstract class AbstractTopicGrouper<T> implements TGSolver<T> {
	/** Only words occurring at least this often in the documents take part in the grouping. */
	protected final int minWordFrequency;
	/** The training documents. */
	protected final DocumentProvider<T> documentProvider;
	/** Per document: the number of occurrences of grouped words (words below the minimum frequency do not count). */
	protected final int[] documentSizes;
	/** Per document: the natural logarithm of its size in {@link #documentSizes} (0 for empty documents). */
	protected final double[] logDocumentSizes;
	/** The training documents; the position in this list is the document index. */
	protected final List<Document<T>> documents;

	/** Size of the vocabulary, including the words that are too infrequent to be grouped. */
	protected final int nWords;
	/** Number of initial (one word) topics, i.e. of grouped words; the length of the per-topic arrays. */
	protected final int maxTopics;

	/** Per topic index: the indices of the words of the topic; <code>null</code> once the topic was joined into another. */
	protected final TIntList[] topics;
	/** Union-find over the topic indices: the representative of a word's initial topic is its current topic. */
	protected final UnionFind topicUnionFind;
	/** Per word index: the index of its initial one word topic, -1 if the word is too infrequent to be grouped. */
	protected final int[] wordToInitialTopic;
	/** Per topic: the number of occurrences of all its words in the documents (0 for vanished topics). */
	protected final int[] topicSizes;
	/** The sum of {@link #topicSizes} over the current topics. */
	protected int totalSize;
	/** Per topic: its contribution to the log likelihood of the training documents (0 for vanished topics). */
	protected final double[] topicLogLikelihoods;
	/** The sum of {@link #topicLogLikelihoods} over the current topics. */
	protected double totalLogLikelihood;
	/** The current number of topics, in a one element array. */
	protected final int[] nTopics;
	/** The live view on the state that is handed to the listeners, created by {@link #createSolution()}. */
	protected final TGSolution<T> solution;
	/** Per topic and document: the number of occurrences of the topic's words in the document. */
	protected final int[][] topicFrequencyPerDocuments;
	/**
	 * Per topic: the sum of <code>f(w) * log(f(w))</code> over its words <code>w</code>, where <code>f(w)</code> is
	 * the global frequency of the word; a term of the topic log likelihood that is additive under joins.
	 */
	protected final double[] sumWordFrTimesLogWordFrByTopic;
	/**
	 * Per topic: the sorted indices of the documents the topic occurs in (a
	 * superset is allowed: entries whose count dropped to 0 are tolerated), in
	 * the first {@link #topicDocsSize} entries.
	 */
	protected final int[][] topicDocs;
	/** Per topic: the number of valid entries of {@link #topicDocs}. */
	protected final int[] topicDocsSize;
	/**
	 * Per topic: the document dependent part of its log likelihood, the sum
	 * over its documents of <code>f * (log(f) - log(size))</code> with
	 * <code>f</code> the frequency of the topic in the document; the topic log
	 * likelihood is this plus {@link #sumWordFrTimesLogWordFrByTopic} minus
	 * <code>n log n</code> for the topic frequency <code>n</code>.
	 */
	protected final double[] topicDocPart;

	/**
	 * Inverted index: for every word index the documents containing the word with the frequency of the word in
	 * the document, sorted by document index.
	 */
	protected final TIntObjectMap<List<DocIndexAndWordFr>> invertedIndex;

	/** The algorithm stops when this number of topics is reached. */
	protected final int minTopics;
	/** The number of threads used by {@link #solve(TGSolutionListener)}. */
	protected final int threads;
	/** Runs loops on the worker threads; exists while {@link #solve(TGSolutionListener)} runs. */
	protected ParallelRunner runner;

	/**
	 * Sets up the per-topic arrays and the inverted index for a single threaded
	 * solver; the initial topics are created by {@link #solve(TGSolutionListener)}.
	 *
	 * @param minWordFrequency only words occurring at least this often (&gt;= 1) in the documents are grouped
	 * @param documentProvider the training documents
	 * @param minTopics the algorithm stops when this number of topics is reached (values below 1 are treated as 1)
	 * @throws IllegalArgumentException if minWordFrequency is below 1
	 */
	public AbstractTopicGrouper(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics) {
		this(minWordFrequency, documentProvider, minTopics, 1);
	}

	/**
	 * Sets up the per-topic arrays and the inverted index; the initial topics are created by {@link #solve(TGSolutionListener)}.
	 *
	 * @param minWordFrequency only words occurring at least this often (&gt;= 1) in the documents are grouped
	 * @param documentProvider the training documents
	 * @param minTopics the algorithm stops when this number of topics is reached (values below 1 are treated as 1)
	 * @param threads the number of threads to use, including the calling one (values below 1 are treated as 1)
	 * @throws IllegalArgumentException if minWordFrequency is below 1
	 */
	public AbstractTopicGrouper(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics, int threads) {
		if (minWordFrequency < 1) {
			throw new IllegalArgumentException("minWordFrequency must be >= 1");
		}
		this.minWordFrequency = minWordFrequency;
		this.documentProvider = documentProvider;
		this.documents = documentProvider.getDocuments();
		this.documentSizes = getDocumentSizes();
		this.logDocumentSizes = getLogDocumentSizes();
		this.threads = Math.max(1, threads);

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
		topicDocs = new int[maxTopics][];
		topicDocsSize = new int[maxTopics];
		topicDocPart = new double[maxTopics];

		invertedIndex = createInvertedIndex();

		solution = createSolution();
	}

	/**
	 * The number of threads the solver uses.
	 *
	 * @return the number of threads including the calling one
	 */
	public int getThreads() {
		return threads;
	}

	/**
	 * Creates the solution object handed to listeners. Can be overridden to provide a richer solution.
	 *
	 * @return the live view on the state of this solver
	 */
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

	/**
	 * Builds {@link #invertedIndex} from the documents.
	 *
	 * @return the inverted index
	 */
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
	/**
	 * Runs the algorithm and notifies the listener about each join. The worker
	 * threads exist while this method runs.
	 */
	public void solve(TGSolutionListener<T> solutionListener) {
		runner = new ParallelRunner(threads);
		try {
			// Initialization
			totalLogLikelihood = 0;
			totalSize = 0;
			solutionListener.beforeInitialization(maxTopics, documentSizes.length);
			createInitialTopics();

			createInitialJoinCandidates(solutionListener);

			solutionListener.initialized(solution);

			groupTopics(solutionListener);

			solutionListener.done();
		} finally {
			runner.close();
			runner = null;
		}
	}

	/**
	 * Creates one topic per grouped word and initializes the per-topic arrays
	 * and the totals.
	 */
	protected void createInitialTopics() {
		nTopics[0] = maxTopics;
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
				topicDocPart[counter] = computeOneWordTopicLogLikelihood(i);
				topicLogLikelihoods[counter] = topicDocPart[counter];
				totalLogLikelihood += topicLogLikelihoods[counter];

				topicFrequencyPerDocuments[counter] = new int[documents.size()];
				List<DocIndexAndWordFr> postings = invertedIndex.get(i);
				int n = postings == null ? 0 : postings.size();
				topicDocs[counter] = new int[Math.max(1, n)];
				topicDocsSize[counter] = n;
				for (int j = 0; j < n; j++) {
					DocIndexAndWordFr e = postings.get(j);
					topicDocs[counter][j] = e.docIndex;
					topicFrequencyPerDocuments[counter][e.docIndex] = e.wordFr;
				}

				sumWordFrTimesLogWordFrByTopic[counter] = wordFr
						* Math.log(wordFr);

				counter++;
			}
		}
	}

	/**
	 * Log likelihood contribution of a topic consisting of a single word: the sum
	 * over the documents of <code>f * (log(f) - log(size))</code>, where
	 * <code>f</code> is the frequency of the word in the document and
	 * <code>size</code> the document size.
	 *
	 * @param wordIndex the word
	 * @return the log likelihood contribution
	 */
	protected double computeOneWordTopicLogLikelihood(int wordIndex) {
		double sum = 0; // Coherence weight log(1).
		List<DocIndexAndWordFr> postings = invertedIndex.get(wordIndex);
		if (postings != null) {
			for (DocIndexAndWordFr e : postings) {
				if (e.wordFr > 0 && documentSizes[e.docIndex] > 0) {
					sum += e.wordFr * (Math.log(e.wordFr) - logDocumentSizes[e.docIndex]);
				}
			}
		}
		return sum;
	}

	/**
	 * Log likelihood contribution of the topic that results from joining two one
	 * word topics (computed via the inverted index).
	 *
	 * @param i the first topic
	 * @param j the second topic
	 * @param word1 the word of the first topic
	 * @param word2 the word of the second topic
	 * @return the log likelihood contribution of the joined topic
	 */
	protected double computeTwoWordLogLikelihood(int i, int j, int word1,
			int word2) {
		double sum = computeTwoWordLogLikelihoodHelp(word1, word2);
		sum += sumWordFrTimesLogWordFrByTopic[i];
		sum += sumWordFrTimesLogWordFrByTopic[j];
		int sizeSum = topicSizes[i] + topicSizes[j];
		sum -= (sizeSum) * Math.log(sizeSum);

		return sum;
	}

	/**
	 * The document dependent part of {@link #computeTwoWordLogLikelihood(int, int, int, int)}:
	 * the sum over the documents containing at least one of the words of
	 * <code>f * (log(f) - log(size))</code>, where <code>f</code> is the joint
	 * frequency of both words in the document. The two posting lists are merged
	 * in one pass (they are sorted by document); the method has no state and may
	 * be called concurrently.
	 *
	 * @param word1 the first word
	 * @param word2 the second word
	 * @return the sum
	 */
	protected double computeTwoWordLogLikelihoodHelp(int word1, int word2) {
		double sum = 0;
		List<DocIndexAndWordFr> l1 = invertedIndex.get(word1);
		List<DocIndexAndWordFr> l2 = invertedIndex.get(word2);
		if (l1 == null || l2 == null) {
			return sum;
		}
		int n1 = l1.size();
		int n2 = l2.size();
		int p1 = 0;
		int p2 = 0;
		while (p1 < n1 || p2 < n2) {
			int d;
			int fr;
			if (p2 >= n2 || (p1 < n1 && l1.get(p1).docIndex < l2.get(p2).docIndex)) {
				DocIndexAndWordFr e = l1.get(p1++);
				d = e.docIndex;
				fr = e.wordFr;
			} else if (p1 >= n1 || l2.get(p2).docIndex < l1.get(p1).docIndex) {
				DocIndexAndWordFr e = l2.get(p2++);
				d = e.docIndex;
				fr = e.wordFr;
			} else {
				DocIndexAndWordFr e1 = l1.get(p1++);
				DocIndexAndWordFr e2 = l2.get(p2++);
				d = e1.docIndex;
				fr = e1.wordFr + e2.wordFr;
			}
			if (fr > 0 && documentSizes[d] > 0) {
				sum += fr * (Math.log(fr) - logDocumentSizes[d]);
			}
		}
		return sum;
	}

	/**
	 * Computes the join candidates of all pairs of initial topics.
	 *
	 * @param solutionListener informed about the progress via {@link TGSolutionListener#initalizing(double)}
	 */
	protected abstract void createInitialJoinCandidates(
			TGSolutionListener<T> solutionListener);

	/**
	 * The main loop: repeatedly joins the two topics with the best join candidate
	 * until {@link #minTopics} topics are left.
	 *
	 * @param solutionListener informed about every join via
	 *            {@link TGSolutionListener#updatedSolution(int, int, double, int, int, TGSolution)}
	 */
	protected abstract void groupTopics(TGSolutionListener<T> solutionListener);

	/**
	 * Log likelihood contribution of the topic that results from joining two
	 * topics. Only the documents of the topic with fewer documents are visited:
	 * with <code>a</code> and <code>b</code> the frequencies of the two topics
	 * in a document, the sum over the documents of
	 * <code>(a + b) log((a + b) / size)</code> equals the document part of the
	 * first topic plus, over the documents of the second,
	 * <code>(a + b) log((a + b) / size) - a log(a / size)</code>. The method
	 * reads shared state only and may be called concurrently for different pairs.
	 *
	 * @param topic1 the first topic
	 * @param topic2 the second topic
	 * @return the log likelihood contribution of the joined topic
	 */
	protected double computeTwoTopicLogLikelihood(int topic1, int topic2) {
		int big = topicDocsSize[topic1] >= topicDocsSize[topic2] ? topic1 : topic2;
		int small = big == topic1 ? topic2 : topic1;
		double sum = topicDocPart[big] + computeJoinDocPartDelta(big, small);

		sum += sumWordFrTimesLogWordFrByTopic[topic1];
		sum += sumWordFrTimesLogWordFrByTopic[topic2];
		int sizeSum = topicSizes[topic1] + topicSizes[topic2];
		sum -= (sizeSum) * Math.log(sizeSum);

		return sum;
	}

	/**
	 * The change of the document part of the log likelihood of
	 * <code>into</code> when the counts of <code>from</code> are added to it:
	 * the sum over the documents of <code>from</code> of
	 * <code>(a + b) log((a + b) / size) - a log(a / size)</code>.
	 *
	 * @param into the topic whose documents are the base
	 * @param from the topic whose documents are visited
	 * @return the change of the document part
	 */
	protected double computeJoinDocPartDelta(int into, int from) {
		int[] a = topicFrequencyPerDocuments[into];
		int[] b = topicFrequencyPerDocuments[from];
		int[] docs = topicDocs[from];
		int n = topicDocsSize[from];
		double sum = 0;
		for (int k = 0; k < n; k++) {
			int d = docs[k];
			int fb = b[d];
			if (fb > 0 && documentSizes[d] > 0) {
				int fa = a[d];
				int fr = fa + fb;
				double ls = logDocumentSizes[d];
				sum += fr * (Math.log(fr) - ls);
				if (fa > 0) {
					sum -= fa * (Math.log(fa) - ls);
				}
			}
		}
		return sum;
	}

	/**
	 * Adds the per document counts of topic <code>from</code> to topic
	 * <code>into</code> (the way a join merges them) and updates the document
	 * list and the document part of the log likelihood of <code>into</code>;
	 * <code>from</code> is left unchanged.
	 *
	 * @param into the receiving topic
	 * @param from the topic whose counts are added
	 */
	protected void addTopicCounts(int into, int from) {
		topicDocPart[into] += computeJoinDocPartDelta(into, from);
		int[] a = topicFrequencyPerDocuments[into];
		int[] b = topicFrequencyPerDocuments[from];
		int[] docs = topicDocs[from];
		int n = topicDocsSize[from];
		for (int k = 0; k < n; k++) {
			int d = docs[k];
			a[d] += b[d];
		}
		addTopicDocs(into, docs, n);
	}

	/**
	 * Adds documents to the document list of a topic (a sorted merge; documents
	 * already in the list are not duplicated).
	 *
	 * @param topic the topic
	 * @param docs the sorted document indices to add
	 * @param n the number of entries of <code>docs</code> to add
	 */
	protected void addTopicDocs(int topic, int[] docs, int n) {
		int[] mine = topicDocs[topic];
		int m = topicDocsSize[topic];
		int[] merged = new int[Math.max(1, m + n)];
		int p = 0;
		int q = 0;
		int c = 0;
		while (p < m || q < n) {
			if (q >= n || (p < m && mine[p] < docs[q])) {
				merged[c++] = mine[p++];
			} else if (p >= m || docs[q] < mine[p]) {
				merged[c++] = docs[q++];
			} else {
				merged[c++] = mine[p++];
				q++;
			}
		}
		topicDocs[topic] = merged;
		topicDocsSize[topic] = c;
	}

	/**
	 * Log likelihood change when the given word is moved from its current topic
	 * to the given topic.
	 *
	 * @param topic the topic the word is moved to
	 * @param wordIndex the word
	 * @return 0 if the word already belongs to the topic
	 * @throws IllegalArgumentException if the word is too infrequent to belong to a topic
	 */
	public double computeTopicWordLogLikelihood(int topic, int wordIndex) {
		int htopic = wordToInitialTopic[wordIndex];
		if (htopic == -1) {
			throw new IllegalArgumentException("word " + wordIndex
					+ " occurs less than minWordFrequency times and is not part of any topic");
		}
		int wtopic = topicUnionFind.find(htopic);

		if (wtopic == topic) {
			return 0;
		} else {
			double singleTopicLog = computeTopicWordLogLikelihoodHelp(wtopic, wordIndex, false);
			// Topic joined with word...
			double joinedTopicLog = computeTopicWordLogLikelihoodHelp(topic, wordIndex, true);
			return joinedTopicLog - singleTopicLog;
		}
	}

	/**
	 * Log likelihood contribution of a topic with a word added to or removed
	 * from it.
	 *
	 * @param topic the topic
	 * @param wordIndex the word
	 * @param add true to add the word, false to remove it
	 * @return the log likelihood contribution of the modified topic
	 */
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
		sum += sign * xLogX(frWordTotal);
		int sizeSum = topicSizes[topic] + sign * frWordTotal;
		sum -= xLogX(sizeSum);

		return sum;
	}

	/**
	 * Computes <code>x * log(x)</code> with the usual convention that the
	 * result is 0 for x = 0 (instead of NaN).
	 *
	 * @param x the argument
	 * @return x log x, 0 for x &lt;= 0
	 */
	protected static double xLogX(double x) {
		return x <= 0 ? 0 : x * Math.log(x);
	}

	/**
	 * Entry of the inverted index: a document and the frequency of a word in it (ordered by document).
	 */
	protected static class DocIndexAndWordFr implements
			Comparable<DocIndexAndWordFr> {
		/** The index of the document. */
		public int docIndex;
		/** The frequency of the word in the document. */
		public int wordFr;

		/** Creates an entry with document index and frequency 0. */
		public DocIndexAndWordFr() {
		}

		public int compareTo(DocIndexAndWordFr o) {
			return docIndex - o.docIndex;
		}
	}

	/**
	 * The live view on the state of the solver.
	 */
	public class DefaultTGSolution implements TGSolution<T> {
		/** Creates the view; it holds no state of its own. */
		public DefaultTGSolution() {
		}

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

		/**
		 * Delegates to {@link AbstractTopicGrouper#computeTopicWordLogLikelihood(int, int)}.
		 *
		 * @param topic the topic the word is moved to
		 * @param wordIndex the word
		 * @return the log likelihood change, 0 if the word already belongs to the topic
		 */
		public double computeTopicWordLogLikelihood(int topic, int wordIndex) {
			return AbstractTopicGrouper.this.computeTopicWordLogLikelihood(topic, wordIndex);
		}
	};

	/**
	 * A potential join of a topic with the topic <code>j</code>, with the resulting log likelihood and its improvement. Ordered by decreasing improvement.
	 */
	protected static class JoinCandidate implements Comparable<JoinCandidate> {
		/** The index of the partner topic. */
		public int j;
		/** The log likelihood contribution of the joined topic. */
		public double logLikelihood;
		/** The change of the total log likelihood caused by the join. */
		public double improvement;
		/** The position in the heap holding the candidate (maintained by the heap, see {@link EHACTopicGrouper}). */
		public int heapIndex;

		/** Creates a candidate with improvement negative infinity (worse than any real candidate). */
		public JoinCandidate() {
			improvement = Double.NEGATIVE_INFINITY;
		}

		/**
		 * Creates an initialized candidate.
		 *
		 * @param j the index of the partner topic
		 * @param logLikelihood the log likelihood contribution of the joined topic
		 * @param improvement the change of the total log likelihood caused by the join
		 */
		public JoinCandidate(int j, double logLikelihood, double improvement) {
			init(j, logLikelihood, improvement);
		}

		/**
		 * Sets all fields (candidates are reused to avoid garbage).
		 *
		 * @param j the index of the partner topic
		 * @param logLikelihood the log likelihood contribution of the joined topic
		 * @param improvement the change of the total log likelihood caused by the join
		 */
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

		/**
		 * Compares like {@link #compareTo(JoinCandidate)}, but breaks ties of
		 * equal improvement by the indices of the topics the candidates belong to
		 * before the partner indices.
		 *
		 * @param o the other candidate
		 * @param i the index of the topic this candidate belongs to
		 * @param oi the index of the topic the other candidate belongs to
		 * @return negative if this candidate is better (or ranks first), positive if the other does, 0 if identical
		 */
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
