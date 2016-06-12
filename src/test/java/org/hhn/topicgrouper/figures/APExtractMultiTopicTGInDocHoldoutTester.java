package org.hhn.topicgrouper.figures;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import javax.swing.UIManager;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.report.CSVSolutionReporter;
import org.hhn.topicgrouper.test.AbstractTGTester;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;
import org.hhn.topicgrouper.validation.InDocumentHoldOutSplitter;

public class APExtractMultiTopicTGInDocHoldoutTester extends
		AbstractTGTester<String> {
	private DocumentProvider<String> testDocumentProvider;

	public APExtractMultiTopicTGInDocHoldoutTester(File outputFile)
			throws IOException {
		super(outputFile);
	}

	protected SolutionListener<String> createSolutionListener(PrintStream out) {
		CSVSolutionReporter<String> res = new CSVSolutionReporter<String>(out);
		res.setTestDocumentProvider(testDocumentProvider);
		return res;
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		DocumentProvider<String> documentProvider = new APParser(true)
				.getCorpusDocumentProvider(new File(
						"src/test/resources/ap-corpus/extract/ap.txt"));
		InDocumentHoldOutSplitter<String> splitter = new InDocumentHoldOutSplitter<String>(
				new Random(42), documentProvider, 0.1, 10);
		testDocumentProvider = splitter.getHoldOut();
		return splitter.getRest();
	}

	@Override
	protected Solver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		return new OptimizedTopicGrouper<String>(1, 0, documentProvider, 1);
	}

	public static void main(String[] args) throws IOException {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		new APExtractMultiTopicTGInDocHoldoutTester(new File(
				"target/APExtractMultiTopicTGInDocHoldoutTester.csv")).run();
	}
}
