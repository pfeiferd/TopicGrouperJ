package org.hhn.topicgrouper.paper;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.FiftyFiftyDocumentSplitter;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.validation.AbstractLDAPerplexityCalculator;
import org.hhn.topicgrouper.lda.validation.LDAPerplexityCalculatorAveraging;
import org.hhn.topicgrouper.lda.validation.LDAPerplexityCalculatorEstimatedTheta;
import org.hhn.topicgrouper.lda.validation.LDAPerplexityCalculatorLeftToRight;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.validation.TGPerplexityCalculator;

public abstract class AbstractPerplexityErrorRateExperiment<T> {
	protected final Random random;
	protected final AbstractLDAPerplexityCalculator<T> calc1;
	protected final AbstractLDAPerplexityCalculator<T> calc2;
	protected final AbstractLDAPerplexityCalculator<T> calc3;
	protected final TGPerplexityCalculator<T> perplexityCalculator;
	protected final int gibbsIterations;

	protected double tgSmothingLambda;
	
	public AbstractPerplexityErrorRateExperiment(Random random, int gibbsIterations) {
		this.random = random;
		this.gibbsIterations = gibbsIterations;
		calc1 = initLDAPerplexityCalculator1();
		calc2 = initLDAPerplexityCalculator2();
		calc3 = initLDAPerplexityCalculator3();
		perplexityCalculator = initTGPerplexityCalculator();
		tgSmothingLambda = 0.5;
	}
	
	protected DocumentSplitter<T> createDocumentSplitter() {
		// Use separate random object to ensure that split happens always the same way across steps and algorithms.
		return new FiftyFiftyDocumentSplitter<T>(new Random(43));
	}
	
	protected TGPerplexityCalculator<T> initTGPerplexityCalculator() {
		return new TGPerplexityCalculator<T>(false, createDocumentSplitter()) {
			@Override
			protected double getSmoothingLambda(TGSolution<T> s) {
				return tgSmothingLambda;
			}
		};
	}
	
	protected AbstractLDAPerplexityCalculator<T> initLDAPerplexityCalculator1() {
		return new LDAPerplexityCalculatorAveraging<T>(false, createDocumentSplitter()); 
	}

	protected AbstractLDAPerplexityCalculator<T> initLDAPerplexityCalculator2() {
		return new LDAPerplexityCalculatorLeftToRight<T>(false, createDocumentSplitter(),
				gibbsIterations);
	}
	
	protected AbstractLDAPerplexityCalculator<T> initLDAPerplexityCalculator3() {
		return new LDAPerplexityCalculatorEstimatedTheta<T>(false, createDocumentSplitter(), gibbsIterations, 1000);
	}
	
	public void run(int steps, int avgC)
			throws IOException {
		PrintStream pw = prepareLDAPrintStream();
		PrintStream pw2 = prepareTGPrintStream();
		PrintStream pw3 = prepareTGLikelihoodPrintStream();

		double[] perplexity1 = new double[avgC];
		double[] perplexity2 = new double[avgC];
		double[] perplexity3 = new double[avgC];
		double[] acc = new double[avgC];

		double[] tgAcc = new double[avgC];
		double[] tgPerplexity = new double[avgC];

		for (int i = 0; i < steps; i++) {
			System.out.print("Step: ");
			System.out.println(i);
			for (int j = 0; j < avgC; j++) {
				System.out.print("Repeat: ");
				System.out.println(j);
				DocumentProvider<T> documentProvider = createDocumentProvider(i, j);
				System.out.print("Documents: ");
				System.out.println(documentProvider.getDocuments().size());
				System.out.print("Vocabulary: ");
				System.out.println(documentProvider.getVocab().getNumberOfWords());
				HoldOutSplitter<T> holdOutSplitter = createHoldoutSplitter(documentProvider, i, j);

				DocumentProvider<T> trainingDocumentProvider = holdOutSplitter
						.getRest();
				trainingDocumentProvider = prepareTrainingDocumentProvider(i,
						trainingDocumentProvider);
				System.out.print("Training Documents: ");
				System.out.println(trainingDocumentProvider.getDocuments().size());
				System.out.print("Training Vocabulary: ");
				System.out.println(trainingDocumentProvider.getVocab().getNumberOfWords());

				runTopicGrouper(pw3, i, j, trainingDocumentProvider,
						holdOutSplitter.getHoldOut(), tgPerplexity, tgAcc);
				
				runLDAGibbsSampler(i, j, gibbsIterations,
						trainingDocumentProvider,
						holdOutSplitter.getHoldOut(), perplexity1, perplexity2,
						perplexity3, acc);
			}
			aggregateTGResults(pw2, i, tgPerplexity, tgAcc);
			aggregateLDAResults(pw, i, perplexity1, perplexity2, perplexity3, acc);
		}
		if (pw != null) {
			pw.close();
		}
		if (pw2 != null) {
			pw2.close();
		}
		if (pw3 != null) {
			pw3.close();
		}
	}

	protected DocumentProvider<T> prepareTrainingDocumentProvider(int step,
			DocumentProvider<T> trainingDocumentProvider) {
		return trainingDocumentProvider;
	}

	protected abstract HoldOutSplitter<T> createHoldoutSplitter(DocumentProvider<T> documentProvider, int step, int repeat);

	protected abstract DocumentProvider<T> createDocumentProvider(int step, int repeat);

	protected abstract LDAGibbsSampler<T> createGibbsSampler(int step,
			DocumentProvider<T> documentProvider);

	protected abstract void runLDAGibbsSampler(int step, int repeat,
			int gibbsIterations, final DocumentProvider<T> documentProvider,
			final DocumentProvider<T> testDocumentProvider,
			double[] perplexity1, double[] perplexity2, double[] perplexity3, double[] acc);

	protected abstract PrintStream prepareLDAPrintStream() throws IOException;

	protected abstract PrintStream prepareTGPrintStream() throws IOException;

	protected abstract PrintStream prepareTGLikelihoodPrintStream()
			throws IOException;

	protected abstract void runTopicGrouper(PrintStream pw3, int step,
			int repeat, DocumentProvider<T> documentProvider,
			DocumentProvider<T> testDocumentProvider, double[] tgPerplexity,
			double[] tgAcc);

	protected abstract void aggregateLDAResults(PrintStream pw, int step,
			double[] perplexity1, double[] perplexity2, double[] perplexity3, double[] acc);

	protected abstract void aggregateTGResults(PrintStream pw, int step,
			double[] tgPerplexity, double[] tgAcc);
}
