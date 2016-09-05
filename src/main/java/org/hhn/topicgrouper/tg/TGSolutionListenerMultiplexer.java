package org.hhn.topicgrouper.tg;

import java.util.ArrayList;
import java.util.List;

public class TGSolutionListenerMultiplexer<T> implements TGSolutionListener<T> {
	private final List<TGSolutionListener<T>> listeners;

	public TGSolutionListenerMultiplexer() {
		this.listeners = new ArrayList<TGSolutionListener<T>>();
	}

	public void addSolutionListener(TGSolutionListener<T> listener) {
		listeners.add(listener);
	}

	public void removeSolutionListener(TGSolutionListener<T> listener) {
		listeners.remove(listener);
	}

	@Override
	public void initalizing(double percentage) {
		for (TGSolutionListener<T> listener : listeners) {
			listener.initalizing(percentage);
		}
	}
	
	@Override
	public void beforeInitialization(int maxTopics, int documents) {
		for (TGSolutionListener<T> listener : listeners) {
			listener.beforeInitialization(maxTopics, documents);
		}
	}

	@Override
	public void initialized(TGSolution<T> initialSolution) {
		for (TGSolutionListener<T> listener : listeners) {
			listener.initialized(initialSolution);
		}
	}

	public void updatedSolution(int newTopicIndex, int oldTopicIndex,
			double improvement, int t1Size, int t2Size, TGSolution<T> solution) {
		for (TGSolutionListener<T> listener : listeners) {
			listener.updatedSolution(newTopicIndex, oldTopicIndex, improvement,
					t1Size, t2Size, solution);
		}
	}
	
	public void done() {		
		for (TGSolutionListener<T> listener : listeners) {
			listener.done();
		}
	}
}
