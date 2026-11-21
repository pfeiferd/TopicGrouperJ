package org.hhn.topicgrouper.tg;

/**
 * Observes the progress of a {@link TGSolver}.
 *
 * @param <T> the type of the words
 */
public interface TGSolutionListener<T> {
	/**
	 * Called before the initial (one word per topic) solution is set up.
	 */
	public void beforeInitialization(int maxTopics, int documents);

	/**
	 * @param percentage the share of the initialization work done so far (0 to 1).
	 */
	public void initalizing(double percentage);

	/**
	 * Called once the initial solution with one topic per word is available.
	 */
	public void initialized(TGSolution<T> initialSolution);

	/**
	 * Called after two topics were joined.
	 *
	 * @param newTopicIndex index of the topic that received the words of the other topic
	 * @param oldTopicIndex index of the topic that vanished
	 * @param improvement change of the log likelihood caused by the join
	 * @param t1Size frequency of the topic at <code>newTopicIndex</code> before the join
	 * @param t2Size frequency of the topic at <code>oldTopicIndex</code> before the join
	 * @param solution the solution after the join (a live view - it changes with the next join)
	 */
	public void updatedSolution(int newTopicIndex, int oldTopicIndex,
			double improvement, int t1Size, int t2Size, TGSolution<T> solution);
	
	/**
	 * Called after the last join.
	 */
	public void done();
}