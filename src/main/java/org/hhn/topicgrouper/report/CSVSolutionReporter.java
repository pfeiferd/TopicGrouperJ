package org.hhn.topicgrouper.report;

import java.io.PrintStream;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solution;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.validation.PerplexityCalculator;

public class CSVSolutionReporter<T> implements SolutionListener<T> {
	private final PrintStream pw;
	private final PerplexityCalculator<T> perplexityCalculator;
	private DocumentProvider<T> testDocumentProvider;

	public CSVSolutionReporter(PrintStream pw) {
		this.pw = pw;
		this.perplexityCalculator = new PerplexityCalculator<T>();
	}

	public void setTestDocumentProvider(DocumentProvider<T> documentProvider) {
		this.testDocumentProvider = documentProvider;
	}

	@Override
	public void beforeInitialization(int maxTopics, int documents) {
		pw.println("Processed words / max topics: " + maxTopics);
		pw.println("Processed documents: " + documents);
		pw.println("Initializating... ");
	}


	@Override
	public void updatedSolution(int newTopicIndex, int oldTopicIndex,
			double improvement, int t1Size, int t2Size, Solution<T> solution) {
		pw.print(solution.getNumberOfTopics());
		pw.print("; ");
		if (testDocumentProvider != null) {
			double p = perplexityCalculator.computePerplexity(
					testDocumentProvider, solution);
			pw.print(p);
			pw.print("; ");
		}
		pw.print(improvement);
		pw.print("; ");
		pw.println(solution.getTotalLikelhood());
	}

	@Override
	public void initalizing(double percentage) {
		pw.print((int) (percentage * 100));
		pw.print("% ");
	}

	@Override
	public void initialized(Solution<T> initialSolution) {
		pw.println();
		pw.println();
		pw.print("Number of Topics; ");
		if (testDocumentProvider != null) {
			pw.print("Perplexity; ");
		}
		pw.print("Log Improvement; ");
		pw.println("Log Likelihood");
	}

	@Override
	public void done() {
	}
}
