package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;

import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.classify.impl.vocab.VocabDFNBClassifier;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public class ReutersVocabDFClassificationExperiment extends ReutersVocabIGClassificationExperiment {	
	public ReutersVocabDFClassificationExperiment() throws IOException {
		super();
	}
	
	@Override
	protected AbstractTopicBasedNBClassifier<String, String> createClassifier(
			int topics,
			LabelingDocumentProvider<String, String> documentProvider) {
		return new VocabDFNBClassifier<String, String>(
				initialLambda(), documentProvider, topics);
	}

	public static void main(String[] args) throws IOException {
		new ReutersVocabDFClassificationExperiment().run(false);
	}
}
