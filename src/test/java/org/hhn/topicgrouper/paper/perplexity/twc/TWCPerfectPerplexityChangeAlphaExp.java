package org.hhn.topicgrouper.paper.perplexity.twc;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;
import org.hhn.topicgrouper.validation.AbstractTopicModelerWithProvider;

public class TWCPerfectPerplexityChangeAlphaExp extends
		TWCPLSAPerplexityChangeAlphaExp {
	public TWCPerfectPerplexityChangeAlphaExp() {
		super(1);
	}
	
	protected int getSeed() {
		return 42;
	}

	@Override
	protected AbstractTopicModeler<String> createTopicModeler(int topics,
			DocumentProvider<String> documentProvider, boolean optimize) {
		return new PerfectTWCTopicModeler(documentProvider, topics);
	}

	@Override
	protected void trainTopicModeler(AbstractTopicModeler<String> modeler,
			DocumentProvider<String> documentProvider, boolean optimize) {
	}

	public static void main(String[] args) {
		new TWCPerfectPerplexityChangeAlphaExp().run(false);
	}
	
	protected static class PerfectTWCTopicModeler extends AbstractTopicModelerWithProvider<String> {
		private double[] alphaBase;
		
		public PerfectTWCTopicModeler(DocumentProvider<String> provider,
				int nTopics) {
			super(null, provider, nTopics);
			alphaBase = new double[4];
			int sumAll = 0;
			for (int i = 0; i < nTopics; i++) {
				double sum = 0;
				for (int j = 0; j < nWords; j++) {
					String w = provider.getVocab().getWord(j);
					int wi = Integer.valueOf(w);
					if (i == wi / 100) { 
						phi[i][j] = provider.getWordFrequency(j);
						sum += phi[i][j];
					}
				}
				alphaBase[i] = sum;
				sumAll += sum;
				for (int j = 0; j < nWords; j++) {
					phi[i][j] = phi[i][j] / sum;
				}
			}
			for (int i = 0; i < nTopics; i++) {
				alphaBase[i] = alphaBase[i] / sumAll;
			}
		}
		
		@Override
		public double getAlpha(int i) {
			return getAlphaConc() * alphaBase[i];
		}

		@Override
		public double getAlphaConc() {
			return 6.5;
		}

		@Override
		public void setAlphaConc(double alphaConc) {
		}
	}
}
