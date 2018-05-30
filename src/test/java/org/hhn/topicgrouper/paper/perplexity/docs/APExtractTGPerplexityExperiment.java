package org.hhn.topicgrouper.paper.perplexity.docs;

import java.io.File;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.FiftyFiftyDocumentSplitter;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.paper.perplexity.AbstractTGPerplexityNTopicsExperiment;

public class APExtractTGPerplexityExperiment extends
		AbstractTGPerplexityNTopicsExperiment<String> {
	public APExtractTGPerplexityExperiment() {
		super(AllPerplexityExperimentsRunner.MAX_EVAL_TOPICS);
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		createSplit(res);
	}

//	@Override
//	protected DocumentSplitter<String> createDocumentSplitter() {
//		return new FiftyFiftyDocumentSplitter<String>(new Random(42));
//	}

	public static void createSplit(DocumentProvider<String>[] res) {
		DocumentProvider<String> provider = new APParser(true, true)
				.getCorpusDocumentProvider(new File(
						"src/test/resources/ap-corpus/extract/ap.txt"));

		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), provider, 0.1, 10);
		res[0] = splitter.getHoldOut();
		res[1] = splitter.getRest();
	}

	public static void main(String[] args) {
		new APExtractTGPerplexityExperiment().run();
	}
}
