package org.hhn.topicgrouper.paper;

import java.io.IOException;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedTfIdfClassifier;
import org.hhn.topicgrouper.tg.TGSolution;

public class ReutersTGTfIdfExperiment extends ReutersTGNaiveBayesExperiment {

	public ReutersTGTfIdfExperiment() throws IOException {
		super();
	}

	@Override
	protected SupervisedDocumentClassifier<String, String> createClassifier(
			final TGSolution<String> solution) {
		int nt = solution.getNumberOfTopics();
		if (nt % 100 == 0 || nt < 300) {
			final int[] topicsIds = solution.getTopicIds();

			return new AbstractTopicBasedTfIdfClassifier<String, String>() {
				@Override
				protected int getTopicIndex(int wordIndex) {
					return solution.getTopicForWord(wordIndex);
				}

				@Override
				protected int[] getTopicIndices() {
					return topicsIds;
				}
			};
		} else {
			return null;
		}
	}

	public static void main(String[] args) throws IOException {
		new ReutersTGTfIdfExperiment().run();
	}
}
