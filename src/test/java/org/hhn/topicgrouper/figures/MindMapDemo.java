package org.hhn.topicgrouper.figures;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.SolutionListenerMultiplexer;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.report.BasicSolutionReporter;
import org.hhn.topicgrouper.report.FreeMindXMLTopicHierarchyWriter;
import org.hhn.topicgrouper.report.MindMapSolutionReporter;
import org.hhn.topicgrouper.report.TopicHistoryCSVSolutionReporter;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

public class MindMapDemo extends OptimizedTGTester {
	private MindMapSolutionReporter<String> mindMapSolutionReporter;
	private OutputStream file;

	public MindMapDemo(OutputStream file) throws IOException {
		super(null);
		this.file = file;
	}

	@Override
	protected Solver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		return new OptimizedTopicGrouper<String>(1, 0, documentProvider, 1);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		return new TWCLDAPaperDocumentGenerator(new Random(45), new double[] {
				5, 0.5, 0.5, 0.5 }, 6000, 100, 100, 30, 30, 0, null, 0.8, 0.8);
	}

	@Override
	protected SolutionListener<String> createSolutionListener(PrintStream out) {
		SolutionListenerMultiplexer<String> multiplexer = new SolutionListenerMultiplexer<String>();
		multiplexer
				.addSolutionListener(mindMapSolutionReporter = new MindMapSolutionReporter<String>(
						5, false, 1.2, 10));
		multiplexer.addSolutionListener(new BasicSolutionReporter<String>(
				System.out, 4, true));
		try {
			File file = new File("./target/MindMapDemoTopicHistory.csv");
			PrintStream pw = new PrintStream(file);
			multiplexer
					.addSolutionListener(new TopicHistoryCSVSolutionReporter<String>(
							pw, 10));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		return multiplexer;
	}

	@Override
	protected void done() {
		try {
			FreeMindXMLTopicHierarchyWriter<String> writer = new FreeMindXMLTopicHierarchyWriter<String>(
					true);
			writer.writeToFile(file, mindMapSolutionReporter.getCurrentNodes()
					.values());
			File serializedFile = new File("./target/MindMapDemoTopicHistory.ser");
			ObjectOutputStream objectStream = new ObjectOutputStream(new FileOutputStream(serializedFile));
			objectStream.writeObject(mindMapSolutionReporter.getAllNodes());
			objectStream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws IOException {
		File file = new File("./target/MindMapDemo.mm");
		FileOutputStream writer = new FileOutputStream(file);
		new MindMapDemo(writer).run();
		writer.close();
	}
}
