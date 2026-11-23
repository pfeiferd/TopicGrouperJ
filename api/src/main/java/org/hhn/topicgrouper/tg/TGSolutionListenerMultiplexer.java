package org.hhn.topicgrouper.tg;

import java.util.ArrayList;
import java.util.List;

/**
 * Forwards all events to a list of registered listeners. Sense splits (see
 * {@link TGSenseSolutionListener}) are forwarded to the listeners that
 * implement {@link TGSenseSolutionListener}.
 *
 * @param <T> the type of the words
 */
public class TGSolutionListenerMultiplexer<T> implements TGSenseSolutionListener<T> {
	private final List<TGSolutionListener<T>> listeners;

	/** Creates a multiplexer without listeners. */
	public TGSolutionListenerMultiplexer() {
		this.listeners = new ArrayList<TGSolutionListener<T>>();
	}

	/**
	 * Registers a listener; the listeners are notified in the order of registration.
	 *
	 * @param listener the listener
	 */
	public void addSolutionListener(TGSolutionListener<T> listener) {
		listeners.add(listener);
	}

	/**
	 * Unregisters a listener.
	 *
	 * @param listener the listener
	 */
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

	@Override
	public void updatedSolution(int newTopicIndex, int oldTopicIndex,
			double improvement, int t1Size, int t2Size, TGSolution<T> solution) {
		for (TGSolutionListener<T> listener : listeners) {
			listener.updatedSolution(newTopicIndex, oldTopicIndex, improvement,
					t1Size, t2Size, solution);
		}
	}

	@Override
	public void splitSense(int topicIndex, int wordIndex, int senseIndex, int documents, double improvement,
			TGSenseSolution<T> solution) {
		for (TGSolutionListener<T> listener : listeners) {
			if (listener instanceof TGSenseSolutionListener) {
				((TGSenseSolutionListener<T>) listener).splitSense(topicIndex, wordIndex, senseIndex, documents,
						improvement, solution);
			}
		}
	}

	@Override
	public void done() {
		for (TGSolutionListener<T> listener : listeners) {
			listener.done();
		}
	}
}
