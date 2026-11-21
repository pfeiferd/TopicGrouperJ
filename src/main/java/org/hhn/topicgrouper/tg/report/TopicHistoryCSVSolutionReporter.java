package org.hhn.topicgrouper.tg.report;

import gnu.trove.TIntCollection;

import java.io.PrintStream;

import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;

/**
 * Solution listener that writes a CSV line for each newly merged topic, including its size, frequency, likelihood data and words.
 * @param <T> the word type
 */
public class TopicHistoryCSVSolutionReporter<T> implements TGSolutionListener<T> {
	/**
	 * Stream to write to.
	 */
	private final PrintStream pw;
	/**
	 * Minimum size of a merged topic for it to be reported.
	 */
	private final int reportDetailsAtTopicSize;

	/**
	 * Creates a reporter.
	 * @param pw stream to write to
	 * @param reportDetailsAtTopicSize only topics with at least this many words are reported
	 */
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
			double improvement, int t1Size, int t2Size, TGSolution<T> solution) {
		TIntCollection topic = solution.getTopics()[newTopicIndex];
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
			pw.print(solution.getTotalLogLikelhood());
			pw.print("; ");
			pw.print(improvement);
			pw.print("; ");
			pw.print(ratio);
			pw.print("; ");
			BasicTGSolutionReporter.printTopicDetails(solution, topic, pw);
		}
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

}
