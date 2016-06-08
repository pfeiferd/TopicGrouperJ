package org.hhn.topicgrouper.figures;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.SolutionListenerMultiplexer;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.report.BasicSolutionReporter;
import org.hhn.topicgrouper.report.FreeMindXMLTopicHierarchyWriter;
import org.hhn.topicgrouper.report.MindMapSolutionReporter;
import org.hhn.topicgrouper.test.AbstractTGTester;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

public class OptimizedTG2TesterOnAPMindMap extends AbstractTGTester<String> {
	private MindMapSolutionReporter<String> mindMapSolutionReporter;
	private Writer file;

	public OptimizedTG2TesterOnAPMindMap(Writer file) throws IOException {
		super(null);
		this.file = file;
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		return new APParser(true).getCorpusDocumentProvider(new File(
				"src/test/resources/ap-corpus/extract/ap.txt"));
	}

	@Override
	protected SolutionListener<String> createSolutionListener(PrintStream out) {
		SolutionListenerMultiplexer<String> multiplexer = new SolutionListenerMultiplexer<String>();
		multiplexer
				.addSolutionListener(mindMapSolutionReporter = new MindMapSolutionReporter<String>(
						10, false, 1.01, 300));
		multiplexer.addSolutionListener(new BasicSolutionReporter<String>(
				System.out, 100, true));
		return multiplexer;
	}

	@Override
	protected void done() {
		try {
			FreeMindXMLTopicHierarchyWriter<String> writer = new FreeMindXMLTopicHierarchyWriter<String>(
					true);
			writer.writeToFile(file, mindMapSolutionReporter.getCurrentNodes()
					.values());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Solver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		return new OptimizedTopicGrouper<String>(20, 0, documentProvider, 1);
	}

	public static void main(String[] args) throws IOException {
		File file = new File("./target/OptimizedTG2TesterOnAPMindMap.mm");
		FileWriter writer = new FileWriter(file);
		new OptimizedTG2TesterOnAPMindMap(writer).run();
		writer.close();
	}
}
