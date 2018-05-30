package org.hhn.topicgrouper.paper.perplexity.twc;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;
import org.hhn.topicgrouper.validation.UnigramModeler;

public class TWCUnigramPerplexityChangeAlphaExp extends
		TWCPLSAPerplexityChangeAlphaExp {
	public TWCUnigramPerplexityChangeAlphaExp() {
		super(1);
	}
	
	protected int getSeed() {
		return 42;
	}

	@Override
	protected AbstractTopicModeler<String> createTopicModeler(int topics,
			DocumentProvider<String> documentProvider, boolean optimize) {
		return new UnigramModeler<String>(documentProvider, topics);
	}

	@Override
	protected void trainTopicModeler(AbstractTopicModeler<String> modeler,
			DocumentProvider<String> documentProvider, boolean optimize) {
	}

	public static void main(String[] args) {
		new TWCUnigramPerplexityChangeAlphaExp().run(false);
	}
}
