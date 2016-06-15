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
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

public class MindMapDemoAPExtract extends OptimizedTGTester {
	private final Writer file;
	private MindMapSolutionReporter<String> mindMapSolutionReporter;
	private PrintStream out;

	public MindMapDemoAPExtract(File textFile, Writer file) throws IOException {
		super(textFile);
		this.file = file;
	}

	@Override
	protected Solver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		return new OptimizedTopicGrouper<String>(10, 0, documentProvider, 1);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		return new APParser(true).getCorpusDocumentProvider(new File(
				"src/test/resources/ap-corpus/extract/ap.txt"));
	}

	@Override
	protected SolutionListener<String> createSolutionListener(PrintStream out) {
		this.out = out;
		SolutionListenerMultiplexer<String> multiplexer = new SolutionListenerMultiplexer<String>();
		multiplexer
				.addSolutionListener(mindMapSolutionReporter = new MindMapSolutionReporter<String>(
						10, false, 1.01, 200));
		multiplexer.addSolutionListener(new BasicSolutionReporter<String>(
				out, 200, true, false, true));
		return multiplexer;
	}
	
	private long startTime;
	
	@Override
	protected void startSolving() {
		startTime = System.currentTimeMillis();
	}

	@Override
	protected void done() {
		out.println("Duration for solving in ms: " + (System.currentTimeMillis() - startTime));
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
		File file = new File("./target/MindMapDemoAPExtract.mm");
		FileWriter writer = new FileWriter(file);
		File file2 = new File("./target/MindMapDemoAPExtract.txt");
		new MindMapDemoAPExtract(file2, writer).run();
		writer.close();
	}
}
