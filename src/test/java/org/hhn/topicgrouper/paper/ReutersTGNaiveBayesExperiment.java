package org.hhn.topicgrouper.paper;

import java.io.IOException;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.tg.TGSolution;

public class ReutersTGNaiveBayesExperiment extends
		AbstractTGClassificationExperiment {
	public ReutersTGNaiveBayesExperiment() throws IOException {
		super();
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

	public static void main(String[] args) throws IOException {
		new ReutersTGNaiveBayesExperiment().run();
	}
}
