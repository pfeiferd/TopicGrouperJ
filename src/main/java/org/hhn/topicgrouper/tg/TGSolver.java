package org.hhn.topicgrouper.tg;

/**
 * Solves the Topic Grouper optimization problem.
 *
 * @param <T> the type of the words
 */
public interface TGSolver<T> {
	/**
	 * Runs the algorithm. The listener is notified about every step, i.e. about
	 * the solution for every number of topics between the number of words and the
	 * minimum number of topics.
	 */
	public void solve(TGSolutionListener<T> solutionListener);
}
