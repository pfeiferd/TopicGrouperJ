package org.hhn.topicgrouper.tg;

/**
 * A {@link TGSolutionListener} that is also told about sense splits. A split
 * does not change the number of topics, therefore
 * {@link #updatedSolution(int, int, double, int, int, TGSolution)} is only called
 * for joins.
 * 
 * @param <T>
 *            the type of the words
 */
public interface TGSenseSolutionListener<T> extends TGSolutionListener<T> {
	/**
	 * Called after the occurrences of a word in some documents were moved into
	 * another topic as a new sense.
	 * 
	 * @param topicIndex
	 *            the topic that received the new sense
	 * @param wordIndex
	 *            the word that was split (its remaining occurrences stay where they were)
	 * @param senseIndex
	 *            the word type index of the new sense
	 * @param documents
	 *            the number of documents in which the word now has the new sense
	 * @param improvement
	 *            the change of the log likelihood caused by the split and the join
	 * @param solution
	 *            the solution after the split
	 */
	public void splitSense(int topicIndex, int wordIndex, int senseIndex,
			int documents, double improvement, TGSenseSolution<T> solution);
}
