package org.hhn.topicgrouper.paper.classfication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;
import org.hhn.topicgrouper.doc.impl.LabelingHoldOutSplitter;
import org.hhn.topicgrouper.eval.Reuters21578;
import org.hhn.topicgrouper.tg.TGSolution;

public class ReutersTGNaiveBayesExperiment extends
		AbstractTGClassificationExperiment {
	private final PrintStream output;
	private final PrintStream outputOpt;

	public ReutersTGNaiveBayesExperiment() throws IOException {
		super();
		output = new PrintStream(new FileOutputStream(new File("./target/"
				+ getClass().getSimpleName() + ".csv")));
		output.println("topics; microAvg; macroAvg");
		outputOpt = new PrintStream(new FileOutputStream(new File("./target/"
				+ getClass().getSimpleName() + "Opt.csv")));
		outputOpt.println("topics; microAvg; macroAvg");
	}

	@Override
	protected void createTrainingAndTestProvider(
			LabelingDocumentProvider<String, String>[] res) {
		createModApteSplit(res);
	}

	public static void createModApteSplit(
			LabelingDocumentProvider<String, String>[] res) {
		Reuters21578 reuters = new Reuters21578(true);
		LabelingDocumentProvider<String, String> trainingData = reuters
				.getCorpusDocumentProvider(new File(
						"src/test/resources/reuters21578"), true, false);
		LabelingDocumentProvider<String, String> trainingProvider = new LabelingHoldOutSplitter<String, String>(
				new Random(42), trainingData, 0, 5, 10).getRest();
		LabelingDocumentProvider<String, String> testData = reuters
				.getCorpusDocumentProvider(new File(
						"src/test/resources/reuters21578"), false, true);
		LabelingDocumentProvider<String, String> testProvider = new LabelingHoldOutSplitter<String, String>(
				new Random(42), testData, 1, 0, 5,
				(DefaultVocab<String>) trainingProvider.getVocab())
				.getHoldOut();

		System.out.println("Test docs: " + testProvider.getDocuments().size());
		System.out.println("Trainig docs: "
				+ trainingProvider.getDocuments().size());
		System.out.println("Vocab: "
				+ trainingProvider.getVocab().getNumberOfWords());

		res[0] = testProvider;
		res[1] = trainingProvider;
	}

	@Override
	protected SupervisedDocumentClassifier<String, String> createClassifier(
			final TGSolution<String> solution) {
		int nt = solution.getNumberOfTopics();
		if (nt % 100 == 0 || nt < 300) {
			final int[] topicIds = solution.getTopicIds();
			return new AbstractTopicBasedNBClassifier<String, String>(1) {
				@Override
				protected int[] getTopicIndices() {
					return topicIds;
				}

				@Override
				protected int getTopicIndex(int wordIndex) {
					return solution.getTopicForWord(wordIndex);
				}
			};
		} else {
			return null;
		}
	}

	@Override
	protected void printResult(PrintStream out, boolean optmized, int topics, double microAvg,
			double macroAvg) {
		if (optmized) {
			outputOpt.println(topics + "; " + microAvg + "; " + macroAvg);
		} else {
			super.printResult(out, optmized, topics, microAvg, macroAvg);
			output.println(topics + "; " + microAvg + "; " + macroAvg);
		}
	}
	
	@Override
	protected void done() {
		super.done();
		output.close();
		outputOpt.close();
	}

	public static void main(String[] args) throws IOException {
		new ReutersTGNaiveBayesExperiment().run();
	}
}
