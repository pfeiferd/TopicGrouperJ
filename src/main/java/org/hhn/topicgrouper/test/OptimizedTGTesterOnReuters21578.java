package org.hhn.topicgrouper.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import javax.swing.UIManager;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.InDocumentHoldOutSplitter;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.eval.Reuters21578;
import org.hhn.topicgrouper.report.BasicSolutionReporter;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

public class OptimizedTGTesterOnReuters21578 extends AbstractTGTester<String> {
	private DocumentProvider<String> testDocumentProvider;

	public OptimizedTGTesterOnReuters21578(File outputFile) throws IOException {
		super(outputFile);
	}

	protected SolutionListener<String> createSolutionListener(PrintStream out) {
		BasicSolutionReporter<String> res = new BasicSolutionReporter<String>(
				out, 4, true);
		res.setTestDocumentProvider(testDocumentProvider);
		return res;
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		DocumentProvider<String> provider = new Reuters21578(true)
				.getCorpusDocumentProvider(new File(
						"src/main/resources/reuters21578"),
						new String[] { "earn" }, false, true);
		InDocumentHoldOutSplitter<String> splitter = new InDocumentHoldOutSplitter<String>(
				new Random(42), provider, 0.1, 10);
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
		new OptimizedTGTesterOnReuters21578(null /*
				new File("target/ReutersResult.txt")*/).run();
	}
}
