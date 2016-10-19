package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.lda.impl.LDAFullBetaGibbsSampler;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.tg.impl.AbstractTopicGrouper;
import org.hhn.topicgrouper.tg.impl.TopicGrouperWithTreeSet;
import org.hhn.topicgrouper.tg.report.MindMapSolutionReporter;
import org.hhn.topicgrouper.tg.report.store.MapNode;
import org.hhn.topicgrouper.tg.report.store.WordInfo;

public class APExtractPerplexityNTopicsLDAWithAlphaFromTG extends
		APExtractPerplexityNTopics {
	protected final List<WordInfo<String>> wordInfosForNodeCache;

	public APExtractPerplexityNTopicsLDAWithAlphaFromTG(Random random,
			double concAlpha, double concBeta, boolean fast) {
		super(random, concAlpha, concBeta, fast);
		wordInfosForNodeCache = new ArrayList<WordInfo<String>>();
	}
	
//	@Override
//	protected int nTopicFromStep(int step) {
//		return 70 + super.nTopicFromStep(step);
//	}

	@Override
	protected LDAGibbsSampler<String> createGibbsSampler(int step,
			DocumentProvider<String> documentProvider) {
		int topics = nTopicFromStep(step);
		List<MapNode<String>> nodes = getNodesByHistory(topics);
		// One word topics are not in nodes and should be avoided anyways. So we might end up with too few nodes.
		// Just search for more history nodes then (until the number is sufficient).
		for (int i = 1; nodes.size() < topics; i++) {
			nodes = getNodesByHistory(topics + i);
		}
		
		return new LDAFullBetaGibbsSampler<String>(documentProvider,
				createAlpha(topics, nodes), createFullBeta(topics,
						documentProvider, nodes), random);
	}

	protected double[] createAlpha(int topics, List<MapNode<String>> nodes) {
		double concentration = getConcAlpha();
		double[] alphaFromTG = new double[topics];
		double sum = 0;
		for (int i = 0; i < topics; i++) {
			alphaFromTG[i] = nodes.get(i).getTopicFrequency();
			sum += alphaFromTG[i];
		}

		for (int i = 0; i < topics; i++) {
			alphaFromTG[i] = concentration * alphaFromTG[i] / sum;
		}

		return alphaFromTG;
	}


	protected double[][] createFullBeta(int topics,
			DocumentProvider<String> documentProvider,
			List<MapNode<String>> nodes) {
		double concentration = getConcBeta();
		double[][] betaFromTG = new double[topics][];
		for (int i = 0; i < topics; i++) {
			MapNode<String> node = nodes.get(i);
			wordInfosForNodeCache.clear();
			collectLeafWordInfos(node, wordInfosForNodeCache);
			betaFromTG[i] = new double[documentProvider.getNumberOfWords()];
			int sum = 0;
			for (WordInfo<String> info : wordInfosForNodeCache) {
				int index = documentProvider.getIndex(info.getWord());
				if (index >= 0) {
					betaFromTG[i][index] = info.getFrequency();
					sum += info.getFrequency();
				}
			}
			for (int j = 0; j < betaFromTG[i].length; j++) {
				betaFromTG[i][j] = concentration * betaFromTG[i][j] / sum;
			}
		}
		return betaFromTG;
	}

	protected void collectLeafWordInfos(MapNode<String> node,
			List<WordInfo<String>> list) {
		if (node.getLeftNode() == null || node.getRightNode() == null) {
			list.addAll(node.getTopTopicWordInfos());
		} else {
			collectLeafWordInfos(node.getLeftNode(), list);
			collectLeafWordInfos(node.getRightNode(), list);
		}
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
		new APExtractPerplexityNTopicsLDAWithAlphaFromTG(new Random(42), 50,
				50, true).run(100, 20, 1);
	}
}
