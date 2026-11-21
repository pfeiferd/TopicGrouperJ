package org.hhn.topicgrouper.validation;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;

/**
 * Base class of the left-to-right estimators of the probability of a document
 * (Wallach et al., "Evaluation Methods for Topic Models", 2009, Algorithm 3).
 * Subclasses implement the estimate for a single word given the preceding ones.
 *
 * @param <T> the type of the words
 */
public abstract class LeftToRightSamplerBase<T> {
	/**
	 * Source of randomness.
	 */
	protected final Random random;

	// For "left to right".
	/**
	 * The words processed so far, in order of occurrence.
	 */
	protected final TIntList linearWordIndices;
	/**
	 * Buffer for unnormalized topic probabilities.
	 */
	protected final double[] pzn;
	/**
	 * Number of particles.
	 */
	protected final int particles;

	/**
	 * @param random source of randomness
	 * @param particles number of particles
	 * @param ntopics the number of topics
	 */
	public LeftToRightSamplerBase(Random random, int particles, int ntopics) {
		this.particles = particles;
		this.random = random;
		linearWordIndices = new TIntArrayList();
		pzn = new double[ntopics];
	}

	/**
	 * Estimates the log probability of a document.
	 *
	 * @param d the document
	 * @return the estimated log probability
	 */
	public double leftToRight(Document<T> d) {
		initFields();
		return leftToRightHelp(d);
	}

	/**
	 * Resets the sampler state before a new document.
	 */
	protected void initFields() {
		linearWordIndices.clear();
	}

	/**
	 * Sums the log of the per-word estimates over all word occurrences of the document,
	 * continuing after the words already processed.
	 *
	 * @param d the document
	 * @return the sum of the log probabilities
	 */
	protected double leftToRightHelp(Document<T> d) {
		double l = 0;
		int n = linearWordIndices.size();
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int wordIndex = it.next();
			int fr = d.getWordFrequency(wordIndex);
			for (int i = 0; i < fr; i++) {
				l += Math.log(leftToRightForWord(n, wordIndex));
				n++;
			}
		}

		return l;
	}

	/**
	 * Estimates the log probability of a document given a reference document: the reference
	 * words are processed first (their probability is not counted).
	 *
	 * @param refDoc the reference document, may be null
	 * @param d the document to be scored
	 * @return the estimated log probability of d
	 */
	public double leftToRightDocCompletion(Document<T> refDoc, Document<T> d) {
		initFields();
		if (refDoc != null) {
			leftToRightHelp(refDoc);
		}
		return leftToRightHelp(d);
	}

	/**
	 * Estimates the probability of the next word given the n previous ones.
	 *
	 * @param n the number of preceding words
	 * @param wordIndex the index of the next word
	 * @return the estimated probability
	 */
	protected abstract double leftToRightForWord(int n, int wordIndex);
	
	/**
	 * Draws an index from the unnormalized discrete distribution.
	 *
	 * @param probs unnormalized probabilities
	 * @return the drawn index
	 */
	protected int nextDiscrete(double[] probs) {
		double sum = 0.0;
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
		}
		double r = random.nextDouble() * sum;

		sum = 0.0;
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
			if (sum > r) {
				return i;
			}
		}
		return probs.length - 1;
	}

	/**
	 * @param t the topic
	 * @return the Dirichlet prior parameter alpha of the topic
	 */
	protected abstract double getAlpha(int t);

	/**
	 * @return the sum of all alpha values
	 */
	protected abstract double getAlphaSum();

	/**
	 * @param t the topic
	 * @param wordIndex the word
	 * @return the probability of the word in the topic
	 */
	protected abstract double getPhi(int t, int wordIndex);
}
