package org.hhn.topicgrouper.paper.classfication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.eval.AbstractTGTester;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolutionListenerMultiplexer;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.TopicGrouperWithTreeSet;
import org.hhn.topicgrouper.tg.report.BasicTGSolutionReporter;
import org.hhn.topicgrouper.tg.report.FreeMindXMLTopicHierarchyWriter;
import org.hhn.topicgrouper.tg.report.MindMapSolutionReporter;

public abstract class AbstractTGClassificationExperiment extends
		AbstractTGTester<String> {
	protected final LabelingDocumentProvider<String, String> testProvider;
	protected final LabelingDocumentProvider<String, String> trainingProvider;

	private MindMapSolutionReporter<String> mindMapSolutionReporter;

	public AbstractTGClassificationExperiment() throws IOException {
		super(null);

		// Use ModApte split:
		LabelingDocumentProvider<String, String>[] res = new LabelingDocumentProvider[2];
		createTrainingAndTestProvider(res);
		testProvider = res[0];
		trainingProvider = res[1];

		// LabelingDocumentProvider<String, String> provider = initProvider();
		// LabelingHoldOutSplitter<String, String> splitter =
		// createHoldOutSplitter(provider);
		// trainingProvider = splitter.getRest();
		// testProvider = splitter.getHoldOut();
	}

	// protected LabelingDocumentProvider<String, String> initProvider() {
	// return new Reuters21578(true).getCorpusDocumentProvider(new File(
	// "src/test/resources/reuters21578"), true, true);
	// }
	//
	// protected LabelingHoldOutSplitter<String, String> createHoldOutSplitter(
	// LabelingDocumentProvider<String, String> provider) {
	// return new LabelingHoldOutSplitter<String, String>(new Random(42),
	// provider, 0.1, 20, 10);
	// }

	protected abstract void createTrainingAndTestProvider(
			LabelingDocumentProvider<String, String>[] res);

	@Override
	protected TGSolver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		return new TopicGrouperWithTreeSet<String>(1, documentProvider, 1); // , 0.05);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		return trainingProvider;
	}

	@Override
	protected TGSolutionListener<String> createSolutionListener(PrintStream out) {
		TGSolutionListenerMultiplexer<String> multiplexer = new TGSolutionListenerMultiplexer<String>();
		multiplexer
				.addSolutionListener(mindMapSolutionReporter = new MindMapSolutionReporter<String>(
						10, false, 1.1, 20));
		multiplexer
				.addSolutionListener(createClassificationSolutionListener(out));
		return multiplexer;
	}

	protected TGSolutionListener<String> createClassificationSolutionListener(
			final PrintStream out) {
		return new TGSolutionListener<String>() {
			@Override
			public void beforeInitialization(int maxTopics, int documents) {
			}

			@Override
			public void initalizing(double percentage) {
			}

			@Override
			public void done() {
				AbstractTGClassificationExperiment.this.done();
			}

			@Override
			public void initialized(TGSolution<String> initialSolution) {
			}

			@Override
			public void updatedSolution(int newTopicIndex, int oldTopicIndex,
					double improvement, int t1Size, int t2Size,
					final TGSolution<String> solution) {
				trainAndTest(out, solution, false);
				trainAndTest(out, solution, true);
			}
		};
	}

	@Override
	protected void done() {
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
		return new File("./target/" + this.getClass().getSimpleName() + ".mm");
	}

	protected File createSerializationFile() {
		return new File("./target/" + this.getClass().getSimpleName() + ".ser");
	}

	protected void trainAndTest(PrintStream out, TGSolution<String> solution,
			boolean optimizeLambda) {
		SupervisedDocumentClassifier<String, String> classifier = AbstractTGClassificationExperiment.this
				.createClassifier(solution);
		if (classifier != null) {
			classifier.train(trainingProvider);
			if (optimizeLambda) {
				((AbstractTopicBasedNBClassifier<String, String>) classifier)
						.optimizeLambda(0, 0.5, trainingProvider, 10, true);
			}
			double microAvg = classifier.test(testProvider, true);
			double macroAvg = classifier.test(testProvider, false);
			printResult(out, optimizeLambda, solution.getNumberOfTopics(),
					microAvg, macroAvg);
		}
	}

	protected void printResult(PrintStream out, boolean optimized, int topics,
			double microAvg, double macroAvg) {
		out.println(topics + "; " + microAvg + "; " + macroAvg);
	}

	protected abstract SupervisedDocumentClassifier<String, String> createClassifier(
			TGSolution<String> solution);
}
