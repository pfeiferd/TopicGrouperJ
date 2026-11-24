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
	 * Creates a sampler.
	 *
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
	 * The topics in which a word has a non-zero probability, in ascending order; the
	 * samplers only consider these topics for the word. <code>null</code> stands for
	 * all topics (the default).
	 *
	 * @param wordIndex the word
	 * @return the candidate topics of the word or <code>null</code> for all topics
	 */
	protected int[] getCandidateTopics(int wordIndex) {
		return null;
	}

	/**
	 * Samples a topic for a word given the topic counts: proportional to
	 * phi(k, w) * (counts[k] + alpha_k) over the candidate topics of the word.
	 * A word with a single candidate topic gets it without a random draw.
	 *
	 * @param wordIndex the word
	 * @param counts the current topic counts
	 * @return the sampled topic
	 */
	protected int sampleTopic(int wordIndex, int[] counts) {
		int[] candidates = getCandidateTopics(wordIndex);
		if (candidates == null) {
			for (int k = 0; k < pzn.length; k++) {
				pzn[k] = getPhi(k, wordIndex) * (counts[k] + getAlpha(k));
			}
			return nextDiscrete(pzn, pzn.length);
		}
		if (candidates.length == 1) {
			return candidates[0];
		}
		if (candidates.length == 0) {
			// The word has probability zero everywhere (as the dense loop would find).
			return pzn.length - 1;
		}
		for (int i = 0; i < candidates.length; i++) {
			int k = candidates[i];
			pzn[i] = getPhi(k, wordIndex) * (counts[k] + getAlpha(k));
		}
		return candidates[nextDiscrete(pzn, candidates.length)];
	}

	/**
	 * The predictive probability of a word given the topic counts of the previous
	 * words: sum_t phi(t, w) * (counts[t] + alpha_t) / (n + sum alpha) over the
	 * candidate topics of the word.
	 *
	 * @param wordIndex the word
	 * @param counts the topic counts of the previous words
	 * @param n the number of previous words
	 * @return the predictive probability
	 */
	protected double wordProbability(int wordIndex, int[] counts, int n) {
		double pn = 0;
		int[] candidates = getCandidateTopics(wordIndex);
		if (candidates == null) {
			for (int t = 0; t < pzn.length; t++) {
				pn += getPhi(t, wordIndex) * (counts[t] + getAlpha(t));
			}
		} else {
			for (int t : candidates) {
				pn += getPhi(t, wordIndex) * (counts[t] + getAlpha(t));
			}
		}
		return pn / (n + getAlphaSum());
	}
	
	/**
	 * Draws an index from the unnormalized discrete distribution.
	 *
	 * @param probs unnormalized probabilities
	 * @return the drawn index
	 */
	protected int nextDiscrete(double[] probs) {
		return nextDiscrete(probs, probs.length);
	}

	/**
	 * Draws an index from the unnormalized discrete distribution given by the first
	 * <code>count</code> entries.
	 *
	 * @param probs unnormalized probabilities
	 * @param count the number of entries to consider
	 * @return the drawn index, below <code>count</code>
	 */
	protected int nextDiscrete(double[] probs, int count) {
		double sum = 0.0;
		for (int i = 0; i < count; i++) {
			sum += probs[i];
		}
		double r = random.nextDouble() * sum;

		sum = 0.0;
		for (int i = 0; i < count; i++) {
			sum += probs[i];
			if (sum > r) {
				return i;
			}
		}
		return count - 1;
	}

	/**
	 * The Dirichlet prior over the topic distribution of a document.
	 *
	 * @param t the topic
	 * @return the Dirichlet prior parameter alpha of the topic
	 */
	protected abstract double getAlpha(int t);

	/**
	 * The concentration of the Dirichlet prior over the topic distribution of a document.
	 *
	 * @return the sum of all alpha values
	 */
	protected abstract double getAlphaSum();

	/**
	 * The topic-word distribution of the model being evaluated.
	 *
	 * @param t the topic
	 * @param wordIndex the word
	 * @return the probability of the word in the topic
	 */
	protected abstract double getPhi(int t, int wordIndex);
}
