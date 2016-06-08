package org.hhn.topicgrouper.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.SolutionListenerMultiplexer;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.eval.Reuters21578;
import org.hhn.topicgrouper.report.BasicSolutionReporter;
import org.hhn.topicgrouper.report.FreeMindXMLTopicHierarchyWriter;
import org.hhn.topicgrouper.report.MindMapSolutionReporter;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

public class MindMapDemo extends OptimizedTGTester {
	private MindMapSolutionReporter<String> mindMapSolutionReporter;
	private Writer file;

	public MindMapDemo(Writer file) throws IOException {
		super(null);
		this.file = file;
	}

	@Override
	protected Solver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		return new OptimizedTopicGrouper<String>(50, 0, documentProvider, 1);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		return new Reuters21578(true).getCorpusDocumentProvider(new File(
				"src/main/resources/reuters21578"), new String[] { "earn" },
				false, true);
		// return new TWCLDAPaperDocumentGenerator(new Random(45), new double[]
		// {
		// 5, 0.5, 0.5, 0.5 }, 6000, 10, 10, 60, 60, 0, null, 0.8, 0.8);
	}

	@Override
	protected SolutionListener<String> createSolutionListener(PrintStream out) {
		SolutionListenerMultiplexer<String> multiplexer = new SolutionListenerMultiplexer<String>();
		multiplexer
				.addSolutionListener(mindMapSolutionReporter = new MindMapSolutionReporter<String>(
						5, false, 1.1, 20));
		multiplexer.addSolutionListener(new BasicSolutionReporter<String>(
				System.out, 30, true));
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

	public static void main(String[] args) throws IOException {
		File file = new File("./target/result7.mm");
		FileWriter writer = new FileWriter(file);
		new MindMapDemo(writer).run();
		writer.close();
	}
}
