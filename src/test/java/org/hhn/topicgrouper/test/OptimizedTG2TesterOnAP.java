package org.hhn.topicgrouper.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.UIManager;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.eval.AbstractTGTester;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolutionListenerMultiplexer;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.report.BasicTGSolutionReporter;
import org.hhn.topicgrouper.tg.report.CSVSolutionReporter;
import org.hhn.topicgrouper.tgimpl.exp.OptimizedTopicGrouper2;

public class OptimizedTG2TesterOnAP extends AbstractTGTester<String> {
	public OptimizedTG2TesterOnAP(File outputFile) throws IOException {
		super(outputFile);
	}

	protected TGSolutionListener<String, T> createSolutionListener(PrintStream out) {
		TGSolutionListenerMultiplexer<String> multiplexer = new TGSolutionListenerMultiplexer<String>();
		CSVSolutionReporter<String> res1 = new CSVSolutionReporter<String>(out, true);
		BasicTGSolutionReporter<String> res2 = new BasicTGSolutionReporter<String>(
				System.out, 100, true);
		multiplexer.addSolutionListener(res1);
		multiplexer.addSolutionListener(res2);
		return multiplexer;
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		DocumentProvider<String> provider = new APParser(true)
				.getCorpusDocumentProvider(new File(
						"src/test/resources/ap-corpus/extract/ap.txt"));
		return provider;
	}

	@Override
	protected TGSolver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		return new OptimizedTopicGrouper2<String>(10, 0, documentProvider, 1);
	}

	public static void main(String[] args) throws IOException {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		new OptimizedTG2TesterOnAP(// null /*
				new File("target/OptimizedTG2TesterOnAP.csv")).run();
	}
}
