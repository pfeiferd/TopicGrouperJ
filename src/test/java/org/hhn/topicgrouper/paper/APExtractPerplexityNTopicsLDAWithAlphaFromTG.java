package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
	private final double concAlpha;
	private final double concBeta;
	private final List<WordInfo<String>> wordInfosForNodeCache;

	private final MindMapSolutionReporter<String> mindMapSolutionReporter;
	private List<MapNode<String>> allNodes;

	public APExtractPerplexityNTopicsLDAWithAlphaFromTG(Random random,
			double concAlpha, double concBeta, boolean fast) {
		super(random);
		this.concAlpha = concAlpha;
		this.concBeta = concBeta;
		wordInfosForNodeCache = new ArrayList<WordInfo<String>>();
		if (fast) {
			allNodes = loadFile(new File(getSerializationFileName()));
			mindMapSolutionReporter = null;
		} else {
			mindMapSolutionReporter = new MindMapSolutionReporter<String>(10,
					false, 1.1, 20);
		}
	}

	@Override
	public void run(int gibbsIterations, int steps, int avgC)
			throws IOException {
		super.run(gibbsIterations, steps, avgC);
	}

	@Override
	protected LDAGibbsSampler<String> createGibbsSampler(int step,
			DocumentProvider<String> documentProvider) {
		int topics = nTopicFromStep(step);
		List<MapNode<String>> nodes = getNodesByHistory(topics);
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

	protected void markDeps(MapNode<String> node, BitSet bitSet, int topicId) {
		if (node == null || node.getId() < 1) {
			return;
		}
		bitSet.set(node.getId() - topicId);
		markDeps(node.getLeftNode(), bitSet, topicId);
		markDeps(node.getRightNode(), bitSet, topicId);
	}

	public double getConcAlpha() {
		return concAlpha;
	}

	public double getConcBeta() {
		return concBeta;
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
	protected PrintStream prepareLDAPrintStream() throws IOException {
		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/APExtractPerplexityNTopicsLDAWithAlphaFromTG.csv")));

		pw.print("ntopics;");
		pw.print("perplexity;");
		pw.println("perplexityFoldIn;");

		return pw;
	}

	@Override
	protected PrintStream prepareTGPrintStream() throws IOException {
		return null;
	}

	@Override
	protected PrintStream prepareTGLikelihoodPrintStream() throws IOException {
		return null;
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

	protected void saveFile(File file, Object o) {
		try {
			ObjectOutputStream oo = new ObjectOutputStream(
					new FileOutputStream(file));
			oo.writeObject(o);
			oo.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void runTopicGrouper(final PrintStream pw3, final int step,
			final int repeat, final DocumentProvider<String> documentProvider,
			final DocumentProvider<String> testDocumentProvider,
			final double[] tgPerplexity, final double[] tgAcc) {
		if (step == 0 && repeat == 0 && mindMapSolutionReporter != null) {
			AbstractTopicGrouper<String> topicGrouper = new TopicGrouperWithTreeSet<String>(
					1, documentProvider, 1);
			topicGrouper.solve(mindMapSolutionReporter);
			allNodes = mindMapSolutionReporter.getAllNodes();
			saveFile(new File(getSerializationFileName()), allNodes);
		}
	}

	protected String getSerializationFileName() {
		return "./target/APExtract.ser";
	}

	@Override
	protected void aggregateTGResults(PrintStream pw, int step,
			double[] tgPerplexity, double[] tgAcc) {
	}

	public static void main(String[] args) throws IOException {
		new APExtractPerplexityNTopicsLDAWithAlphaFromTG(new Random(42), 150,
				20, false).run(1000, 20, 1);
	}
}
