package org.hhn.topicgrouper.paper.perplexity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.eval.AbstractTGTester;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolutionListenerMultiplexer;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.EHACTopicGrouper;
import org.hhn.topicgrouper.tg.report.FreeMindXMLTopicHierarchyWriter;
import org.hhn.topicgrouper.tg.report.MindMapSolutionReporter;
import org.hhn.topicgrouper.tg.report.store.MapNodeTGSolution;
import org.hhn.topicgrouper.tg.validation.TGPerplexityCalculator;

public abstract class AbstractTGPerplexityNTopicsExperiment<T> extends
		AbstractTGTester<T> {
	private MindMapSolutionReporter<T> mindMapSolutionReporter;
	protected final TGPerplexityCalculator<T> perplexityCalculator;

	protected final DocumentProvider<T> testProvider;
	protected final DocumentProvider<T> trainingProvider;

	protected final int maxTopicEval;
	protected final int topicEvalSteps;

	public AbstractTGPerplexityNTopicsExperiment(int maxTopicEval) {
		super(null);
		this.maxTopicEval = maxTopicEval;
		this.topicEvalSteps = initTopicEvalSteps();
		DocumentProvider<T>[] res = new DocumentProvider[2];
		createTrainingAndTestProvider(res);
		testProvider = res[0];
		trainingProvider = res[1];

		perplexityCalculator = createTGPerplexityCalculator();
		perplexityCalculator.init();

		try {
			os.addOutputStream(new FileOutputStream(new File("./target/"
					+ getClass().getSimpleName() + ".csv")));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		printHeader();
	}
	
	protected void printHeader() {
		printStream.println("topics; perplexity; alphaConc");		
	}

	protected int initTopicEvalSteps() {
		return 10;
	}

	protected abstract void createTrainingAndTestProvider(
			DocumentProvider<T>[] res);

	protected TGPerplexityCalculator<T> createTGPerplexityCalculator() {
		return new TGPerplexityCalculator<T>(false, createDocumentSplitter(),
				50);
	}

	protected DocumentSplitter<T> createDocumentSplitter() {
		return new DefaultDocumentSplitter<T>();
	}

	protected TGSolver<T> createSolver(DocumentProvider<T> documentProvider) {
		return new EHACTopicGrouper<T>(1, documentProvider, 1);
	}

	@Override
	protected DocumentProvider<T> createDocumentProvider() {
		return trainingProvider;
	}

	protected double computeTGPerplexity(TGSolution<T> solution,
			DocumentProvider<T> testDocumentProvider) {
		perplexityCalculator.setSolution(solution);
		optimizeParameters(trainingProvider);
		return perplexityCalculator.computePerplexity(testDocumentProvider);
	}

	protected void optimizeParameters(
			DocumentProvider<T> trainingDocumentProvider) {
	}

	@Override
	protected TGSolutionListener<T> createSolutionListener(PrintStream out,
			boolean fast) {
		TGSolutionListenerMultiplexer<T> multiplexer = new TGSolutionListenerMultiplexer<T>();
		if (!fast) {
			multiplexer
					.addSolutionListener(mindMapSolutionReporter = new MindMapSolutionReporter<T>(
							10, false, 1.1, 20));
		}
		multiplexer.addSolutionListener(createPerplexitySolutionListener(out));
		return multiplexer;
	}

	protected TGSolutionListener<T> createPerplexitySolutionListener(
			PrintStream out) {
		return new TGSolutionListener<T>() {
			@Override
			public void updatedSolution(int newTopicIndex, int oldTopicIndex,
					double improvement, int t1Size, int t2Size,
					TGSolution<T> solution) {
				if (isProcessSolutionForTopics(solution.getNumberOfTopics())) {
					evaluateSolution(solution);
				}
			}

			@Override
			public void initialized(TGSolution<T> initialSolution) {
			}

			@Override
			public void initalizing(double percentage) {
			}

			@Override
			public void done() {
			}

			@Override
			public void beforeInitialization(int maxTopics, int documents) {
			}
		};
	}

	protected void evaluateSolution(TGSolution<T> solution) {
		double perplexity = computeTGPerplexity(solution, testProvider);
		printResult(solution.getNumberOfTopics(), perplexity,
				perplexityCalculator.getAlphaConc());
	}

	@Override
	protected boolean isProcessSolutionForTopics(int nTopics) {
		return isComputePerplexity(nTopics, maxTopicEval, topicEvalSteps);
	}

	public static boolean isComputePerplexity(int topics, int max, int steps) {
		return topics <= max && (topics == 1 || topics % steps == 0);
	}

	protected void printResult(int topics, double perplexity, double alphaConc) {
		printStream.println(topics + "; " + perplexity + "; " + alphaConc);
	}

	@Override
	protected void done(boolean fast) {
		super.done(fast);
		if (!fast) {
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
	}

	protected File createMindMapFile() {
		return new File("./target/" + this.getClass().getSimpleName() + ".mm");
	}

	protected File createSerializationFile() {
		return new File("./target/" + this.getClass().getSimpleName() + ".ser");
	}

	public void runQuick() {
		run(MapNodeTGSolution.<T> loadFile(createSerializationFile()));
	}
}
