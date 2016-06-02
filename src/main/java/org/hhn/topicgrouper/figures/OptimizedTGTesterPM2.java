package org.hhn.topicgrouper.figures;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import javax.swing.UIManager;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.InDocumentHoldOutSplitter;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.report.BasicSolutionReporter;
import org.hhn.topicgrouper.test.AbstractTGTester;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

public class OptimizedTGTesterPM2 extends AbstractTGTester<String> {
	private DocumentProvider<String> testDocumentProvider;

	public OptimizedTGTesterPM2(File outputFile) throws IOException {
		super(outputFile);
	}

	protected SolutionListener<String> createSolutionListener(PrintStream out) {
		BasicSolutionReporter<String> res = new BasicSolutionReporter<String>(
				out, 4, true);
		res.setTestDocumentProvider(testDocumentProvider);
		return res;
	}

	protected DocumentProvider<String> createDocumentProvider() {
		DocumentProvider<String> provider = new TWCLDAPaperDocumentGenerator(
				new Random(45), new double[] { 5, 0.5, 0.5, 0.5 }, 6000, 100,
				100, 300, 300, 0, null, 0.5, 0.8);
		InDocumentHoldOutSplitter<String> splitter = new InDocumentHoldOutSplitter<String>(
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
		new OptimizedTGTesterPM2(null).run();
	}
}
