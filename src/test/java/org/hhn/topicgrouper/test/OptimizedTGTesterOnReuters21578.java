package org.hhn.topicgrouper.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import javax.swing.UIManager;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.eval.AbstractTGTester;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.report.CSVSolutionReporter;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

public class OptimizedTGTesterOnReuters21578 extends AbstractTGTester<String> {
	private DocumentProvider<String> testDocumentProvider;

	public OptimizedTGTesterOnReuters21578(File outputFile) throws IOException {
		super(outputFile);
	}

	protected TGSolutionListener<String, T> createSolutionListener(PrintStream out) {
		CSVSolutionReporter<String> res = new CSVSolutionReporter<String>(out, true);
		res.setTestDocumentProvider(testDocumentProvider);
		return res;
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		DocumentProvider<String> documentProvider = new APParser(true)
				.getCorpusDocumentProvider(new File(
						"src/test/resources/ap-corpus/extract/ap.txt"));
		// DocumentProvider<String> provider = new Reuters21578(true)
		// .getCorpusDocumentProvider(new File(
		// "src/test/resources/reuters21578"),
		// new String[] { "earn" }, false, true);
		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), documentProvider, 0.1, 10);
		testDocumentProvider = splitter.getHoldOut();
		return splitter.getRest();
	}

	@Override
	protected TGSolver<String> createSolver(
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
		new OptimizedTGTesterOnReuters21578(
				new File("target/TGOnAPExtractMinWordFr10Holdout0.1.csv")).run();
	}
}
