package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.lda.impl.LDAFullBetaGibbsSampler;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.tg.report.store.MapNode;
import org.hhn.topicgrouper.tg.report.store.WordInfo;

public class AlphaBetaFromTGCollector {
	protected final List<WordInfo<String>> wordInfosForNodeCache;
	protected List<MapNode<String>> allNodes;
	protected final double concAlpha;
	protected final double concBeta;
	protected final String serFileName;

	public AlphaBetaFromTGCollector(double concAlpha, double concBeta, String serFileName) {
		wordInfosForNodeCache = new ArrayList<WordInfo<String>>();
		this.concAlpha = concAlpha;
		this.concBeta = concBeta;
		this.serFileName = serFileName;
		allNodes = loadFile(new File(getSerializationFileName()));
	}

	protected String getSerializationFileName() {
		return serFileName;
	}
	
	public LDAGibbsSampler<String> createGibbsSampler(int topics,
			DocumentProvider<String> documentProvider, Random random) {
		List<MapNode<String>> nodes = getNodesByHistory(topics);
		// One word topics are not in nodes and should be avoided anyways. So we
		// might end up with too few nodes.
		// Just search for more history nodes then (until the number is
		// sufficient).
		for (int i = 1; nodes.size() < topics; i++) {
			nodes = getNodesByHistory(topics + i);
		}

		return new LDAFullBetaGibbsSampler<String>(documentProvider,
				createAlpha(topics, nodes), createFullBeta(topics,
						documentProvider, nodes), random);
	}

	@SuppressWarnings("unchecked")
	protected List<MapNode<String>> loadFile(File file) {
		try {
			ObjectInputStream oi = new ObjectInputStream(new FileInputStream(
					file));
			List<MapNode<String>> res = (List<MapNode<String>>) oi.readObject();
			oi.close();
			return res;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected List<MapNode<String>> getNodesByHistory(int topicId) {
		List<MapNode<String>> res = new ArrayList<MapNode<String>>();
		if (allNodes != null) {
			BitSet bitSet = new BitSet(allNodes.size() - topicId);
			for (int i = topicId; i <= allNodes.size(); i++) {
				if (!bitSet.get(i - topicId)) {
					int nodeIndex = allNodes.size() - i;
					MapNode<String> node = allNodes.get(nodeIndex);
					res.add(node);
					markDeps(node, bitSet, topicId);
				}
			}
		}
		return res;
	}

	public double getConcAlpha() {
		return concAlpha;
	}

	public double getConcBeta() {
		return concBeta;
	}

	protected void markDeps(MapNode<String> node, BitSet bitSet, int topicId) {
		if (node == null || node.getId() < 1) {
			return;
		}
		bitSet.set(node.getId() - topicId);
		markDeps(node.getLeftNode(), bitSet, topicId);
		markDeps(node.getRightNode(), bitSet, topicId);
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
