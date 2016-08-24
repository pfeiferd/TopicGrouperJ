package org.hhn.topicgrouper.report;

import gnu.trove.TIntCollection;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.List;

import org.hhn.topicgrouper.base.Solution;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.report.store.MapNode;
import org.hhn.topicgrouper.report.store.WordInfo;

public class MindMapSolutionReporter<T> implements SolutionListener<T> {

	private final List<MapNode<T>> allNodes;
	private final TIntObjectMap<MapNode<T>> currentNodes;
	private final int topWords;
	private final boolean removeWordFromChildAtPull;
	private final double markRatioLevel;
	private final double minMarkTopics;
	private final ImprovementAssessor assessor;

	public MindMapSolutionReporter(int topWords,
			boolean removeWordFromChildAtPull, double markRatioLevel,
			int minMarkTopics) {
		currentNodes = new TIntObjectHashMap<MapNode<T>>();
		this.topWords = topWords;
		this.removeWordFromChildAtPull = removeWordFromChildAtPull;
		this.markRatioLevel = markRatioLevel;
		this.minMarkTopics = minMarkTopics;
		this.assessor = new ImprovementAssessor(5);
		this.allNodes = new ArrayList<MapNode<T>>();
	}

	public TIntObjectMap<MapNode<T>> getCurrentNodes() {
		return currentNodes;
	}

	public List<MapNode<T>> getAllNodes() {
		return allNodes;
	}

	@Override
	public void initalizing(double percentage) {
	}

	@Override
	public void initialized(Solution<T> initialSolution) {
		TIntCollection[] t = initialSolution.getTopics();
		for (int i = 0; i < t.length; i++) {
			if (t[i] != null) {
				int wordId = t[i].iterator().next();

				List<WordInfo<T>> list = new ArrayList<WordInfo<T>>();
				list.add(new WordInfo<T>(wordId, initialSolution
						.getGlobalWordFrequency(wordId), initialSolution
						.getWord(wordId)));
				MapNode<T> node = new MapNode<T>(-i, null, null, list,
						initialSolution.getTotalLikelhood(), 0, 0,
						initialSolution.getTopicFrequency(i));
				currentNodes.put(i, node);
			}
		}
	}

	@Override
	public void beforeInitialization(int maxTopics, int documents) {
	}

	private Double lastImprovement = null;

	@Override
	public void updatedSolution(int newTopicIndex, int oldTopicIndex,
			double improvement, int t1Size, int t2Size, Solution<T> solution) {
		boolean mark = false;
		// if (solution.getNumberOfTopics() <= minMarkTopics
		// && lastImprovement != null) {
		// double ratio = improvement / lastImprovement;
		// if (ratio >= markRatioLevel) {
		// mark = true;
		// }
		// }
		// lastImprovement = improvement;

		Double value = assessor.addImprovement(improvement);
		if (solution.getNumberOfTopics() <= minMarkTopics && value != null) {
			if (value >= markRatioLevel) {
				mark = true;
			}
		}

		MapNode<T> child1 = currentNodes.get(newTopicIndex);
		currentNodes.remove(newTopicIndex);
		MapNode<T> child2 = currentNodes.get(oldTopicIndex);
		currentNodes.remove(oldTopicIndex);

		List<WordInfo<T>> topList = new ArrayList<WordInfo<T>>();
		List<WordInfo<T>> child1List = child1.getTopTopicWordInfos();
		List<WordInfo<T>> child2List = child2.getTopTopicWordInfos();
		if (removeWordFromChildAtPull) {
			for (int i = 0; i < topWords; i++) {
				pullFromChildren(topList, child1, child2);
			}
		} else {
			int i1 = 0, i2 = 0;
			for (int i = 0; i < topWords; i++) {
				if (i1 < child1List.size() && i2 < child2List.size()) {
					if (child1List.get(i1).getFrequency() > child2List.get(i2)
							.getFrequency()) {
						topList.add(child1List.get(i1++));
					} else {
						topList.add(child2List.get(i2++));
					}
				} else {
					if (i1 < child1List.size()) {
						topList.add(child1List.get(i1++));
					} else if (i2 < child2List.size()) {
						topList.add(child2List.get(i2++));
					}
				}
			}
		}

		MapNode<T> parent = new MapNode<T>(solution.getNumberOfTopics(),
				child1List.isEmpty() ? null : child1,
				child2List.isEmpty() ? null : child2, topList,
				solution.getTotalLikelhood(),
				solution.getTopicLikelihoods()[newTopicIndex], improvement,
				solution.getTopicFrequency(newTopicIndex));
		child1.setParent(parent);
		child2.setParent(parent);

		currentNodes.put(newTopicIndex, parent);
		allNodes.add(parent);

		if (mark) {
			child1.setMarked(true);
			child2.setMarked(true);
		}
	}

	private void pullFromChildren(List<WordInfo<T>> target, MapNode<T> child1,
			MapNode<T> child2) {
		List<WordInfo<T>> child1List = child1 == null ? null : child1
				.getTopTopicWordInfos();
		List<WordInfo<T>> child2List = child2 == null ? null : child2
				.getTopTopicWordInfos();
		if (child1List != null && !child1List.isEmpty()) {
			if (child2List != null && !child2List.isEmpty()) {
				if (child1List.get(0).getFrequency() > child2List.get(0)
						.getFrequency()) {
					doWordPull(target, child1);
				} else {
					doWordPull(target, child2);
				}
			} else {
				if (child1List != null) {
					doWordPull(target, child1);
				}
			}
		} else {
			if (child2List != null && !child2List.isEmpty()) {
				doWordPull(target, child2);
			}
		}
	}

	private void doWordPull(List<WordInfo<T>> target, MapNode<T> child) {
		List<WordInfo<T>> childList = child.getTopTopicWordInfos();
		target.add(childList.remove(0));
		pullFromChildren(child.getTopTopicWordInfos(), child.getLeftNode(),
				child.getRightNode());
	}

	@Override
	public void done() {
	}
}
