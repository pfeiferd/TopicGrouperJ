package org.hhn.topicgrouper.paper.classfication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.hhn.topicgrouper.classify.impl.vocab.VocabNBClassifier;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public class ReutersVocabClassificationExperiment {
	protected final LabelingDocumentProvider<String, String> testProvider;
	protected final LabelingDocumentProvider<String, String> trainingProvider;
	private final PrintStream output;

	public ReutersVocabClassificationExperiment() throws IOException {
		// Use ModApte split:
		LabelingDocumentProvider<String, String>[] res = new LabelingDocumentProvider[2];
		ReutersTGNaiveBayesExperiment.createModApteSplit(res);
		testProvider = res[0];
		trainingProvider = res[1];

		output = new PrintStream(new FileOutputStream(new File("./target/"
				+ getClass().getSimpleName() + "Opt.csv")));
		output.println("topics; microAvg; macroAvg");
	}

	public void run(boolean optimize) {
		for (int topics = 1; topics <= 9; topics++) {
			runExperiment(topics, optimize);
		}
		for (int topics = 10; topics <= 500; topics += 10) {
			runExperiment(topics, optimize);
		}
	}

	protected void runExperiment(final int topics, boolean optimize) {
		VocabNBClassifier<String, String> classifier = new VocabNBClassifier<String, String>(
				0, trainingProvider, topics);
		classifier.train(trainingProvider);
		if (optimize) {
			classifier.optimizeLambda(0, 0.5, trainingProvider, 10, true);
		}
		double microAvg = classifier.test(testProvider, true);
		double macroAvg = classifier.test(testProvider, false);
		System.out.println(topics + "; " + microAvg + "; " + macroAvg);
		output.println(topics + "; " + microAvg + "; " + macroAvg);
	}

	public static void main(String[] args) throws IOException {
		new ReutersVocabClassificationExperiment().run(true);
	}
}
