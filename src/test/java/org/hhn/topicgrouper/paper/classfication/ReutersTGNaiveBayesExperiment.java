package org.hhn.topicgrouper.paper.classfication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.classify.impl.tg.TGNBClassifier;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;
import org.hhn.topicgrouper.doc.impl.LabelingHoldOutSplitter;
import org.hhn.topicgrouper.eval.Reuters21578;
import org.hhn.topicgrouper.tg.TGSolution;

public class ReutersTGNaiveBayesExperiment extends
		AbstractTGClassificationExperiment {

	public ReutersTGNaiveBayesExperiment() throws IOException {
		super();
	}

	@Override
	protected void createTrainingAndTestProvider(
			LabelingDocumentProvider<String, String>[] res) {
		createModApteSplit(res);
	}

	public static void createModApteSplit(
			LabelingDocumentProvider<String, String>[] res) {
		Reuters21578 reuters = new Reuters21578(true); // Excluding stop words.
		LabelingDocumentProvider<String, String> trainingData = reuters
				.getCorpusDocumentProvider(new File(
						"src/test/resources/reuters21578"), true, false);
		LabelingDocumentProvider<String, String> trainingProvider = new LabelingHoldOutSplitter<String, String>(
				new Random(42), trainingData, 0, 3, 10).getRest();
		LabelingDocumentProvider<String, String> testData = reuters
				.getCorpusDocumentProvider(new File(
						"src/test/resources/reuters21578"), false, true);
		LabelingDocumentProvider<String, String> testProvider = new LabelingHoldOutSplitter<String, String>(
				new Random(42), testData, 1, 0, testData.getAllLabels(),
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
			final TGSolution<String> solution, boolean optimizeLambda) {
		int nt = solution.getNumberOfTopics();
		if (nt < 10 || (nt < 100 && nt % 10 == 0)
				|| (nt < 1000 && nt % 100 == 0)
				|| (nt < 10000 && nt % 1000 == 0)) {
			return new TGNBClassifier<String, String>(
					initialLambda(optimizeLambda), solution);
		} else {
			return null;
		}
	}

	protected double initialLambda(boolean optimizeLambda) {
		return optimizeLambda ? 0.3 : 0;
	}

	public static void main(String[] args) throws IOException {
		new ReutersTGNaiveBayesExperiment().run();
	}
}
