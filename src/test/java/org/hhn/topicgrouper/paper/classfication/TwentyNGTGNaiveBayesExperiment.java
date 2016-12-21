package org.hhn.topicgrouper.paper.classfication;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.LabelingHoldOutSplitter;
import org.hhn.topicgrouper.eval.TwentyNGParser;

public class TwentyNGTGNaiveBayesExperiment extends
		ReutersTGNaiveBayesExperiment {
	public TwentyNGTGNaiveBayesExperiment() throws IOException {
		super();
	}

	@Override
	protected void createTrainingAndTestProvider(
			LabelingDocumentProvider<String, String>[] res) {
		createSplit(res);
	}

	public static void createSplit(
			LabelingDocumentProvider<String, String>[] res) {
		LabelingDocumentProvider<String, String> provider = new TwentyNGParser(
				null, false, true).getCorpusDocumentProvider(new File(
				"src/test/resources/20news-18828"), 0.5);
		LabelingHoldOutSplitter<String, String> splitter = new LabelingHoldOutSplitter<String, String>(
				new Random(42), provider, 0.4, 5, -1);
		System.out.println(provider.getDocuments().size());
		System.out.println(provider.getVocab().getNumberOfWords());
		res[0] = splitter.getHoldOut();
		res[1] = splitter.getRest();
	}

	public static void main(String[] args) throws IOException {
		new TwentyNGTGNaiveBayesExperiment().run();
	}
}
