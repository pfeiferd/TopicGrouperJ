package org.hhn.topicgrouper.paper.perplexity.retail;

import java.io.File;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.USSupermarketParser;
import org.hhn.topicgrouper.paper.perplexity.AbstractTGPerplexityNTopicsExperiment;

public class USSupermarketTGPerplexityExperiment extends
		AbstractTGPerplexityNTopicsExperiment<String> {
	public USSupermarketTGPerplexityExperiment() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		createSplit(res);
	}

	@Override
	protected int initTopicEvalSteps() {
		return 20;
	}
	
	@Override
	protected DocumentSplitter<String> createDocumentSplitter() {
		return new DefaultDocumentSplitter<String>();
		// return new FiftyFiftyDocumentSplitter<String>(new Random(42));
	}

	public static void createSplit(DocumentProvider<String>[] res) {
		USSupermarketParser uciParser = new USSupermarketParser(new File(
				"./src/test/resources/USSupermarket98"), "uss", -1, true);
		DocumentProvider<String> provider = uciParser.getDocumentProvider();

		// BelgianRetailStoreParser parser = new BelgianRetailStoreParser();
		// DocumentProvider<String> provider = parser
		// .getCorpusDocumentProvider(new File(
		// "src/test/resources/BelgianRetailStore/retail.dat"));

		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), provider, 0.1, 10);
		res[0] = splitter.getHoldOut();
		res[1] = splitter.getRest();

		System.out.println(res[0].getDocuments().size());
		System.out.println(res[0].getVocab().getNumberOfWords());
		System.out.println(res[1].getDocuments().size());
		System.out.println(res[1].getVocab().getNumberOfWords());
	}

	public static void main(String[] args) {
		new USSupermarketTGPerplexityExperiment().run();
	}
}
