package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.LabelingHoldOutSplitter;
import org.hhn.topicgrouper.eval.OHSUMEDParser;

public class OHSUMEDTGNaiveBayesExperiment extends
		ReutersTGNaiveBayesExperiment {
	public OHSUMEDTGNaiveBayesExperiment() throws IOException {
		super();
	}

	@Override
	protected void createTrainingAndTestProvider(
			LabelingDocumentProvider<String, String>[] res) {
		createSplit(res);
	}

	public static void createSplit(
			LabelingDocumentProvider<String, String>[] res) {
		LabelingDocumentProvider<String, String> provider = new OHSUMEDParser()
				.getCorpusDocumentProvider("src/test/resources/ohscal.wc.arff");
		LabelingHoldOutSplitter<String, String> splitter = new LabelingHoldOutSplitter<String, String>(
				new Random(42), provider, 0.4, 5, -1);
		System.out.println(splitter.getHoldOut().getDocuments().size());
		System.out.println(splitter.getRest().getDocuments().size());
		System.out.println(splitter.getHoldOut().getVocab().getNumberOfWords());
		res[0] = splitter.getHoldOut();
		res[1] = splitter.getRest();
	}

	public static void main(String[] args) throws IOException {
		new OHSUMEDTGNaiveBayesExperiment().run();
	}
}
