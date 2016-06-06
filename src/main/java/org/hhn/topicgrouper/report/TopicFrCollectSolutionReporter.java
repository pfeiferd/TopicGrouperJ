package org.hhn.topicgrouper.report;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;

import java.util.Arrays;

import org.hhn.topicgrouper.base.Solution;
import org.hhn.topicgrouper.base.Solver.SolutionListener;

public class TopicFrCollectSolutionReporter<T> implements SolutionListener<T> {
	private int[][] frequenciesPerNTopics;

	public TopicFrCollectSolutionReporter() {
	}

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
	public void initialized(Solution<T> initialSolution) {
	}

	@Override
	public void updatedSolution(int newTopicIndex, int oldTopicIndex,
			double improvement, int t1Size, int t2Size, Solution<T> solution) {
		int[] frs = new int[solution.getNumberOfTopics()];
		int i = 0;
		for (TIntCollection t : solution.getTopicsAlt()) {
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
