package org.hhn.topicgrouper.tg.validation;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.impl.AbstractTopicGrouper;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;
import org.hhn.topicgrouper.validation.BasicPerplexityCalculator;
import org.hhn.topicgrouper.validation.PeakValueOptimizer;

public class TGPerplexityCalculator<T> {
	protected final boolean bowFactor;
	protected final DocumentSplitter<T> documentSplitter;
	protected double alphaConc;

	private double smoothingLambda = 0.5;

	private int[] topicIds;
	private TIntCollection[] topics;

	private BasicPerplexityCalculator<T> perplexityCalculator;

	public TGPerplexityCalculator() {
		this(false, new DefaultDocumentSplitter<T>(), 1);
	}

	public TGPerplexityCalculator(boolean bowFactor,
			DocumentSplitter<T> documentSplitter, double alphaConc) {
		this.bowFactor = bowFactor;
		this.documentSplitter = documentSplitter;
		this.alphaConc = alphaConc;
	}

	public void init() {
		perplexityCalculator = createBasicPerplexityCalculator();
	}

	public BasicPerplexityCalculator<T> getPerplexityCalculator() {
		return perplexityCalculator;
	}

	public double getAlphaConc() {
		return alphaConc;
	}

	public void setAlphaConc(double alphaConc) {
		this.alphaConc = alphaConc;
	}

	public void setSolution(TGSolution<T> s) {
		perplexityCalculator.setTopicModeler(createTopicModeler(s));
	}

	public void optimizeAlphaConc(double minLambda, double maxLambda,
			final DocumentProvider<T> provider, int steps) {
		PeakValueOptimizer peakValueOptimizer = new PeakValueOptimizer() {
			public double test(double value) {
				alphaConc = value;
				return -perplexityCalculator.computePerplexity(provider);
			}
		};
		alphaConc = peakValueOptimizer.optimizeLambda(minLambda, maxLambda,
				steps);
	}

	public void optimizeLambda(double minLambda, double maxLambda,
			final DocumentProvider<T> provider, int steps) {
		PeakValueOptimizer peakValueOptimizer = new PeakValueOptimizer() {
			public double test(double value) {
				smoothingLambda = value;
				return perplexityCalculator.computePerplexity(provider);
			}
		};
		smoothingLambda = peakValueOptimizer.optimizeLambda(minLambda,
				maxLambda, steps);
	}

	public double computePerplexity(DocumentProvider<T> testDocumentProvider) {
		return perplexityCalculator.computePerplexity(testDocumentProvider);
	}

	public AbstractTopicModeler<T> createTopicModeler(final TGSolution<T> s) {
		topicIds = s.getTopicIds();
		topics = s.getTopics();

		AbstractTopicModeler<T> topicModeler = new AbstractTopicModeler<T>(
				s.getVocab(), s.getNumberOfTopics()) {
			@Override
			protected void initPhi() {
				for (int i = 0; i < phi.length; i++) {
					double tf = s.getTopicFrequency(topicIds[i]);
					for (int j = 0; j < phi[i].length; j++) {
						if (s.getTopicForWord(j) == topicIds[i]) {
							phi[i][j] = s.getGlobalWordFrequency(j) / tf;
						}
					}
				}
			}

			@Override
			protected void initTopicProb() {
				for (int i = 0; i < topicProb.length; i++) {
					topicProb[i] = ((double) s.getTopicFrequency(topicIds[i]))
							/ s.getSize();
				}
			}

			@Override
			public double getWordProb(int wordIndex) {
				return ((double) s.getGlobalWordFrequency(wordIndex))
						/ s.getSize();
			}

			@Override
			public double getAlpha(int i) {
				return topicProb[i] * alphaConc;
			}

			@Override
			public double getAlphaConc() {
				return alphaConc;
			}

			@Override
			public void setAlphaConc(double alphaConc) {
				TGPerplexityCalculator.this.alphaConc = alphaConc;
			}
		};

		return topicModeler;
	}

	public AbstractTopicModeler<T> createSmoothedTopicModeler(
			final TGSolution<T> s) {
		topicIds = s.getTopicIds();
		topics = s.getTopics();

		final AbstractTopicGrouper<T>.DefaultTGSolution defaultSolution = (AbstractTopicGrouper<T>.DefaultTGSolution) s;

		AbstractTopicModeler<T> topicModeler = new AbstractTopicModeler<T>(
				s.getVocab(), s.getNumberOfTopics()) {
			@Override
			protected void initPhi() {
				double[] wSum = new double[nWords];

				for (int i = 0; i < phi.length; i++) {
					for (int j = 0; j < phi[i].length; j++) {
						double deltaH = defaultSolution
								.computeTopicWordLogLikelihood(topicIds[i], j);
						phi[i][j] = deltaH;
						wSum[j] += deltaH;
					}
				}
				for (int i = 0; i < phi.length; i++) {
					topicProb[i] = 0;
					double sum = 0;
					for (int j = 0; j < phi[i].length; j++) {
						int wordFr = s.getGlobalWordFrequency(j);
						topicProb[i] += wordFr * phi[i][j] / wSum[j] / s.getSize();
						phi[i][j] *= wordFr;
						sum += phi[i][j];
					}
					for (int j = 0; j < phi[i].length; j++) {
						phi[i][j] = phi[i][j] / sum;
					}
				}
			}

			@Override
			protected void initTopicProb() {
				// Nothing to do, all done in initPhi()...
			}

			@Override
			public double getWordProb(int wordIndex) {
				return ((double) s.getGlobalWordFrequency(wordIndex))
						/ s.getSize();
			}

			@Override
			public double getAlpha(int i) {
				return topicProb[i] * alphaConc;
			}

			@Override
			public double getAlphaConc() {
				return alphaConc;
			}

			@Override
			public void setAlphaConc(double alphaConc) {
				TGPerplexityCalculator.this.alphaConc = alphaConc;
			}
		};

		return topicModeler;
	}

	protected BasicPerplexityCalculator<T> createBasicPerplexityCalculator() {
		return new BasicPerplexityCalculator<T>(bowFactor, documentSplitter, 1) {
			@Override
			protected void updatePtd(Document<T> d,
					AbstractTopicModeler<T> sampler) {
				super.updatePtd(d, sampler);
				if (d != null && smoothingLambda < 1) {
					for (int i = 0; i < ptd.length; i++) {
						int topicFrInDoc = 0;
						TIntIterator it = topics[topicIds[i]].iterator();
						while (it.hasNext()) {
							topicFrInDoc += d.getWordFrequency(it.next());
						}
						ptd[i] = smoothedPtd(topicFrInDoc, d.getSize(), ptd[i]);
					}
				}
			}
		};
	}

	protected double smoothedPtd(int topicFrInDoc, int docSize, double ptd) {
		double h = (((1 - smoothingLambda) * topicFrInDoc) / docSize)
		// Smoothing via global frequency of topic;
				+ smoothingLambda * ptd;
		if (Double.isNaN(h)) {
			throw new IllegalStateException();
		}
		return h;
	}

	protected double getSmoothingLambda(TGSolution<T> s) {
		return smoothingLambda;
	}
}
