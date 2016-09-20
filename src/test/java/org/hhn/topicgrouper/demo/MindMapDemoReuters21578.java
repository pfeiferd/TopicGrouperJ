package org.hhn.topicgrouper.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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

	public MindMapDemoReuters21578(File file) throws IOException {
		super(file);
	}

	@Override
	protected TGSolver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		return new TopicGrouperWithTreeSet<String>(10, documentProvider, 1);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		return new Reuters21578(true).getCorpusDocumentProvider(new File(
				"src/test/resources/reuters21578"), new String[] { "earn" },
				false, true);
	}

	@Override
	protected TGSolutionListener<String> createSolutionListener(PrintStream out) {
		TGSolutionListenerMultiplexer<String> multiplexer = new TGSolutionListenerMultiplexer<String>();
		multiplexer
				.addSolutionListener(mindMapSolutionReporter = new MindMapSolutionReporter<String>(
						10, false, 1.1, 20));
		multiplexer.addSolutionListener(new BasicTGSolutionReporter<String>(
				System.out, 30, true));
		return multiplexer;
	}

	@Override
	protected void done() {
		try {
			FreeMindXMLTopicHierarchyWriter<String> writer = new FreeMindXMLTopicHierarchyWriter<String>(
					true);
			FileOutputStream mmStream = new FileOutputStream(createMindMapFile());
			writer.writeToFile(mmStream, mindMapSolutionReporter.getCurrentNodes()
					.values());
			mmStream.close();
			ObjectOutputStream objectStream = new ObjectOutputStream(
					new FileOutputStream(createSerializationFile()));
			objectStream.writeObject(mindMapSolutionReporter.getAllNodes());
			objectStream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected File createMindMapFile() {
		return new File("./target/Reuters21578.mm");
	}
	
	protected File createSerializationFile() {
		return new File(
				"./target/Reuters21578.ser");
	}

	public static void main(String[] args) throws IOException {
		new MindMapDemoReuters21578(null).run();
	}
}
