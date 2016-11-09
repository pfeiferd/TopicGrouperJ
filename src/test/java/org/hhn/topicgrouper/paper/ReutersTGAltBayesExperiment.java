package org.hhn.topicgrouper.paper;

import gnu.trove.iterator.TIntIterator;

import java.io.IOException;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedAltBayesClassifier;
import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.tg.TGSolution;

public class ReutersTGAltBayesExperiment extends
		AbstractTGClassificationExperiment {
	public ReutersTGAltBayesExperiment() throws IOException {
		super();
	}

	@Override
	protected SupervisedDocumentClassifier<String, String> createClassifier(
			final TGSolution<String> solution) {
		int nt = solution.getNumberOfTopics();
		if (nt % 100 == 0 || nt < 300) {
			final int[] topicIds = solution.getTopicIds();
			return new AbstractTopicBasedAltBayesClassifier<String, String>() {
				@Override
				protected int[] getTopicIndices() {
					return topicIds;
				}
				
				@Override
				protected int getTopicIndex(int wordIndex) {
					return solution.getTopicForWord(wordIndex);
				}
				
				@Override
				protected double getTopicFrequency(int topic) {
					return solution.getTopicFrequency(topicIds[topic]);
				}
			};
		} else {
			return null;
		}
	}

	public static void main(String[] args) throws IOException {
		new ReutersTGAltBayesExperiment().run();
	}
}
