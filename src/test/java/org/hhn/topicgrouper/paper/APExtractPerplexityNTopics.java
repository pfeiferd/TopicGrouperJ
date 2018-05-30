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
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.FiftyFiftyDocumentSplitter;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolutionListenerMultiplexer;
import org.hhn.topicgrouper.tg.impl.AbstractTopicGrouper;
import org.hhn.topicgrouper.tg.impl.EHACTopicGrouper;
import org.hhn.topicgrouper.tg.report.MindMapSolutionReporter;
import org.hhn.topicgrouper.tg.report.store.MapNode;
import org.hhn.topicgrouper.tg.validation.TGPerplexityCalculator;

public class APExtractPerplexityNTopics extends TWCPerplexityErrorRateNDocs {
	protected final DocumentProvider<String> basicDocumentProvider;
	protected double[] tgPerplexityPerNTopics;
	protected int maxTopicsToReport;
	protected HoldOutSplitter<String> holdOutSplitter;

	protected final MindMapSolutionReporter<String> mindMapSolutionReporter;
	protected List<MapNode<String>> allNodes;

	public APExtractPerplexityNTopics(Random random, int gibbsIterations,
			boolean fast) {
		super(random, gibbsIterations);
		basicDocumentProvider = initBasicDocumentProvider();
		if (fast) {
			allNodes = loadFile(new File(getSerializationFileName()));
			mindMapSolutionReporter = null;
		} else {
			mindMapSolutionReporter = new MindMapSolutionReporter<String>(10,
					false, 1.1, 20);
		}
	}

	protected DocumentProvider<String> initBasicDocumentProvider() {
		return new APParser(true, true).getCorpusDocumentProvider(new File(
				"src/test/resources/ap-corpus/extract/ap.txt"));
	}

	@Override
	protected TGPerplexityCalculator<String> initTGPerplexityCalculator() {
		return new TGPerplexityCalculator<String>(false,
				createDocumentSplitter(), 1) {
			// protected double alpha = 0.5;
			//
			// @Override
			// protected double smoothedPtd(int topicFrInDoc, int docSize,
			// int wordIndex, int topicIndex, TGSolution<String> s) {
			// // Weighted lidstone smoothing (performs about as well as the
			// smoothing in the super method).
			// return (topicFrInDoc + (alpha * s.getNumberOfTopics()
			// * s.getTopicFrequency(topicIndex)) / s.getSize())
			// / (docSize + alpha * s.getNumberOfTopics());
			// }

			@Override
			protected double getSmoothingLambda(TGSolution<String> s) {
				return 0.5;
			}
		};
	}

	// @Override
	// protected AbstractLDAPerplexityCalculator<String>
	// createLDAPerplexityCalculator2(
	// int gibbsIterations) {
	// return new MarginalProbEstimator<String>(random,
	// createDocumentSplitter(), 1000, false);
	// }

	@Override
	protected DocumentSplitter<String> createDocumentSplitter() {
		return new FiftyFiftyDocumentSplitter<String>(new Random(45));
		// return new EachWordDocumentSplitter<String>(true);
	}

	@Override
	public void run(int steps, int avgC) throws IOException {
		int maxReportedTopics = nTopicFromStep(steps);
		tgPerplexityPerNTopics = new double[maxReportedTopics];
		super.run(steps, avgC);
	}

	protected String createAlphaBetaFileNameExtension() {
		return "_a" + getConcAlpha() + "_b" + getSymmetricBeta();
	}

