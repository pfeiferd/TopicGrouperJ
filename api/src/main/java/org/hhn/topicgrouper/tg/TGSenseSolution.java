package org.hhn.topicgrouper.tg;

/**
 * A {@link TGSolution} in which a word may have several <em>senses</em>: the
 * occurrences of a word in different documents may belong to different topics
 * (see <code>doc/topic-grouper.md</code>, section "Homonyms").
 * <p>
 * The elements of the topics returned by {@link #getTopics()} are <em>word
 * types</em>. A type is either an ordinary word (index below the size of the
 * original vocabulary) holding the occurrences that were not assigned to another
 * topic, or a sense created by a split (index at or above that size). Use
 * {@link #getOriginalWord(int)} to get the word of a type. {@link #getVocab()}
 * is the original vocabulary, it does not know the senses.
 * {@link #getTopicForWord(int)} and {@link #getGlobalWordFrequency(int)} accept
 * types, too; for an ordinary word index they refer to the remaining occurrences.
 * 
 * @param <T>
 *            the type of the words
 */
public interface TGSenseSolution<T> extends TGSolution<T> {
	/**
	 * The number of word types (the valid indices of the topic elements are below it).
	 *
	 * @return the number of word types, i.e. the original words plus all senses split off so far
	 */
	public int getNumberOfWordTypes();

	/**
	 * Maps a word type to its word.
	 *
	 * @param type
	 *            a word type
	 * @return the index of the word in the original vocabulary the type stems from
	 */
	public int getOriginalWord(int type);

	/**
	 * The share of the word's occurrences that a type holds.
	 *
	 * @param type
	 *            a word type
	 * @return the share (0..1) of the occurrences of the original word that belongs to this type
	 */
	public double getSenseShare(int type);

	/**
	 * The documents of a sense.
	 *
	 * @param type
	 *            a word type
	 * @return the indices of the (training) documents whose occurrences of the word were assigned
	 *         to this sense; <code>null</code> for an ordinary word type, empty for a
	 *         sense that has become one with another type of the word again
	 */
	public int[] getSenseDocuments(int type);

	/**
	 * The number of senses created.
	 *
	 * @return the number of splits performed so far
	 */
	public int getNumberOfSplits();
}
