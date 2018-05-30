package org.hhn.topicgrouper.paper.perplexity.docs;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.paper.perplexity.AbstractLDAPerplexityNTopicsExperiment;

public class APExtractLDAPerplexityExperimentOpt extends
		AbstractLDAPerplexityNTopicsExperiment<String> {
	public APExtractLDAPerplexityExperimentOpt() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}

	@Override
	protected DocumentSplitter<String> createDocumentSplitter() {
		return new DefaultDocumentSplitter<String>();
	}
	
	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		APExtractTGPerplexityExperiment.createSplit(res);
	}
	
	public static void main(String[] args) {
		new APExtractLDAPerplexityExperimentOpt().run(true);
	}
}
