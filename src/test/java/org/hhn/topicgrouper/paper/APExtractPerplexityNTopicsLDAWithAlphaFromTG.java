package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.AlphaBetaFromTGCollector;
import org.hhn.topicgrouper.lda.impl.LDAFullBetaGibbsSampler;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.tg.impl.AbstractTopicGrouper;
import org.hhn.topicgrouper.tg.impl.TopicGrouperWithTreeSet;
import org.hhn.topicgrouper.tg.report.store.MapNode;
import org.hhn.topicgrouper.tg.report.store.WordInfo;

public class APExtractPerplexityNTopicsLDAWithAlphaFromTG extends
		APExtractPerplexityNTopics {
	protected final List<WordInfo<String>> wordInfosForNodeCache;
	protected final AlphaBetaFromTGCollector alphaBetaFromTGCollector;

	public APExtractPerplexityNTopicsLDAWithAlphaFromTG(Random random, int gibbsIterations,
			double concAlpha, double concBeta, boolean fast) {
		super(random, gibbsIterations, concAlpha, concBeta, fast);
		wordInfosForNodeCache = new ArrayList<WordInfo<String>>();
		alphaBetaFromTGCollector = new AlphaBetaFromTGCollector(concAlpha, concBeta, null) {
			protected String getSerializationFileName() {
				return APExtractPerplexityNTopicsLDAWithAlphaFromTG.this.getSerializationFileName();
			};
		};
	}
	
//	@Override
//	protected int nTopicFromStep(int step) {
//		return 70 + super.nTopicFromStep(step);
//	}

	@Override
	protected LDAGibbsSampler<String> createGibbsSampler(int step,
			DocumentProvider<String> documentProvider) {
		return alphaBetaFromTGCollector.createGibbsSampler(nTopicFromStep(step), documentProvider, random);
	}
	
	@Override
	protected String createLDACSVBaseFileName() {
		return "APExtractPerplexityNTopicsLDAWithAlphaFromTG" + createAlphaBetaFileNameExtension();
	}
	
	@Override
	protected PrintStream prepareTGPrintStream() throws IOException {
		return null;
	}

	@Override
	protected PrintStream prepareTGLikelihoodPrintStream() throws IOException {
		return null;
	}

	@Override
	protected void runTopicGrouper(final PrintStream pw3, final int step,
			final int repeat, final DocumentProvider<String> documentProvider,
			final DocumentProvider<String> testDocumentProvider,
			final double[] tgPerplexity, final double[] tgAcc) {
		if (step == -1 && repeat == 0 && mindMapSolutionReporter != null) {
			AbstractTopicGrouper<String> topicGrouper = new TopicGrouperWithTreeSet<String>(
					1, documentProvider, 1);
			topicGrouper.solve(mindMapSolutionReporter);
			allNodes = mindMapSolutionReporter.getAllNodes();
			saveFile(new File(getSerializationFileName()), allNodes);
		}
	}

	@Override
	protected void aggregateTGResults(PrintStream pw, int step,
			double[] tgPerplexity, double[] tgAcc) {
	}

	public static void main(String[] args) throws IOException {
		new APExtractPerplexityNTopicsLDAWithAlphaFromTG(new Random(42), 1000, 50,
				50, true).run(20, 1);
	}
}
