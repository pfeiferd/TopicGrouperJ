package org.hhn.topicgrouper.tg;

import gnu.trove.TIntCollection;

import org.hhn.topicgrouper.doc.DocumentProvider.Vocab;

/**
 * A partitioning of the (frequent enough) vocabulary into topics.
 * Topics are identified by an index; after joins only some indices are still in use
 * (see {@link #getTopicIds()}).
 *
 * @param <T> the type of the words
 */
public interface TGSolution<T> {
	/**
	 * The number of topics currently in the solution.
	 *
	 * @return the number of topics
	 */
	public int getNumberOfTopics();

	/**
	 * The words of every topic, indexed by topic index. Entries of vanished topics are <code>null</code>.
	 *
	 * @return the word indices per topic index
	 */
	public TIntCollection[] getTopics();

	/**
	 * The indices of the topics that are currently in use.
	 *
	 * @return the topic indices
	 */
	public int[] getTopicIds();

	/**
	 * The number of occurrences of all words of the topic.
	 *
	 * @param topicIndex the topic
	 * @return the frequency of the topic
	 */
	public int getTopicFrequency(int topicIndex);

	/**
	 * The total number of word occurrences covered by the topics.
	 *
	 * @return the sum of the topic frequencies
	 */
	public int getSize();

	/**
	 * The index of the topic containing the word or -1 if the word is not covered (too infrequent).
	 *
	 * @param wordIndex the word
	 * @return the topic index or -1
	 */
	public int getTopicForWord(int wordIndex);

	/**
	 * The number of occurrences of the word in all documents.
	 *
	 * @param wordIndex the word
	 * @return the frequency of the word
	 */
	public int getGlobalWordFrequency(int wordIndex);

	/**
	 * The log likelihood of the training documents under the current solution.
	 *
	 * @return the log likelihood
	 */
	public double getTotalLogLikelhood();

	/**
	 * The vocabulary of the training documents.
	 *
	 * @return the vocabulary
	 */
	public Vocab<T> getVocab();

	/**
	 * The log likelihood contribution of every topic, indexed by topic index.
	 *
	 * @return the log likelihood contributions per topic index
	 */
	public double[] getTopicLogLikelihoods();
}
