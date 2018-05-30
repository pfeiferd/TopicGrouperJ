package org.hhn.topicgrouper.paper.perplexity.tgldamix;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.paper.perplexity.docs.APExtractTGPerplexityExperiment;

public class APExtractMixPerplexityExp extends
		AbstractLDATGMixPerplexityExperimentOpt<String> {
	public APExtractMixPerplexityExp() {
		super(100, APExtractTGPerplexityExperiment.class);
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		APExtractTGPerplexityExperiment.createSplit(res);
	}
	
	public static void main(String[] args) {
		new APExtractMixPerplexityExp().run(true);
	}
}
