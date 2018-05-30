package org.hhn.topicgrouper.paper.perplexity.twc;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;

public class TWCTGPerplexityChangeDocsExp extends TWCLDAPerplexityChangeDocsExp {
	public TWCTGPerplexityChangeDocsExp(int maxSteps) {
		super(1, maxSteps);
	}

	protected void runExperiment(final int step) {
		new AdaptedTWCTGLDAPerplexityExperiment() {
			protected int getStep() {
				return step;
			};
		}.run();
	}

	protected abstract class AdaptedTWCTGLDAPerplexityExperiment extends
			TWCTGPerplexityChangeAlphaExp {
		@Override
		protected void printHeader() {
		}

		@Override
		protected int initTopicEvalSteps() {
			return 1;
		}
		
		@Override
		protected TWCLDAPaperDocumentGenerator getTWCLDAPaperDocumentGenerator() {
			return TWCTGPerplexityChangeDocsExp.this.gen[getStep()];
		}		

		@Override
		protected void createTrainingAndTestProvider(
				DocumentProvider<String>[] res) {
			res[0] = TWCTGPerplexityChangeDocsExp.this.res[getStep()][0];
			res[1] = TWCTGPerplexityChangeDocsExp.this.res[getStep()][1];
		}

		protected abstract int getStep();

		@Override
		protected void printResult(int topics, double perplexity,
				double err) {
			perplexities[getStep()][0] = perplexity;
			errorRates[getStep()][0] = err;
		}
	}

	public static void main(String[] args) {
		new TWCTGPerplexityChangeDocsExp(20).run();
	}
}
