package org.hhn.topicgrouper.base;

import java.util.ArrayList;
import java.util.List;

import org.hhn.topicgrouper.base.Solver.SolutionListener;

public class SolutionListenerMultiplexer<T> implements SolutionListener<T> {
	private final List<SolutionListener<T>> listeners;

	public SolutionListenerMultiplexer() {
		this.listeners = new ArrayList<SolutionListener<T>>();
	}

	public void addSolutionListener(SolutionListener<T> listener) {
		listeners.add(listener);
	}

	public void removeSolutionListener(SolutionListener<T> listener) {
		listeners.remove(listener);
	}

	@Override
	public void initalizing(double percentage) {
		for (SolutionListener<T> listener : listeners) {
			listener.initalizing(percentage);
		}
	}
	
	@Override
	public void beforeInitialization(int maxTopics, int documents) {
		for (SolutionListener<T> listener : listeners) {
			listener.beforeInitialization(maxTopics, documents);
		}
	}

	@Override
	public void initialized(Solution<T> initialSolution) {
		for (SolutionListener<T> listener : listeners) {
			listener.initialized(initialSolution);
		}
	}

	public void updatedSolution(int newTopicIndex, int oldTopicIndex,
			double improvement, int t1Size, int t2Size, Solution<T> solution) {
		for (SolutionListener<T> listener : listeners) {
			listener.updatedSolution(newTopicIndex, oldTopicIndex, improvement,
					t1Size, t2Size, solution);
		}
	}
}
