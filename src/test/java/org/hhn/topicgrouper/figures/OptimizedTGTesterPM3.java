package org.hhn.topicgrouper.figures;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import javax.swing.UIManager;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.eval.AbstractTGTester;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.report.BasicSolutionReporter;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;
import org.hhn.topicgrouper.validation.HoldOutSplitter;

public class OptimizedTGTesterPM3 extends AbstractTGTester<String> {
	private DocumentProvider<String> testDocumentProvider;

	public OptimizedTGTesterPM3(File outputFile) throws IOException {
		super(outputFile);
	}

	protected SolutionListener<String> createSolutionListener(PrintStream out) {
		BasicSolutionReporter<String> res = new BasicSolutionReporter<String>(
				out, 4, true);
		res.setTestDocumentProvider(testDocumentProvider);
		return res;
	}

	protected DocumentProvider<String> createDocumentProvider() {
		DocumentProvider<String> provider = new TWCLDAPaperDocumentGenerator();
		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), provider, 0.1, 0);
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
		new OptimizedTGTesterPM3(null)
				.run();
	}
}
