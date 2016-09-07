package org.hhn.topicgrouper.demo;

import java.util.HashMap;
import java.util.Map;

import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;

import external.com.apporiented.algorithm.clustering.Cluster;
import external.com.apporiented.algorithm.clustering.Distance;
import gnu.trove.TIntCollection;

public class ClusterSolutionReporter<T> implements TGSolutionListener<T> {
	private final Map<Integer, Cluster> currentClusters;

	public ClusterSolutionReporter() {
		currentClusters = new HashMap<Integer, Cluster>();
	}

	public Cluster getRootCluster() {
		if (currentClusters.size() == 1) {
			return currentClusters.values().iterator().next();
		} else {
			Cluster rootCluster = new Cluster("root");
			double maxDistance = 0;
			double sum = 0;
			for (Cluster c : currentClusters.values()) {
				rootCluster.addChild(c);
				c.setParent(rootCluster);
				maxDistance = Math.max(maxDistance,
						Math.abs(c.getDistanceValue()));
				sum += c.getDistanceValue();
			}
			rootCluster.setDistance(new Distance((sum > 0 ? 1 : -1)
					* maxDistance));
			return rootCluster;
		}
	}
	
	@Override
	public void beforeInitialization(int maxTopics, int documents) {
	}

	@Override
	public void initalizing(double percentage) {
	}
	
	@Override
	public void done() {
	}

	@Override
	public void initialized(TGSolution<T> initialSolution) {
		TIntCollection[] t = initialSolution.getTopics();
		for (int i = 0; i < t.length; i++) {
			Cluster cluster = new Cluster(initialSolution.getWord(
					t[i].iterator().next()).toString());
			currentClusters.put(i, cluster);
		}
	}

	@Override
	public void updatedSolution(int newTopicIndex, int oldTopicIndex,
			double improvement, int t1Size, int t2Size, TGSolution<T> solution) {
		Cluster parent = new Cluster(String.valueOf(newTopicIndex));
		Cluster child1 = currentClusters.get(newTopicIndex);
		currentClusters.remove(newTopicIndex);
		Cluster child2 = currentClusters.get(oldTopicIndex);
		currentClusters.remove(oldTopicIndex);

		parent.addChild(child1);
		parent.addChild(child2);
		parent.setDistance(new Distance(computeDistance(newTopicIndex,
				oldTopicIndex, improvement, t1Size, t2Size, solution)));

		child1.setParent(parent);
		child2.setParent(parent);
		currentClusters.put(newTopicIndex, parent);
	}

	public double computeDistance(int newTopicIndex, int oldTopicIndex,
			double improvement, int t1Size, int t2Size, TGSolution<T> solution) {
		return Math.log(- improvement);
	}
}
