package org.hhn.topicgrouper.paper.classfication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.classify.impl.vocab.VocabIGNBClassifier;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public class ReutersVocabIGClassificationExperiment {
	protected final LabelingDocumentProvider<String, String> testProvider;
	protected final LabelingDocumentProvider<String, String> trainingProvider;
	private final PrintStream output;

	public ReutersVocabIGClassificationExperiment() throws IOException {
		LabelingDocumentProvider<String, String>[] res = new LabelingDocumentProvider[2];
		createTrainingAndTestProvider(res);
		testProvider = res[0];
		trainingProvider = res[1];
		output = new PrintStream(new FileOutputStream(new File("./target/"
				+ getClass().getSimpleName() + ".csv")));
		output.println("topics; microAvg; macroAvg; lambda");
	}

	protected void createTrainingAndTestProvider(
			LabelingDocumentProvider<String, String>[] res) {
		// Use ModApte split:
		ReutersTGNaiveBayesExperiment.createModApteSplit(res);		
	}
	
	public void run(boolean optimize) {
		for (int topics = 1; topics <= 9; topics++) {
			runExperiment(topics, optimize);
		}
		for (int topics = 10; topics <= 99; topics += 10) {
			runExperiment(topics, optimize);
		}
		for (int topics = 100; topics <= 1000; topics += 100) {
			runExperiment(topics, optimize);
		}
	}

	protected void runExperiment(final int topics, boolean optimize) {
		AbstractTopicBasedNBClassifier<String, String> classifier = createClassifier(topics, trainingProvider);
		classifier.train(trainingProvider);
		if (optimize) {
			classifier.optimizeLambda(0, 0.5, trainingProvider, 10, true);
		}
		double microAvg = classifier.test(testProvider, true);
		double macroAvg = classifier.test(testProvider, false);
		System.out.println(topics + "; " + microAvg + "; " + macroAvg + "; " + classifier.getSmoothingLambda());
		output.println(topics + "; " + microAvg + "; " + macroAvg + "; " + classifier.getSmoothingLambda());
	}
	
	protected AbstractTopicBasedNBClassifier<String, String> createClassifier(int topics, LabelingDocumentProvider<String, String> documentProvider) {
		return new VocabIGNBClassifier<String, String>(
				initialLambda(), documentProvider, topics);
	}
	
	protected double initialLambda() {
		return 0;
	}

	public static void main(String[] args) throws IOException {
		new ReutersVocabIGClassificationExperiment().run(false);
	}
}
