package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.report.store.MapNode;

public class APExtractPerplexityNTopicsLDAWithAlphaFromTG extends APExtractPerplexityNTopics {
	private final List<MapNode<String>> nodes;
	
	public APExtractPerplexityNTopicsLDAWithAlphaFromTG(Random random) {
		super(random);
		nodes = loadFile(new File("./target/APExtract.ser"));
	}

	@Override
	public void run(int gibbsIterations, int steps, int avgC)
			throws IOException {
		super.run(gibbsIterations, steps, avgC);
	}

	protected double[] createAlpha(int topics) {
		double[] alphaFromTG = new double[topics];
		double sum = 0;
		for (int i = 0; i < topics; i++) {
			alphaFromTG[i] = nodes.get(nodes.size() - topics - i).getTopicFrequency();
			sum += alphaFromTG[i];
		}
		for (int i = 0; i < topics; i++) {
			alphaFromTG[i] /= sum;
		}
		
		return alphaFromTG;
	}

	protected double createBeta(int topics) {
		return 1;
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
	
	@Override
	protected void runTopicGrouper(final PrintStream pw3, final int step,
			final int repeat, final DocumentProvider<String> documentProvider,
			final DocumentProvider<String> testDocumentProvider,
			final double[] tgPerplexity, final double[] tgAcc) {
//		if (step == 0 && repeat == 0) {
//			AbstractTopicGrouper<String> topicGrouper = new TopicGrouperWithTreeSet<String>(
//					1, documentProvider, 1);
//			topicGrouper.solve(new TGSolutionListener<String>() {
//				@Override
//				public void updatedSolution(int newTopicIndex,
//						int oldTopicIndex, double improvement, int t1Size,
//						int t2Size, final TGSolution<String> solution) {
//					int topics = solution.getNumberOfTopics();
//					if (topics <= tgPerplexityPerNTopics.length) {
//						tgPerplexityPerNTopics[topics - 1] = computeTGPerplexity(
//								solution, testDocumentProvider);
//					}
//					if (topics <= tgAlphaPerNTopics.length) {
//						int[] topicId = solution.getTopicIds();
//						tgAlphaPerNTopics[topics - 1] = new double[topics];
//						double sum = 0;
//						for (int i = 0; i < topics; i++) {
//							tgAlphaPerNTopics[topics - 1][i] = solution
//									.getTopicFrequency(topicId[i]);
//							sum += tgAlphaPerNTopics[topics - 1][i];
//						}
//						for (int i = 0; i < topics; i++) {
//							tgAlphaPerNTopics[topics - 1][i] /= sum;
//						}
//					}
//				}
//
//				@Override
//				public void initialized(TGSolution<String> initialSolution) {
//				}
//
//				@Override
//				public void initalizing(double percentage) {
//				}
//
//				@Override
//				public void done() {
//				}
//
//				@Override
//				public void beforeInitialization(int maxTopics, int documents) {
//				}
//			});
//		}
	}


	@Override
	protected void aggregateTGResults(PrintStream pw, int step,
			double[] tgPerplexity, double[] tgAcc) {
	}

	public static void main(String[] args) throws IOException {
		new APExtractPerplexityNTopicsLDAWithAlphaFromTG(new Random(42)).run(1000, 20, 1);
	}
}
