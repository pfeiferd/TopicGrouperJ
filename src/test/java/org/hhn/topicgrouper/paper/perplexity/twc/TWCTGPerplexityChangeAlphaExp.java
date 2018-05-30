package org.hhn.topicgrouper.paper.perplexity.twc;

import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator.PwtProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.validation.TGLRPerplexityCalculator;
import org.hhn.topicgrouper.tg.validation.TGPerplexityCalculator;

public class TWCTGPerplexityChangeAlphaExp extends TWCTGPerplexityExperiment {
	protected final TrueTopicAccuracyCalculator<String> accuracyCalculator;

	protected TWCLDAPaperDocumentGenerator gen;

	public TWCTGPerplexityChangeAlphaExp() {
		accuracyCalculator = new TrueTopicAccuracyCalculator<String>();
	}

	@Override
	protected boolean isProcessSolutionForTopics(int nTopics) {
		return nTopics == 4;
	}

	@Override
	protected void printHeader() {
		printStream.println("topics; perplexity; errorRate");
	}
	
	protected TWCLDAPaperDocumentGenerator getTWCLDAPaperDocumentGenerator() {
		return gen;
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		gen = TWCTGPerplexityExperiment.createSplit(res, 6000, new Random(getSeed()));
	}
	
	protected int getSeed() {
		return 42;
	}

	@Override
	protected TGPerplexityCalculator<String> createTGPerplexityCalculator() {
		return new TGLRPerplexityCalculator<String>(new Random(42), false,
				createDocumentSplitter(), 6.5);
	}

	@Override
	protected void evaluateSolution(TGSolution<String> solution) {
		double perplexity = computeTGPerplexity(solution, testProvider);

		PwtProvider<String> ldaFrequencyProvider = new PwtProvider<String>() {
			@Override
			public double getPwtFromModel(int topic, int wordIndex) {
				return perplexityCalculator.getPerplexityCalculator()
						.getTopicModeler().getPhi(topic, wordIndex);
			}

			@Override
			public double getCorrectPwt(int topic, String word) {
				return getTWCLDAPaperDocumentGenerator().getPwt(topic, word);
			}
		};

		double acc = accuracyCalculator.computeAccuracy(trainingProvider, 4,
				ldaFrequencyProvider);

		printResult(solution.getNumberOfTopics(), perplexity, 1 - acc);
	}

	protected void printResult(int topics, double perplexity, double alphaConc) {
		printStream.println(topics + "; " + perplexity + "; " + alphaConc);
	}

	public static void main(String[] args) {
		new TWCTGPerplexityChangeAlphaExp().run();
	}
}
