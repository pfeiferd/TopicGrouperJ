package org.hhn.topicgrouper.paper.perplexity.docs;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.APLargeParser;
import org.hhn.topicgrouper.paper.perplexity.AbstractTGPerplexityNTopicsExperiment;
import org.hhn.topicgrouper.tg.validation.TGLRPerplexityCalculator;
import org.hhn.topicgrouper.tg.validation.TGPerplexityCalculator;
import org.hhn.topicgrouper.util.StatsReport;

public class APTGPerplexityExperiment extends
		AbstractTGPerplexityNTopicsExperiment<String> {
	public APTGPerplexityExperiment() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		createSplit(res);
	}
	
	@Override
	protected TGPerplexityCalculator<String> createTGPerplexityCalculator() {
		return new TGLRPerplexityCalculator<String>(new Random(42), false,
				createDocumentSplitter(), 4) {
			@Override
			protected int initParticles() {
				return 5;
			}			
		};
	}

	@Override
	protected void optimizeParameters(
			DocumentProvider<String> trainingDocumentProvider) {
		HoldOutSplitter<String> holdoutSplitter = new HoldOutSplitter<String>(
				new Random(10), trainingDocumentProvider, 0.01, 0, false,
				(DefaultVocab<String>) trainingDocumentProvider.getVocab());
		perplexityCalculator.optimizeAlphaConc(1, 250,
				holdoutSplitter.getHoldOut(), 10);
		// perplexityCalculator.optimizeLambda(0, 1, trainingProvider, solution,
		// 10);
	}
	
	public static void createSplit(DocumentProvider<String>[] res) {
		DocumentProvider<String> provider = new APLargeParser(System.out, true, true).getCorpusDocumentProvider(new File(
				"src/test/resources/ap-corpus/full"), 25000);

		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), provider, 0.1, 5);
		res[0] = splitter.getHoldOut();
		res[1] = splitter.getRest();
		StatsReport.report(res[1], System.out);
	}

	public static void main(String[] args) throws IOException {
		new APTGPerplexityExperiment().run();
	}
}