	protected int nTopicFromStep(int step) {
		return (step + 1) * 10;
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

	protected String getSerializationFileName() {
		return "./target/APExtract.ser";
	}

	@Override
	protected HoldOutSplitter<String> createHoldoutSplitter(
			DocumentProvider<String> documentProvider, int step, int repeat) {
		// Use always the same hold out splitter at every step.
		if (holdOutSplitter == null) {
			holdOutSplitter = new HoldOutSplitter<String>(random,
					documentProvider, 0.1, 30);
		}
		return holdOutSplitter;
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider(int step,
			int repeat) {
		return basicDocumentProvider;
	}

	@Override
	protected LDAGibbsSampler<String> createGibbsSampler(int step,
			DocumentProvider<String> documentProvider) {
		int topics = nTopicFromStep(step);
		return new LDAGibbsSampler<String>(random, documentProvider,
				createAlpha(topics), createBeta(topics, documentProvider));
	}

	// Like in: http://psiexp.ss.uci.edu/research/papers/sciencetopics.pdf
	// and
	// http://stats.stackexchange.com/questions/59684/what-are-typical-values-to-use-for-alpha-and-beta-in-latent-dirichlet-allocation
	protected double[] createAlpha(int topics) {
		return LDAGibbsSampler.symmetricAlpha(getConcAlpha(), topics);
	}

	// Like in: http://psiexp.ss.uci.edu/research/papers/sciencetopics.pdf
	// and
	// http://stats.stackexchange.com/questions/59684/what-are-typical-values-to-use-for-alpha-and-beta-in-latent-dirichlet-allocation
	protected double createBeta(int topics,
			DocumentProvider<String> documentProvider) {
		return getSymmetricBeta();
	}

	public double getConcAlpha() {
		return 50;
	}

	public double getSymmetricBeta() {
		return 0.1;
	}

	@Override
	protected void printLDACSVFileHeader(PrintStream pw) {
		pw.print("ntopics;");
		pw.print("perplexity;");
		pw.println("perplexityFoldIn;");
	}

	@Override
	protected String createLDACSVBaseFileName() {
		return "APExtractPerplexityNTopicsLDA";
	}

	@Override
	protected void printTGCSVFileHeader(PrintStream pw) {
		pw.print("ntopics;");
		pw.println("perplexity;");
	}

	@Override
	protected String createTGCSVBaseFileName() {
		return "APExtractPerplexityNTopicsTG";
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
		if (step == -1 && repeat == 0) {
			AbstractTopicGrouper<String> topicGrouper = new EHACTopicGrouper<String>(
					1, documentProvider, 1);

			TGSolutionListener<String> tgSolutionListener = new TGSolutionListener<String>() {
				@Override
				public void updatedSolution(int newTopicIndex,
						int oldTopicIndex, double improvement, int t1Size,
						int t2Size, final TGSolution<String> solution) {
					int topics = solution.getNumberOfTopics();
					if (topics <= tgPerplexityPerNTopics.length) {
						tgPerplexityPerNTopics[topics - 1] = computeTGPerplexity(
								solution, testDocumentProvider);
					}
				}

				@Override
				public void initialized(TGSolution<String> initialSolution) {
				}

				@Override
				public void initalizing(double percentage) {
				}

				@Override
				public void done() {
				}

				@Override
				public void beforeInitialization(int maxTopics, int documents) {
				}
			};
			if (mindMapSolutionReporter != null) {
				TGSolutionListenerMultiplexer<String> multiplexer = new TGSolutionListenerMultiplexer<String>();
				multiplexer.addSolutionListener(mindMapSolutionReporter);
				multiplexer.addSolutionListener(tgSolutionListener);
				topicGrouper.solve(multiplexer);
				allNodes = mindMapSolutionReporter.getAllNodes();
				saveFile(new File(getSerializationFileName()), allNodes);
			} else {
				topicGrouper.solve(tgSolutionListener);
			}
		}
	}

	@Override
	protected void runLDAGibbsSampler(int step, int repeat,
			int gibbsIterations, DocumentProvider<String> documentProvider,
			DocumentProvider<String> testDocumentProvider,
			double[] perplexity1, double[] perplexity2, double[] perplexity3,
			double[] acc) {
		if (repeat == 0) {
			final LDAGibbsSampler<String> gibbsSampler = createGibbsSampler(
					step, documentProvider);

			gibbsSampler.solve(gibbsIterations / 4, gibbsIterations,
					new BasicLDAResultReporter<String>(System.out, 10));

			calc1.setTopicModeler(gibbsSampler);
			perplexity1[repeat] = calc1.computePerplexity(testDocumentProvider);

			calc2.setTopicModeler(gibbsSampler);
			perplexity2[repeat] = calc2.computePerplexity(testDocumentProvider);

			calc2.setTopicModeler(gibbsSampler);
			perplexity3[repeat] = calc3.computePerplexity(testDocumentProvider);
		}
	}

	@Override
	protected void aggregateLDAResults(PrintStream pw, int step,
			double[] perplexity1, double[] perplexity2, double[] perplexity3,
			double[] acc) {
		pw.print(nTopicFromStep(step));
		pw.print("; ");
		pw.print(perplexity1[0]);
		pw.print("; ");
		pw.print(perplexity2[0]);
		pw.println("; ");
	}

	@Override
	protected void aggregateTGResults(PrintStream pw, int step,
			double[] tgPerplexity, double[] tgAcc) {
		if (step == 0) {
			for (int i = 0; i < tgPerplexityPerNTopics.length; i++) {
				pw.print(i + 1);
				pw.print("; ");
				pw.print(tgPerplexityPerNTopics[i]);
				pw.println("; ");
			}
		}
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

	public static void main(String[] args) throws IOException {
		new APExtractPerplexityNTopics(new Random(42), 2000, false).run(20, 1);
	}
}
