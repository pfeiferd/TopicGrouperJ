package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.tg.TGSolution;

public class APExtractPerplexityErrorRateNDocs extends
		TWCPerplexityErrorRateNDocs {
	protected final DocumentProvider<String> apExtractDocumentProvider;

	public APExtractPerplexityErrorRateNDocs(Random random) {
		super(random);
		apExtractDocumentProvider = new APParser(true, true)
				.getCorpusDocumentProvider(new File(
						"src/test/resources/ap-corpus/extract/ap.txt"));
	}

	protected int docsFromStep(int step) {
		return 100 * (step + 1);
	}
	
	@Override
	protected double computeTGAccuracy(TGSolution<String> solution,
			DocumentProvider<String> documentProvider) {
		// Not applicable...
		return 0;
	}

	@Override
	protected DocumentProvider<String> prepareTrainingDocumentProvider(int step,
			DocumentProvider<String> trainingDocumentProvider) {
		HoldOutSplitter<String> holdOutSplitter = new HoldOutSplitter<String>(
				random, trainingDocumentProvider, docsFromStep(step), 1);
		return holdOutSplitter.getHoldOut();
	}

	@Override
	protected HoldOutSplitter<String> createHoldoutSplitter(int step,
			DocumentProvider<String> documentProvider) {
		return new HoldOutSplitter<String>(random, documentProvider,
				0.33333333, 1);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider(int step) {
		return apExtractDocumentProvider;
	}

	public static void main(String[] args) throws IOException {
		new APExtractPerplexityErrorRateNDocs(new Random()).run(100, 30, 10);
	}
}
