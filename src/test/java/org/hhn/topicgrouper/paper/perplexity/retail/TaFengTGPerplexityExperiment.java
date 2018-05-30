package org.hhn.topicgrouper.paper.perplexity.retail;

import java.io.File;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.TaFengGroceryParser;
import org.hhn.topicgrouper.paper.perplexity.AbstractTGPerplexityNTopicsExperiment;
import org.hhn.topicgrouper.util.StatsReport;

public class TaFengTGPerplexityExperiment extends
		AbstractTGPerplexityNTopicsExperiment<String> {
	public TaFengTGPerplexityExperiment() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		createSplit(res);
	}

	@Override
	protected int initTopicEvalSteps() {
		return 10;
	}

	public static void createSplit(DocumentProvider<String>[] res) {
		TaFengGroceryParser uciParser = new TaFengGroceryParser(new File(
				"./src/test/resources/TaFengGrocery"), "D01_D02_D11_D12", -1,
				true, false);
		DocumentProvider<String> provider = uciParser.getDocumentProvider();

		// We work with only 20% of the date from the start...
//		 HoldOutSplitter<String> splitter1 = new HoldOutSplitter<String>(
//		 new Random(42), provider, 0.8, 0);

		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), provider /*splitter1.getRest()*/, 0.1, 20);
		res[0] = splitter.getHoldOut();
		res[1] = splitter.getRest();

		StatsReport.report(res[1], System.out);
	}

	public static void main(String[] args) {
		new TaFengTGPerplexityExperiment().run();
	}
}
