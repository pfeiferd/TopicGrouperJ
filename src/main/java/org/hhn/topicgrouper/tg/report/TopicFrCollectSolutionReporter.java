package org.hhn.topicgrouper.tg.report;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;

import java.util.Arrays;

import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;

/**
 * Solution listener that collects, for every number of topics, the sorted topic frequencies.
 * @param <T> the word type
 */
public class TopicFrCollectSolutionReporter<T> implements TGSolutionListener<T> {
	/**
	 * Sorted topic frequencies indexed by number of topics minus one.
	 */
	private int[][] frequenciesPerNTopics;

	/**
	 * Creates a collecting reporter.
	 */
	public TopicFrCollectSolutionReporter() {
	}

	/**
	 * Returns the collected frequencies; entry i holds the ascending topic frequencies of the solution with i+1 topics.
	 * @return frequency arrays, null entries for unseen topic counts
	 */
	public int[][] getFrequenciesPerNTopics() {
		return frequenciesPerNTopics;
	}

	@Override
	public void beforeInitialization(int maxTopics, int documents) {
		frequenciesPerNTopics = new int[maxTopics][];
	}

	@Override
	public void initalizing(double percentage) {
	}

	@Override
	public void initialized(TGSolution<T> initialSolution) {
	}
	
	@Override
	public void done() {
	}

	@Override
	public void updatedSolution(int newTopicIndex, int oldTopicIndex,
			double improvement, int t1Size, int t2Size, TGSolution<T> solution) {
		int[] frs = new int[solution.getNumberOfTopics()];
		int i = 0;
		for (TIntCollection t : solution.getTopics()) {
			if (t != null) {
				int sum = 0;
				TIntIterator it = t.iterator();
				while (it.hasNext()) {
					sum += solution.getGlobalWordFrequency(it.next());
				}
				frs[i] = sum;
				i++;
			}
		}
		Arrays.sort(frs);
		frequenciesPerNTopics[frs.length - 1] = frs;
	}
}
