package org.hhn.topicgrouper.paper;

import java.io.IOException;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedSVMClassifier;
import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedTfIdfClassifier;
import org.hhn.topicgrouper.tg.TGSolution;

public class ReutersTGSVMExperiment extends AbstractTGClassificationExperiment {
	public ReutersTGSVMExperiment() throws IOException {
		super();
	}

	@Override
	protected SupervisedDocumentClassifier<String, String> createClassifier(
			final TGSolution<String> solution) {
		int nt = solution.getNumberOfTopics();
		if (nt % 500 == 0 || (nt < 300 && nt % 10 == 0)) {
			final int[] topicsIds = solution.getTopicIds();

			return new AbstractTopicBasedSVMClassifier<String, String>() {
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
		new ReutersTGSVMExperiment().run();
	}
}
