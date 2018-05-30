package org.hhn.topicgrouper.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.LabelingHoldOutSplitter;
import org.hhn.topicgrouper.eval.AbstractTGTester;
import org.hhn.topicgrouper.eval.Reuters21578;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolutionListenerMultiplexer;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.EHACTopicGrouper;
import org.hhn.topicgrouper.tg.report.BasicTGSolutionReporter;
import org.hhn.topicgrouper.tg.report.FreeMindXMLTopicHierarchyWriter;
import org.hhn.topicgrouper.tg.report.MindMapSolutionReporter;
import org.hhn.topicgrouper.tg.validation.TGPerplexityCalculator;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;

public class MindMapDemoReuters21578 extends AbstractTGTester<String> {
	private MindMapSolutionReporter<String> mindMapSolutionReporter;

	public MindMapDemoReuters21578(File file) throws IOException {
		super(file);
	}

	@Override
	protected TGSolver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		return new EHACTopicGrouper<String>(1, documentProvider, 1);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		Reuters21578 reuters = new Reuters21578(true); // Excluding stop words.
		LabelingDocumentProvider<String, String> trainingData = reuters
				.getCorpusDocumentProvider(new File(
						"src/test/resources/reuters21578"), true, false);
		return new LabelingHoldOutSplitter<String, String>(new Random(42),
				trainingData, 0, 3, 10).getRest();
	}

	@Override
	protected TGSolutionListener<String> createSolutionListener(
			PrintStream out, boolean fast) {
		TGSolutionListenerMultiplexer<String> multiplexer = new TGSolutionListenerMultiplexer<String>();
		multiplexer
				.addSolutionListener(mindMapSolutionReporter = new MindMapSolutionReporter<String>(
						5, false, 1.1, 0));
		multiplexer.addSolutionListener(new BasicTGSolutionReporter<String>(
				System.out, 30, true) {
			@Override
			public void updatedSolution(int newTopicIndex, int oldTopicIndex,
					double improvement, int t1Size, int t2Size,
					TGSolution<String> solution) {
				super.updatedSolution(newTopicIndex, oldTopicIndex,
						improvement, t1Size, t2Size, solution);
				if (solution.getNumberOfTopics() == 4) {
					AbstractTopicModeler<String> atm = new TGPerplexityCalculator<String>()
							.createSmoothedTopicModeler(solution);
					BasicLDAResultReporter.printTopics(System.out, atm, 10);
				}
			}
		});

		return multiplexer;
	}

	@Override
	protected void done(boolean fast) {
		try {
			FreeMindXMLTopicHierarchyWriter<String> writer = new FreeMindXMLTopicHierarchyWriter<String>(
					true);
			FileOutputStream mmStream = new FileOutputStream(
					createMindMapFile());
			writer.writeToFile(mmStream, mindMapSolutionReporter
					.getCurrentNodes().values());
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
		return new File("./target/Reuters21578.ser");
	}

	public static void main(String[] args) throws IOException {
		new MindMapDemoReuters21578(null).run();
	}
}
