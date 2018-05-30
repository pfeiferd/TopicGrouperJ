package org.hhn.topicgrouper.paper.perplexity.tgldamix;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.lda.impl.LDAFullBetaGibbsSampler;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.paper.perplexity.AbstractLDAPerplexityNTopicsExperiment;
import org.hhn.topicgrouper.tg.report.store.MapNode;
import org.hhn.topicgrouper.tg.report.store.MapNodeTGSolution;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;
import org.hhn.topicgrouper.validation.PeakValueOptimizer;
import org.hhn.topicgrouper.validation.PerplexityCalculatorLeftToRight;

public abstract class AbstractLDATGMixPerplexityExperimentOpt<T> extends
		AbstractLDAPerplexityNTopicsExperiment<T> {
	private final MapNodeTGSolution<T> mapNodeTGSolution;
	private final PerplexityCalculatorLeftToRight<T> perplexityCalculator;

	public AbstractLDATGMixPerplexityExperimentOpt(int maxTopicEval,
			Class<?> clazz) {
		super(maxTopicEval);
		List<MapNode<T>> allNodes = loadFile(new File("./target/"
				+ clazz.getSimpleName() + ".ser"));
		perplexityCalculator = new PerplexityCalculatorLeftToRight<T>(
				new Random(42), false);
		mapNodeTGSolution = new MapNodeTGSolution<T>(allNodes);
	}
	
	@Override
	protected boolean isProcessSolutionForTopics(int nTopics) {
		return nTopics == 50;
	}

	@SuppressWarnings("unchecked")
	protected List<MapNode<T>> loadFile(File file) {
		try {
			ObjectInputStream oi = new ObjectInputStream(new FileInputStream(
					file));
			List<MapNode<T>> res = (List<MapNode<T>>) oi.readObject();
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
	protected AbstractTopicModeler<T> createTopicModeler(int topics,
			DocumentProvider<T> documentProvider, boolean optimize) {
		LDAFullBetaGibbsSampler<T> ldaGibbsSampler = new LDAFullBetaGibbsSampler<T>(
				createRandom(topics), documentProvider, createAlpha(topics),
				createFullBeta(documentProvider, topics));
		ldaGibbsSampler.setUpdateAlpha(optimize);
		ldaGibbsSampler.setSmoothingMixBeta(0);
		return ldaGibbsSampler;
	}

	protected double[][] createFullBeta(DocumentProvider<T> documentProvider,
			int topics) {
		mapNodeTGSolution.setNumberOfTopics(topics);
		TIntCollection[] topicSets = mapNodeTGSolution.getTopics();
		double[][] res = new double[topics][documentProvider.getVocab()
				.getNumberOfWords()];
		for (int i = 0; i < topics; i++) {
			TIntIterator iterator = topicSets[i].iterator();
			int sum = 0;
			while (iterator.hasNext()) {
				int wordIndex = iterator.next();
				T word = mapNodeTGSolution.getVocab().getWord(wordIndex);
				int fr = mapNodeTGSolution.getGlobalWordFrequency(wordIndex);
				int wordIndex2 = documentProvider.getVocab().getIndex(word);
				int fr2 = documentProvider.getWordFrequency(wordIndex2);
				if (wordIndex2 != wordIndex) {
					throw new IllegalStateException("word indices do not match");
				}
				if (fr != fr2) {
					throw new IllegalStateException(
							"word frequencies do not match");
				}
				res[i][wordIndex] = fr;
				sum += fr;
			}
			for (int j = 0; j < res[i].length; j++) {
				res[i][j] = res[i][j] / sum;
			}
		}
		return res;
	}

	protected void trainTopicModeler(final AbstractTopicModeler<T> modeler,
			final DocumentProvider<T> documentProvider, boolean optimize) {
		perplexityCalculator.setTopicModeler(modeler);
		PeakValueOptimizer peakValueOptimizer = new PeakValueOptimizer() {
			public double test(double value) {
				((LDAFullBetaGibbsSampler<T>) modeler).setBetaConc(value);
				((LDAGibbsSampler<T>) modeler).solve(200, 100,
						new BasicLDAResultReporter<T>(System.out, 10));

				return -perplexityCalculator
						.computePerplexity(documentProvider);
			}
		};
		peakValueOptimizer.optimizeLambda(0, 1000, 10);
		PeakValueOptimizer peakValueOptimizer2 = new PeakValueOptimizer() {
			public double test(double value) {
				((LDAFullBetaGibbsSampler<T>) modeler).setSmoothingMixBeta(value);
				((LDAGibbsSampler<T>) modeler).solve(200, 100,
						new BasicLDAResultReporter<T>(System.out, 10));

				return -perplexityCalculator
						.computePerplexity(documentProvider);
			}
		};
		peakValueOptimizer2.optimizeLambda(0, 1, 10);
		((LDAGibbsSampler<T>) modeler).solve(2000, 1000,
				new BasicLDAResultReporter<T>(System.out, 10));
	}
	
	@Override
	protected void printOutputHeader() {
		printStream.print("betaSmooth; ");
		printStream.print("betaConc; ");
		super.printOutputHeader();
	}
	
	@Override
	protected void evaluateTopicModeler(int topics,
			AbstractTopicModeler<T> modeler) {
		printStream.print(((LDAFullBetaGibbsSampler<T>)modeler).getSmoothingMixBeta());
		printStream.print("; ");
		printStream.print(((LDAFullBetaGibbsSampler<T>)modeler).getBetaConc());
		printStream.print("; ");
		super.evaluateTopicModeler(topics, modeler);
	}
}
