package org.hhn.topicgrouper.plsa.impl;

import gnu.trove.iterator.TIntIterator;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.validation.AbstractTopicModelerWithProvider;

/**
 * Probabilistic latent semantic analysis trained with the EM algorithm.
 *
 * @param <T> the type of the words
 */
public class PLSA<T> extends AbstractTopicModelerWithProvider<T> {
	private final List<Document<T>> docs;
	private final int docSize;

	private final int[][] docTermMatrix;

	// p(z|d)
	private final double[][] docTopicPros;

	// p(z|d,w)
	private final double[][][] docTermTopicPros;

	private double alphaConc;

	/**
	 * @param random source of randomness
	 * @param provider the training documents
	 * @param nTopics the number of topics
	 * @param alphaConc concentration used to derive alpha from the topic probabilities
	 */
	public PLSA(Random random, DocumentProvider<T> provider, int nTopics,
			double alphaConc) {
		super(random, provider, nTopics);
		docs = provider.getDocuments();
		docSize = docs.size();
		this.alphaConc = alphaConc;
		// element represent times the word appear in this document
		docTermMatrix = new int[docSize][nWords];
		docTopicPros = new double[docSize][nTopics];
		docTermTopicPros = new double[docSize][nWords][nTopics];
	}

	/**
	 * @param alphaConc the concentration used to derive alpha
	 */
	@Override
	public void setAlphaConc(double alphaConc) {
		this.alphaConc = alphaConc;
	}

	/**
	 * Runs the given number of EM iterations.
	 */
	public void train(int maxIter) {
		// init docTermMatrix
		for (int docIndex = 0; docIndex < docSize; docIndex++) {
			Document<T> doc = docs.get(docIndex);
			TIntIterator it = doc.getWordIndices().iterator();
			while (it.hasNext()) {
				int wordIndex = it.next();
				docTermMatrix[docIndex][wordIndex] = doc
						.getWordFrequency(wordIndex);
			}
		}

		// init p(z|d),for each document the constraint is sum(p(z|d))=1.0
		for (int i = 0; i < docSize; i++) {
			randomProbabilities(docTopicPros[i]);
		}
		// init p(w|z),for each topic the constraint is sum(p(w|z))=1.0
		for (int i = 0; i < nTopics; i++) {
			randomProbabilities(phi[i]);
		}

		// use em to estimate params
		for (int i = 0; i < maxIter; i++) {
			em();
			System.out.print(i + "-");
		}
		computeTopicProbabilities();
		System.out.println("done");
	}

	/**
	 * Computes the topic probabilities from the final posteriors p(z|d,w) weighted by word counts.
	 */
	protected void computeTopicProbabilities() {
		int R = provider.getSize();
		for (int i = 0; i < nTopics; i++) {
			double topicSum = 0;
			for (int j = 0; j < docSize; j++) {
				for (int k = 0; k < nWords; k++) {
					topicSum += docTermMatrix[j][k] * docTermTopicPros[j][k][i];
				}
			}
			topicProb[i] = topicSum / R;
		}
	}

	/**
	 * @return the concentration used to derive alpha
	 */
	@Override
	public double getAlphaConc() {
		return alphaConc;
	}

	/**
	 * @param i the topic
	 * @return the topic probability times the concentration
	 */
	@Override
	public double getAlpha(int i) {
		return topicProb[i] * alphaConc;
	}

	@Override
	public double getTopicProb(int topicIndex) {
		return topicProb[topicIndex];
	}

	private final double[] perTopicPro = new double[nTopics];

