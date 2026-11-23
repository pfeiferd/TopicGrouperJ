package org.hhn.topicgrouper.tg.report;

import java.io.PrintStream;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.validation.TGPerplexityCalculator;

/**
 * Solution listener that writes one semicolon-separated line per solution update (number of topics, optional perplexity, improvement, log likelihood).
 * @param <T> the word type
 */
public class CSVSolutionReporter<T> implements TGSolutionListener<T> {
	private final PrintStream pw;
	private final TGPerplexityCalculator<T> perplexityCalculator;
	private DocumentProvider<T> testDocumentProvider;

	/**
	 * Creates a CSV reporter.
	 * @param pw stream to write to
	 * @param bowFactor whether the perplexity calculator applies the bag-of-words factor
	 */
	public CSVSolutionReporter(PrintStream pw, boolean bowFactor) {
		this.pw = pw;
		this.perplexityCalculator = new TGPerplexityCalculator<T>(bowFactor,
				new DefaultDocumentSplitter<T>(), 1);
	}

	/**
	 * Sets the held-out documents used to compute a perplexity column.
	 * @param documentProvider test documents, or null to omit perplexity
	 */
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
			double improvement, int t1Size, int t2Size, TGSolution<T> solution) {
		pw.print(solution.getNumberOfTopics());
		pw.print("; ");
		if (testDocumentProvider != null) {
			perplexityCalculator.setSolution(solution);
			double p = perplexityCalculator.computePerplexity(
					testDocumentProvider);
			pw.print(p);
			pw.print("; ");
		}
		pw.print(improvement);
		pw.print("; ");
		pw.println(solution.getTotalLogLikelhood());
	}

	@Override
	public void initalizing(double percentage) {
		pw.print((int) (percentage * 100));
		pw.print("% ");
	}

	@Override
	public void initialized(TGSolution<T> initialSolution) {
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
