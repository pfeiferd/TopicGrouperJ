package org.hhn.topicgrouper.paper.classfication;

import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.AlphaBetaFromTGCollector;
import org.hhn.topicgrouper.lda.impl.LDAFullBetaGibbsSampler;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;

public class ReutersLDAWithTGParamsClassificationExperiment extends ReutersLDAClassificationExperiment {
	protected final AlphaBetaFromTGCollector alphaBetaFromTGCollector;

	public ReutersLDAWithTGParamsClassificationExperiment() throws IOException {
		super();
		alphaBetaFromTGCollector = new AlphaBetaFromTGCollector("./target/ReutersTGNaiveBayesExperiment.ser");
	}
	
	@Override
	protected LDAGibbsSampler<String> createGibbsSampler(int topics,
			DocumentProvider<String> documentProvider, boolean optimizeAlphaBeta) {
		LDAFullBetaGibbsSampler<String> ldaGibbsSampler = alphaBetaFromTGCollector.createGibbsSampler(topics, documentProvider, new Random(42));
		ldaGibbsSampler.setUpdateAlphaBeta(optimizeAlphaBeta);
		ldaGibbsSampler.setUpdatePrecisionOnly(true);
		
		return ldaGibbsSampler;
	}

	public static void main(String[] args) throws IOException {
		new ReutersLDAWithTGParamsClassificationExperiment().run(true);
	}
}
