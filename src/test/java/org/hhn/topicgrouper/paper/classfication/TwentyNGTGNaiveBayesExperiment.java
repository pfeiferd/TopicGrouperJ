package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;

import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public class TwentyNGTGNaiveBayesExperiment extends ReutersTGNaiveBayesExperiment {
	public TwentyNGTGNaiveBayesExperiment() throws IOException {
		super();
	}

	@Override
	protected void createTrainingAndTestProvider(
			LabelingDocumentProvider<String, String>[] res) {
		createModApteSplit(res);
	}
	
	public static void createModApteSplit(
			LabelingDocumentProvider<String, String>[] res) {
		// TODO
	}
	
	public static void main(String[] args) throws IOException {
		new TwentyNGTGNaiveBayesExperiment().run();
	}	
}
