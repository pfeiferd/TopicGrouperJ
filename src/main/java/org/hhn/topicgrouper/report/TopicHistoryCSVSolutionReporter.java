package org.hhn.topicgrouper.report;

import gnu.trove.TIntCollection;

import java.io.PrintStream;

import org.hhn.topicgrouper.base.Solution;
import org.hhn.topicgrouper.base.Solver.SolutionListener;

public class TopicHistoryCSVSolutionReporter<T> implements SolutionListener<T> {
	private final PrintStream pw;
	private final int reportDetailsAtTopicSize;

	public TopicHistoryCSVSolutionReporter(PrintStream pw,
			int reportDetailsAtTopicSize) {
		this.pw = pw;
		this.reportDetailsAtTopicSize = reportDetailsAtTopicSize;
	}

	@Override
	public void beforeInitialization(int maxTopics, int documents) {
		pw.println("Processed words / max topics: " + maxTopics);
		pw.println("Processed documents: " + documents);
		pw.println("number of topics; topic size; topic frequency; total log likelihood; log likelihood delta; delta ratio; topic details");
	}

	private Double lastImprovement = null;

	@Override
	public void updatedSolution(int newTopicIndex, int oldTopicIndex,
			double improvement, int t1Size, int t2Size, Solution<T> solution) {
		TIntCollection topic = solution.getTopicsAlt()[newTopicIndex];
		double ratio = 0;
		if (lastImprovement != null && lastImprovement != 0) {
			ratio = improvement / lastImprovement;
		}
		lastImprovement = improvement;
		if (topic.size() >= reportDetailsAtTopicSize) {
			pw.print(solution.getNumberOfTopics());
			pw.print("; ");
			pw.print(topic.size());
			pw.print("; ");
			pw.print(t1Size + t2Size);
			pw.print("; ");
			pw.print(solution.getTotalLikelhood());
			pw.print("; ");
			pw.print(improvement);
			pw.print("; ");
			pw.print(ratio);
			pw.print("; ");
			BasicSolutionReporter.printTopicDetails(solution, topic, pw);
		}
	}

	@Override
	public void initalizing(double percentage) {
	}

	@Override
	public void initialized(Solution<T> initialSolution) {
	}

	@Override
	public void done() {
	}

}
