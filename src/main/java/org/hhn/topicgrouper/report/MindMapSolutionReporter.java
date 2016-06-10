package org.hhn.topicgrouper.report;

import gnu.trove.TIntCollection;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.List;

import org.hhn.topicgrouper.base.Solution;
import org.hhn.topicgrouper.base.Solver.SolutionListener;

public class MindMapSolutionReporter<T> implements SolutionListener<T> {
	private final TIntObjectMap<MapNode<T>> currentNodes;
	private final int topWords;
	private final boolean removeWordFromChildAtPull;
	private final double markRatioLevel;
	private final double minMarkTopics;

	public MindMapSolutionReporter(int topWords,
			boolean removeWordFromChildAtPull, double markRatioLevel,
			int minMarkTopics) {
		currentNodes = new TIntObjectHashMap<MindMapSolutionReporter.MapNode<T>>();
		this.topWords = topWords;
		this.removeWordFromChildAtPull = removeWordFromChildAtPull;
		this.markRatioLevel = markRatioLevel;
		this.minMarkTopics = minMarkTopics;
	}

	public TIntObjectMap<MapNode<T>> getCurrentNodes() {
		return currentNodes;
	}

	@Override
	public void initalizing(double percentage) {
	}

	@Override
	public void initialized(Solution<T> initialSolution) {
		TIntCollection[] t = initialSolution.getTopicsAlt();
		for (int i = 0; i < t.length; i++) {
			if (t[i] != null) {
				int wordId = t[i].iterator().next();

				List<WordInfo<T>> list = new ArrayList<WordInfo<T>>();
				list.add(new WordInfo<T>(wordId, initialSolution
						.getGlobalWordFrequency(wordId), initialSolution
						.getWord(wordId)));
				MapNode<T> node = new MapNode<T>(null, null, list);
				currentNodes.put(i, node);
			}
		}
	}
	
	@Override
	public void beforeInitialization(int maxTopics, int documents) {
	}

	private Double lastImprovement = Double.NaN;

	@Override
	public void updatedSolution(int newTopicIndex, int oldTopicIndex,
			double improvement, int t1Size, int t2Size, Solution<T> solution) {
		boolean mark = false;
		if (solution.getNumberOfTopics() <= minMarkTopics
				&& lastImprovement != Double.NaN) {
			double ratio = improvement / lastImprovement;
			if (ratio >= markRatioLevel) {
				mark = true;
			}
		}
		lastImprovement = improvement;

		MapNode<T> child1 = currentNodes.get(newTopicIndex);
		currentNodes.remove(newTopicIndex);
		MapNode<T> child2 = currentNodes.get(oldTopicIndex);
		currentNodes.remove(oldTopicIndex);

		List<WordInfo<T>> topList = new ArrayList<MindMapSolutionReporter.WordInfo<T>>();
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

		MapNode<T> parent = new MapNode<T>(child1List.isEmpty() ? null : child1,
				child2List.isEmpty() ? null : child2, topList);

		currentNodes.put(newTopicIndex, parent);

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

	public static class MapNode<T> {
		private List<WordInfo<T>> topTopicWordIds;

		private final MapNode<T> leftNode;
		private final MapNode<T> rightNode;
		private boolean marked;

		public MapNode(MapNode<T> leftNode, MapNode<T> rightNode,
				List<WordInfo<T>> topTopicWordIds) {
			this.leftNode = leftNode;
			this.rightNode = rightNode;
			this.topTopicWordIds = topTopicWordIds;
			this.marked = false;
		}

		public MapNode<T> getLeftNode() {
			return leftNode;
		}

		public MapNode<T> getRightNode() {
			return rightNode;
		}

		public List<WordInfo<T>> getTopTopicWordInfos() {
			return topTopicWordIds;
		}

		public boolean isMarked() {
			return marked;
		}

		public void setMarked(boolean marked) {
			this.marked = marked;
		}
	}

	public static class WordInfo<T> implements Comparable<WordInfo<T>> {
		private final int wordId;
		private final int frequency;
		private final T word;

		public WordInfo(int wordId, int frequency, T word) {
			this.word = word;
			this.wordId = wordId;
			this.frequency = frequency;
		}

		@Override
		public int compareTo(WordInfo<T> o) {
			return frequency < o.frequency ? 1 : (frequency == o.frequency ? 0
					: -1);
		}

		public T getWord() {
			return word;
		}

		public int getWordId() {
			return wordId;
		}

		public int getFrequency() {
			return frequency;
		}
	}
	
	@Override
	public void done() {
	}
}
