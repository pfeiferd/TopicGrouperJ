package org.hhn.topicgrouper.paper.perplexity.twc;

import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.paper.perplexity.AbstractTGPerplexityNTopicsExperiment;

public class TWCTGPerplexityExperiment extends
		AbstractTGPerplexityNTopicsExperiment<String> {
	public TWCTGPerplexityExperiment() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		createSplit(res);
	}

	public static TWCLDAPaperDocumentGenerator createSplit(
			DocumentProvider<String>[] res) {
		return createSplit(res, 6000, new Random(42));
	}

	public static TWCLDAPaperDocumentGenerator createSplit(
			DocumentProvider<String>[] res, int docs, Random random) {
		TWCLDAPaperDocumentGenerator provider = new TWCLDAPaperDocumentGenerator(
				random, new double[] { 5, 0.5, 0.5, 0.5 }, docs, 100,
				100, 30, 30, 0, null, 0.5, 0.8);

		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), provider, 0.25, 3);
		res[0] = splitter.getHoldOut();
		res[1] = splitter.getRest();

		return provider;
	}

	public static void main(String[] args) {
		new TWCTGPerplexityExperiment().run();
	}
}
