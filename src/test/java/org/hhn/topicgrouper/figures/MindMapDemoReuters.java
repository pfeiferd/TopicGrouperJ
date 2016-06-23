package org.hhn.topicgrouper.figures;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.SolutionListenerMultiplexer;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.eval.Reuters21578;
import org.hhn.topicgrouper.report.BasicSolutionReporter;
import org.hhn.topicgrouper.report.FreeMindXMLTopicHierarchyWriter;
import org.hhn.topicgrouper.report.MindMapSolutionReporter;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

public class MindMapDemoReuters extends OptimizedTGTester {
	private MindMapSolutionReporter<String> mindMapSolutionReporter;
	private OutputStream file;

	public MindMapDemoReuters(OutputStream file) throws IOException {
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
				"src/test/resources/reuters21578"), new String[] { "earn" },
				true, true);
	}

	@Override
	protected SolutionListener<String> createSolutionListener(PrintStream out) {
		SolutionListenerMultiplexer<String> multiplexer = new SolutionListenerMultiplexer<String>();
		multiplexer
				.addSolutionListener(mindMapSolutionReporter = new MindMapSolutionReporter<String>(
						5, false, 1.2, 50));
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
		File file = new File("./target/MindMapDemoReuters.mm");
		OutputStream writer = new FileOutputStream(file);
		new MindMapDemoReuters(writer).run();
		writer.close();
	}
}
