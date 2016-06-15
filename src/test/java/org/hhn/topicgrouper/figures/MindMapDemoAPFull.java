package org.hhn.topicgrouper.figures;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.SolutionListenerMultiplexer;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.eval.APLargeParser;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.report.BasicSolutionReporter;
import org.hhn.topicgrouper.report.FreeMindXMLTopicHierarchyWriter;
import org.hhn.topicgrouper.report.MindMapSolutionReporter;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

public class MindMapDemoAPFull extends OptimizedTGTester {
	private final Writer file;
	private MindMapSolutionReporter<String> mindMapSolutionReporter;
	private PrintStream out;

	public MindMapDemoAPFull(File textFile, Writer file) throws IOException {
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
		return new APLargeParser(System.out, true).getCorpusDocumentProvider(new File(
				"src/test/resources/ap-corpus/full"), 16000);
	}

	@Override
	protected SolutionListener<String> createSolutionListener(PrintStream out) {
		this.out = out;
		SolutionListenerMultiplexer<String> multiplexer = new SolutionListenerMultiplexer<String>();
		multiplexer
				.addSolutionListener(mindMapSolutionReporter = new MindMapSolutionReporter<String>(
						10, false, 1.01, 200));
		multiplexer.addSolutionListener(new BasicSolutionReporter<String>(
				out, 200, true, false));
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
		File file = new File("./target/MindMapDemoAPFull.mm");
		FileWriter writer = new FileWriter(file);
		File file2 = new File("./target/MindMapDemoAPFull.txt");
		new MindMapDemoAPFull(file2, writer).run();
		writer.close();
	}
}
