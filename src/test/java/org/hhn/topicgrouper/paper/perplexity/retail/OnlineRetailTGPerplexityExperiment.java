package org.hhn.topicgrouper.paper.perplexity.retail;

import java.io.File;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.OnlineRetailParser;
import org.hhn.topicgrouper.paper.perplexity.AbstractTGPerplexityNTopicsExperiment;
import org.hhn.topicgrouper.util.StatsReport;

public class OnlineRetailTGPerplexityExperiment extends
		AbstractTGPerplexityNTopicsExperiment<String> {
	public OnlineRetailTGPerplexityExperiment() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		createSplit(res);
	}
	
//	@Override
//	protected int initTopicEvalSteps() {
//		return 20;
//	}

	@Override
	protected DocumentSplitter<String> createDocumentSplitter() {
		return new DefaultDocumentSplitter<String>();
		// return new FiftyFiftyDocumentSplitter<String>(new Random(42));
	}

	public static void createSplit(DocumentProvider<String>[] res) {
		OnlineRetailParser parser = new OnlineRetailParser();
		DocumentProvider<String> provider = parser.getCorpusDocumentProvider(
				new File("src/test/resources/OnlineRetail/Online Retail.csv"),
				true, 25, false);

		// We work with only 10% of the date from the start...
//		HoldOutSplitter<String> splitter1 = new HoldOutSplitter<String>(
//				new Random(42), provider, 0.8, 0);

		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), provider, 0.1, 10);
		res[0] = splitter.getHoldOut();
		res[1] = splitter.getRest();

		StatsReport.report(res[1], System.out);
	}

	public static void main(String[] args) {
		new OnlineRetailTGPerplexityExperiment().run();
	}
}
