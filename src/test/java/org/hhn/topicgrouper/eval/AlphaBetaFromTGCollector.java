package org.hhn.topicgrouper.eval;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.lda.impl.LDAFullBetaGibbsSampler;
import org.hhn.topicgrouper.tg.report.store.MapNode;
import org.hhn.topicgrouper.tg.report.store.MapNodeTGSolution;
import org.hhn.topicgrouper.tg.report.store.WordInfo;

public class AlphaBetaFromTGCollector {
	protected final List<WordInfo<String>> wordInfosForNodeCache;
	protected final MapNodeTGSolution<String> mapNodeTGSolution;
	protected final String serFileName;

	public AlphaBetaFromTGCollector(String serFileName) {
		wordInfosForNodeCache = new ArrayList<WordInfo<String>>();
		this.serFileName = serFileName;
		mapNodeTGSolution = new MapNodeTGSolution<String>(
				MapNodeTGSolution.<String> loadFile(new File(
						getSerializationFileName())));
	}

	protected String getSerializationFileName() {
		return serFileName;
	}

	public LDAFullBetaGibbsSampler<String> createGibbsSampler(int topics,
			DocumentProvider<String> documentProvider, Random random) {
		List<MapNode<String>> nodes = mapNodeTGSolution
				.getNodesByHistory(topics);

		return new LDAFullBetaGibbsSampler<String>(random, documentProvider,
				createAlpha(topics, nodes), createFullBeta(topics,
						documentProvider, nodes));
	}

	protected double getConcAlpha(int topics) {
		return 50d / topics;
	}

	protected double getConcBeta(int words) {
		return 0.1 * words;
	}

	protected double[] createAlpha(int topics, List<MapNode<String>> nodes) {
		double concentration = getConcAlpha(topics);
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
		double concentration = getConcBeta(documentProvider.getVocab()
				.getNumberOfWords());
		double[][] betaFromTG = new double[topics][];
		for (int i = 0; i < topics; i++) {
			MapNode<String> node = nodes.get(i);
			wordInfosForNodeCache.clear();
			collectLeafWordInfos(node, wordInfosForNodeCache);
			betaFromTG[i] = new double[documentProvider.getVocab()
					.getNumberOfWords()];
			int sum = 0;
			for (WordInfo<String> info : wordInfosForNodeCache) {
				int index = documentProvider.getVocab()
						.getIndex(info.getWord());
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

}
