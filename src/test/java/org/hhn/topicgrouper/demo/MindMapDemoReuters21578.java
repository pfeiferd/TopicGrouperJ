package org.hhn.topicgrouper.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.AbstractTGTester;
import org.hhn.topicgrouper.eval.Reuters21578;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolutionListenerMultiplexer;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.TopicGrouperWithTreeSet;
import org.hhn.topicgrouper.tg.report.BasicTGSolutionReporter;
import org.hhn.topicgrouper.tg.report.FreeMindXMLTopicHierarchyWriter;
import org.hhn.topicgrouper.tg.report.MindMapSolutionReporter;

public class MindMapDemoReuters21578 extends AbstractTGTester<String> {
	private MindMapSolutionReporter<String> mindMapSolutionReporter;
	private OutputStream file;

	public MindMapDemoReuters21578(OutputStream file) throws IOException {
		super(null);
		this.file = file;
	}

	@Override
	protected TGSolver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		return new TopicGrouperWithTreeSet<String>(50, documentProvider, 1);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		return new Reuters21578(true).getCorpusDocumentProvider(new File(
				"src/test/resources/reuters21578"), new String[] { "earn" },
				false, true);
		// return new TWCLDAPaperDocumentGenerator(new Random(45), new double[]
		// {
		// 5, 0.5, 0.5, 0.5 }, 6000, 10, 10, 60, 60, 0, null, 0.8, 0.8);
	}

	@Override
	protected TGSolutionListener<String> createSolutionListener(PrintStream out) {
		TGSolutionListenerMultiplexer<String> multiplexer = new TGSolutionListenerMultiplexer<String>();
		multiplexer
				.addSolutionListener(mindMapSolutionReporter = new MindMapSolutionReporter<String>(
						5, false, 1.1, 20));
		multiplexer.addSolutionListener(new BasicTGSolutionReporter<String>(
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
		File file = new File("./target/reuters21578.mm");
		FileOutputStream writer = new FileOutputStream(file);
		new MindMapDemoReuters21578(writer).run();
		writer.close();
	}
}
