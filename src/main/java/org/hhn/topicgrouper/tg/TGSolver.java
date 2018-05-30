package org.hhn.topicgrouper.tg;

public interface TGSolver<T> {
	public void solve(TGSolutionListener<T> solutionListener);
}
