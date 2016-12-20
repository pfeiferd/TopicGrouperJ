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
		// Use ModApte split:
		LabelingDocumentProvider<String, String>[] res = new LabelingDocumentProvider[2];
		ReutersTGNaiveBayesExperiment.createModApteSplit(res);
		testProvider = res[0];
		trainingProvider = res[1];

		output = new PrintStream(new FileOutputStream(new File("./target/"
				+ getClass().getSimpleName() + ".csv")));
		output.println("topics; microAvg; macroAvg");
	}

	public void run(boolean optimize) {
		for (int topics = 1; topics <= 9; topics++) {
			runExperiment(topics, optimize);
		}
		for (int topics = 10; topics <= 1000; topics += 10) {
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
		System.out.println(topics + "; " + microAvg + "; " + macroAvg);
		output.println(topics + "; " + microAvg + "; " + macroAvg);
	}
	
	protected AbstractTopicBasedNBClassifier<String, String> createClassifier(int topics, LabelingDocumentProvider<String, String> documentProvider) {
		return new VocabIGNBClassifier<String, String>(
				0.3, documentProvider, topics);
	}

	public static void main(String[] args) throws IOException {
		new ReutersVocabIGClassificationExperiment().run(false);
	}
}
