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
	 */
	public int getNumberOfTopics();
	
	/**
	 * The words of every topic, indexed by topic index. Entries of vanished topics are <code>null</code>.
	 */
	public TIntCollection[] getTopics();
	
	/**
	 * The indices of the topics that are currently in use.
	 */
	public int[] getTopicIds();
		
	/**
	 * The number of occurrences of all words of the topic.
	 */
	public int getTopicFrequency(int topicIndex);
	
	/**
	 * The total number of word occurrences covered by the topics.
	 */
	public int getSize();
	
	/**
	 * The index of the topic containing the word or -1 if the word is not covered (too infrequent).
	 */
	public int getTopicForWord(int wordIndex);
	
	/**
	 * The number of occurrences of the word in all documents.
	 */
	public int getGlobalWordFrequency(int wordIndex);
		
	/**
	 * The log likelihood of the training documents under the current solution.
	 */
	public double getTotalLogLikelhood();
	
	/**
	 * The vocabulary of the training documents.
	 */
	public Vocab<T> getVocab();
		
	/**
	 * The log likelihood contribution of every topic, indexed by topic index.
	 */
	public double[] getTopicLogLikelihoods();
}
