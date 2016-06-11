package org.hhn.topicgrouper.base;

public interface Solver<T> {
	public void solve(SolutionListener<T> solutionListener);

	public interface SolutionListener<T> {
		public void beforeInitialization(int maxTopics, int documents);

		public void initalizing(double percentage);

		public void initialized(Solution<T> initialSolution);

		public void updatedSolution(int newTopicIndex, int oldTopicIndex,
				double improvement, int t1Size, int t2Size, Solution<T> solution);
		
		public void done();
	}
}
