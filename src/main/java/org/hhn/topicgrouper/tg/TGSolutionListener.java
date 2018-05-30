package org.hhn.topicgrouper.tg;

public interface TGSolutionListener<T> {
	public void beforeInitialization(int maxTopics, int documents);

	public void initalizing(double percentage);

	public void initialized(TGSolution<T> initialSolution);

	public void updatedSolution(int newTopicIndex, int oldTopicIndex,
			double improvement, int t1Size, int t2Size, TGSolution<T> solution);
	
	public void done();
}