	/**
	 * 
	 * EM algorithm
	 * 
	 */
	private void em() {
		/*
		 * E-step,calculate posterior probability p(z|d,w,&),& is model
		 * params(p(z|d),p(w|z))
		 * 
		 * p(z|d,w,&)=p(z|d)*p(w|z)/sum(p(z'|d)*p(w|z')) z' represent all
		 * posible topic
		 */
		for (int docIndex = 0; docIndex < docSize; docIndex++) {
			for (int wordIndex = 0; wordIndex < nWords; wordIndex++) {
				double total = 0.0;
				Arrays.fill(perTopicPro, 0);
				for (int topicIndex = 0; topicIndex < nTopics; topicIndex++) {
					double numerator = docTopicPros[docIndex][topicIndex]
							* phi[topicIndex][wordIndex];
					total += numerator;
					perTopicPro[topicIndex] = numerator;
				}

				if (total == 0.0) {
					total = avoidZero(total);
				}

				for (int topicIndex = 0; topicIndex < nTopics; topicIndex++) {
					docTermTopicPros[docIndex][wordIndex][topicIndex] = perTopicPro[topicIndex]
							/ total;
				}
			}
		}

		// M-step
		/*
		 * update
		 * p(w|z),p(w|z)=sum(n(d',w)*p(z|d',w,&))/sum(sum(n(d',w')*p(z|d',w',&)))
		 * 
		 * d' represent all documents w' represent all vocabularies
		 */
		for (int topicIndex = 0; topicIndex < nTopics; topicIndex++) {
			double totalDenominator = 0.0;
			for (int wordIndex = 0; wordIndex < nWords; wordIndex++) {
				double numerator = 0.0;
				for (int docIndex = 0; docIndex < docSize; docIndex++) {
					numerator += docTermMatrix[docIndex][wordIndex]
							* docTermTopicPros[docIndex][wordIndex][topicIndex];
				}

				phi[topicIndex][wordIndex] = numerator;

				totalDenominator += numerator;
			}

			if (totalDenominator == 0.0) {
				totalDenominator = avoidZero(totalDenominator);
			}

			for (int wordIndex = 0; wordIndex < nWords; wordIndex++) {
				phi[topicIndex][wordIndex] = phi[topicIndex][wordIndex]
						/ totalDenominator;
			}
		}
		/*
		 * update
		 * p(z|d),p(z|d)=sum(n(d,w')*p(z|d,w'&))/sum(sum(n(d,w')*p(z'|d,w',&)))
		 * 
		 * w' represent all vocabularies z' represnet all topics
		 */
		for (int docIndex = 0; docIndex < docSize; docIndex++) {
			// actually equal sum(w) of this doc
			double totalDenominator = 0.0;
			for (int topicIndex = 0; topicIndex < nTopics; topicIndex++) {
				double numerator = 0.0;
				for (int wordIndex = 0; wordIndex < nWords; wordIndex++) {
					numerator += docTermMatrix[docIndex][wordIndex]
							* docTermTopicPros[docIndex][wordIndex][topicIndex];
				}
				docTopicPros[docIndex][topicIndex] = Math.pow(numerator, 0.96);
				totalDenominator += numerator;
			}

			if (totalDenominator == 0.0) {
				totalDenominator = avoidZero(totalDenominator);
			}

			for (int topicIndex = 0; topicIndex < nTopics; topicIndex++) {
				docTopicPros[docIndex][topicIndex] = docTopicPros[docIndex][topicIndex]
						/ totalDenominator;
			}
		}
	}

	/**
	 * Fills the array with random probabilities that sum to one (all entries positive).
	 *
	 * @param pros the array to fill
	 * @return the same array
	 */
	public double[] randomProbabilities(double[] pros) {
		int total = 0;
		for (int i = 0; i < pros.length; i++) {
			// avoid zero
			pros[i] = random.nextInt(pros.length) + 1;

			total += pros[i];
		}

		// normalize
		for (int i = 0; i < pros.length; i++) {
			pros[i] = pros[i] / total;
		}

		return pros;
	}

	/**
	 * @return p(z|d) as an array indexed by document and topic
	 */
	public double[][] getDocTopics() {
		return docTopicPros;
	}

	/**
	 * avoid zero number.if input number is zero, we will return a magic number.
	 */
	private final static double MAGICNUM = 0.0000000000000001;

	/**
	 * @param num a number
	 * @return num, or a tiny positive constant if num is zero
	 */
	public double avoidZero(double num) {
		if (num == 0.0) {
			return MAGICNUM;
		}

		return num;
	}
}
