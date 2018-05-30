package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;

import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public class TwentyNGTGIGMixClassificationExperiment extends
		ReutersTGIGMixClassificationExperiment {
	public TwentyNGTGIGMixClassificationExperiment(Class<?> clazz)
			throws IOException {
		super(clazz);
	}

	@Override
	protected void createTrainingAndTestProvider(
			LabelingDocumentProvider<String, String>[] res) {
		TwentyNGTGNaiveBayesExperiment.createSplit(res);
	}
	
	public static void main(String[] args) throws IOException {
		new TwentyNGTGNaiveBayesExperiment().run();
		new TwentyNGTGIGMixClassificationExperiment(
				TwentyNGTGNaiveBayesExperiment.class).run(false);
	}
}
