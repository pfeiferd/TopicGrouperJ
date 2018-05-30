package org.hhn.topicgrouper.paper.perplexity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.util.OutputStreamMultiplexer;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;
import org.hhn.topicgrouper.validation.BasicPerplexityCalculator;
import org.hhn.topicgrouper.validation.PerplexityCalculatorAveraging;
import org.hhn.topicgrouper.validation.PerplexityCalculatorLeftToRight;

public abstract class AbstractTopicModelerPerplexityNTopicsExperiment<T> {
	protected final int maxTopicEval;
	protected final int topicEvalSteps;

	protected final PrintStream printStream;
	protected final OutputStreamMultiplexer os;

	protected final DocumentProvider<T> testProvider;
	protected final DocumentProvider<T> trainingProvider;
	protected final BasicPerplexityCalculator<T> calc1;
	protected final BasicPerplexityCalculator<T> calc2;
	protected final BasicPerplexityCalculator<T> calc3;

	public AbstractTopicModelerPerplexityNTopicsExperiment(int maxTopicEval) {
		this.maxTopicEval = maxTopicEval;
		this.topicEvalSteps = initTopicEvalSteps();

		os = new OutputStreamMultiplexer();
		addOutputStreams(os);
		printStream = new PrintStream(os);

		DocumentProvider<T>[] res = new DocumentProvider[2];
		createTrainingAndTestProvider(res);
		testProvider = res[0];
		trainingProvider = res[1];

		calc1 = initPerplexityCalculator1();
		calc2 = initPerplexityCalculator2();
		calc3 = initPerplexityCalculator3();
	}
	
	protected void addOutputStreams(OutputStreamMultiplexer os) {
		os.addOutputStream(System.out);
		try {
			os.addOutputStream(new FileOutputStream(new File("./target/"
					+ getClass().getSimpleName() + ".csv")));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	protected int initParticles() {
		return 20;
	}

	protected int initTopicEvalSteps() {
		return 10;
	}

	protected void printOutputHeader() {
		printStream
				.println("topics; perplexityMean; perplexityLR; perplexityInt");
	}

	protected DocumentSplitter<T> createDocumentSplitter() {
		return new DefaultDocumentSplitter<T>();
		// Use separate random object to ensure that split happens always the
		// same way across steps and algorithms.
		// return new FiftyFiftyDocumentSplitter<T>(new Random(43));
	}

	protected BasicPerplexityCalculator<T> initPerplexityCalculator1() {
		return new PerplexityCalculatorAveraging<T>(false,
				createDocumentSplitter());
	}

	protected BasicPerplexityCalculator<T> initPerplexityCalculator2() {
		return new PerplexityCalculatorLeftToRight<T>(new Random(42), false,
				createDocumentSplitter(), initParticles());
	}

	protected abstract BasicPerplexityCalculator<T> initPerplexityCalculator3();

	protected abstract void createTrainingAndTestProvider(
			DocumentProvider<T>[] res);

	public void run(boolean optimize) {
		printOutputHeader();
		for (int topics = 1; topics <= trainingProvider.getVocab()
				.getNumberOfWords(); topics++) {
			if (isProcessSolutionForTopics(topics)) {
				runExperiment(topics, optimize);
			}
		}
		printStream.close();
	}

	protected boolean isProcessSolutionForTopics(int nTopics) {
		return AbstractTGPerplexityNTopicsExperiment.isComputePerplexity(
				nTopics, maxTopicEval, topicEvalSteps);
	}

	protected void runExperiment(final int topics, boolean optimize) {
		AbstractTopicModeler<T> modeler = createTopicModeler(topics,
				trainingProvider, optimize);
		trainTopicModeler(modeler, trainingProvider, optimize);
		evaluateTopicModeler(topics, modeler);
	}

	protected void printResult(int topics, double perplexity1,
			double perplexity2, double perplexity3) {
		printStream.println(topics + "; " + perplexity1 + "; " + perplexity2
				+ "; " + perplexity3);
	}

	protected abstract AbstractTopicModeler<T> createTopicModeler(int topics,
			DocumentProvider<T> documentProvider, boolean optimize);

	protected abstract void trainTopicModeler(AbstractTopicModeler<T> modeler, DocumentProvider<T> documentProvider, 
			boolean optimize);

	protected void evaluateTopicModeler(int topics,
			AbstractTopicModeler<T> modeler) {
		double perplexity1 = 0;
		if (calc1 != null) {
			calc1.setTopicModeler(modeler);
			perplexity1 = calc1.computePerplexity(testProvider);
		}
		double perplexity2 = 0;
		if (calc2 != null) {
			calc2.setTopicModeler(modeler);
			perplexity2 = calc2.computePerplexity(testProvider);
		}
		double perplexity3 = 0;
		if (calc3 != null) {
			calc3.setTopicModeler(modeler);
			perplexity3 = calc3.computePerplexity(testProvider);
		}
		printResult(topics, perplexity1, perplexity2, perplexity3);
	}

	protected DocumentProvider<T> createDocumentProvider() {
		return trainingProvider;
	}
}
