package external.frommallet.lda;

/* Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
 This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
 http://www.cs.umass.edu/~mccallum/mallet
 This software is provided under the terms of the Common Public License,
 version 1.0, as published by http://www.opensource.org.	For further
 information, see the file `LICENSE' included with this distribution. */

import gnu.trove.iterator.TIntIterator;

import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.DocumentSplitter.Split;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.validation.AbstractLDAPerplexityCalculator;

/**
 * An implementation of topic model marginal probability estimators presented in
 * Wallach et al., "Evaluation Methods for Topic Models", ICML (2009)
 * 
 * @author David Mimno
 */

public class MarginalProbEstimator<T> extends
		AbstractLDAPerplexityCalculator<T> {
	protected final Random random;
	protected final int numParticles;
	protected final boolean usingResampling;

	protected int numTopics; // Number of topics to be fit

	// These values are used to encode type/topic counts as
	// count/topic pairs in a single int.
	protected int topicMask;
	protected int topicBits;

	protected double[] alpha; // Dirichlet(alpha,alpha,...) is the distribution
								// over topics
	protected double alphaSum;
	protected double beta; // Prior on per-topic multinomial distribution over
							// words
	protected double betaSum;

	protected double smoothingOnlyMass = 0.0;
	protected double[] cachedCoefficients;

	protected int[][] typeTopicCounts; // indexed by <feature index, topic
										// index>
	protected int[] tokensPerTopic; // indexed by <topic index>


	public MarginalProbEstimator(Random random, DocumentSplitter<T> documentSplitter,
			int numParticles, boolean usingResampling) {
		super(false, documentSplitter);
		this.random = random;
		this.numParticles = numParticles;
		this.usingResampling = usingResampling;
	}


	public double computePerplexity(DocumentProvider<T> testDocumentProvider, LDAGibbsSampler<T> sampler) {
		this.numTopics = sampler.getNTopics();
		
		this.typeTopicCounts = new int[sampler.getDocumentProvider()
	                       				.getNumberOfWords()][numTopics];
		for (int i = 0; i < typeTopicCounts.length; i++) {
			for (int j = 0; j < typeTopicCounts[i].length; j++) {
				typeTopicCounts[i][j] = sampler.getTopicWordAssignmentCount(j, i);
			}
		}
		this.tokensPerTopic = sampler.getTopicFrCountCopy();
			

		alpha = sampler.getAlphaCopy();
		alphaSum = 0;
		for (int i = 0; i < alpha.length; i++) {
			this.alphaSum += alpha[i];			
		}
		this.beta = sampler.getBeta(0, 0); // Assuming symmetric beta.
		this.betaSum = sampler.getBetaSum(0);

		if (Integer.bitCount(numTopics) == 1) {
			// exact power of 2
			topicMask = numTopics - 1;
			topicBits = Integer.bitCount(topicMask);
		} else {
			// otherwise add an extra bit
			topicMask = Integer.highestOneBit(numTopics) * 2 - 1;
			topicBits = Integer.bitCount(topicMask);
		}

		cachedCoefficients = new double[numTopics];

		// Initialize the smoothing-only sampling bucket
		smoothingOnlyMass = 0;

		// Initialize the cached coefficients, using only smoothing.
		// These values will be selectively replaced in documents with
		// non-zero counts in particular topics.

		for (int topic = 0; topic < numTopics; topic++) {
			smoothingOnlyMass += alpha[topic] * beta
					/ (tokensPerTopic[topic] + betaSum);
			cachedCoefficients[topic] = alpha[topic]
					/ (tokensPerTopic[topic] + betaSum);
		}
		
		
		double sumA = 0;
		long sumB = 0;

		for (Document<T> doc : testDocumentProvider.getDocuments()) {
			documentSplitter.setDocument(doc);
			int splits = documentSplitter.getSplits();
			for (int i = 0; i < splits; i++) {
				Split<T> nextSplit = documentSplitter.nextSplit();
				Document<T> rd = nextSplit.getRefDoc();
				Document<T> d = nextSplit.getTestDoc();
				sumA += computeLogProbability(doc) - computeLogProbability(rd);
				sumB += d.getSize();
				// splitLogSum += computeLogProbability(rd, d, s);
				// splitSizeSum += d.getSize();
			}
		}
		return Math.exp(-sumA / sumB);
	}

	// public double evaluateLeftToRight(DocumentProvider<T> testing) {
	// double logNumParticles = Math.log(numParticles);
	// double totalLogLikelihood = 0;
	// for (Document<T> d : testing.getDocuments()) {
	//
	// double docLogLikelihood = 0;
	//
	// double[][] particleProbabilities = new double[numParticles][];
	// for (int particle = 0; particle < numParticles; particle++) {
	// particleProbabilities[particle] = leftToRight(d,
	// usingResampling);
	// }
	//
	// for (int position = 0; position < particleProbabilities[0].length;
	// position++) {
	// double sum = 0;
	// for (int particle = 0; particle < numParticles; particle++) {
	// sum += particleProbabilities[particle][position];
	// }
	//
	// if (sum > 0.0) {
	// double logProb = Math.log(sum) - logNumParticles;
	// docLogLikelihood += logProb;
	// }
	// }
	// totalLogLikelihood += docLogLikelihood;
	// }
	//
	// return totalLogLikelihood;
	// }

	protected double computeLogProbability(Document<T> d) {
		double docLogLikelihood = 0;
		double logNumParticles = Math.log(numParticles);

		double[][] particleProbabilities = new double[numParticles][];
		for (int particle = 0; particle < numParticles; particle++) {
			particleProbabilities[particle] = leftToRight(d, usingResampling);
		}

		for (int position = 0; position < particleProbabilities[0].length; position++) {
			double sum = 0;
			for (int particle = 0; particle < numParticles; particle++) {
				sum += particleProbabilities[particle][position];
			}

			if (sum > 0.0) {
				double logProb = Math.log(sum) - logNumParticles;
				docLogLikelihood += logProb;
			}
		}
		return docLogLikelihood;
	}

	protected double[] leftToRight(Document<T> d, boolean usingResampling) {

		int[] oneDocTopics = new int[d.getSize()];
		double[] wordProbabilities = new double[d.getSize()];

		int[] currentTypeTopicCounts;
		int type, oldTopic, newTopic;

		// Keep track of the number of tokens we've examined, not
		// including out-of-vocabulary words
		int tokensSoFar = 0;

		int[] localTopicCounts = new int[numTopics];
		int[] localTopicIndex = new int[numTopics];

		// Build an array that densely lists the topics that
		// have non-zero counts.
		int denseIndex = 0;

		// Record the total number of non-zero topics
		int nonZeroTopics = denseIndex;

		// Initialize the topic count/beta sampling bucket
		double topicBetaMass = 0.0;
		double topicTermMass = 0.0;

		double[] topicTermScores = new double[numTopics];
		int i;
		double score;

		// All counts are now zero, we are starting completely fresh.

		// Iterate over the positions (words) in the document
		TIntIterator it1 = d.getWordIndices().iterator();
		int limit = 0;
		while (it1.hasNext()) {
			int index1 = it1.next();
			int fr1 = d.getWordFrequency(index1);
			for (int j1 = 0; j1 < fr1; j1++, limit++) {
				// Record the marginal probability of the token
				// at the current limit, summed over all topics.

				if (usingResampling) {

					// Iterate up to the current limit
					TIntIterator it = d.getWordIndices().iterator();
					int position = 0;
					while (it.hasNext()) {
						type = it.next();
						int fr = d.getWordFrequency(type);
						for (int j = 0; j < fr; j++, position++) {
							oldTopic = oneDocTopics[position];

							// Check for out-of-vocabulary words
							if (type >= typeTopicCounts.length
									|| typeTopicCounts[type] == null) {
								continue;
							}

							currentTypeTopicCounts = typeTopicCounts[type];

							// Remove this token from all counts.

							// Remove this topic's contribution to the
							// normalizing constants.
							// Note that we are using clamped estimates of
							// P(w|t),
							// so we are NOT changing smoothingOnlyMass.
							topicBetaMass -= beta * localTopicCounts[oldTopic]
									/ (tokensPerTopic[oldTopic] + betaSum);

							// Decrement the local doc/topic counts

							localTopicCounts[oldTopic]--;

							// Maintain the dense index, if we are deleting
							// the old topic
							if (localTopicCounts[oldTopic] == 0) {

								// First get to the dense location associated
								// with
								// the old topic.

								denseIndex = 0;

								// We know it's in there somewhere, so we don't
								// need bounds checking.
								while (localTopicIndex[denseIndex] != oldTopic) {
									denseIndex++;
								}

								// shift all remaining dense indices to the
								// left.
								while (denseIndex < nonZeroTopics) {
									if (denseIndex < localTopicIndex.length - 1) {
										localTopicIndex[denseIndex] = localTopicIndex[denseIndex + 1];
									}
									denseIndex++;
								}

								nonZeroTopics--;
							}

							// Add the old topic's contribution back into the
							// normalizing constants.
							topicBetaMass += beta * localTopicCounts[oldTopic]
									/ (tokensPerTopic[oldTopic] + betaSum);

							// Reset the cached coefficient for this topic
							cachedCoefficients[oldTopic] = (alpha[oldTopic] + localTopicCounts[oldTopic])
									/ (tokensPerTopic[oldTopic] + betaSum);

							// Now go over the type/topic counts, calculating
							// the
							// score
							// for each topic.

							int index = 0;
							int currentTopic, currentValue;

							topicTermMass = 0.0;

							while (index < currentTypeTopicCounts.length
									&& currentTypeTopicCounts[index] > 0) {
								currentTopic = currentTypeTopicCounts[index]
										& topicMask;
								currentValue = currentTypeTopicCounts[index] >> topicBits;

								score = cachedCoefficients[currentTopic]
										* currentValue;
								topicTermMass += score;
								topicTermScores[index] = score;

								index++;
							}

							double sample = random.nextDouble()
									* (smoothingOnlyMass + topicBetaMass + topicTermMass);
							double origSample = sample;

							// Make sure it actually gets set
							newTopic = -1;

							if (sample < topicTermMass) {

								i = -1;
								while (sample > 0) {
									i++;
									sample -= topicTermScores[i];
								}

								newTopic = currentTypeTopicCounts[i]
										& topicMask;
							} else {
								sample -= topicTermMass;

								if (sample < topicBetaMass) {
									// betaTopicCount++;

									sample /= beta;

									for (denseIndex = 0; denseIndex < nonZeroTopics; denseIndex++) {
										int topic = localTopicIndex[denseIndex];

										sample -= localTopicCounts[topic]
												/ (tokensPerTopic[topic] + betaSum);

										if (sample <= 0.0) {
											newTopic = topic;
											break;
										}
									}

								} else {
									// smoothingOnlyCount++;

									sample -= topicBetaMass;

									sample /= beta;

									newTopic = 0;
									sample -= alpha[newTopic]
											/ (tokensPerTopic[newTopic] + betaSum);

									while (sample > 0.0) {
										newTopic++;
										sample -= alpha[newTopic]
												/ (tokensPerTopic[newTopic] + betaSum);
									}

								}

							}

							if (newTopic == -1) {
								System.err.println("sampling error: "
										+ origSample + " " + sample + " "
										+ smoothingOnlyMass + " "
										+ topicBetaMass + " " + topicTermMass);
								newTopic = numTopics - 1; // TODO is this
															// appropriate
								// throw new IllegalStateException
								// ("WorkerRunnable: New topic not sampled.");
							}
							// assert(newTopic != -1);

							// Put that new topic into the counts
							oneDocTopics[position] = newTopic;

							topicBetaMass -= beta * localTopicCounts[newTopic]
									/ (tokensPerTopic[newTopic] + betaSum);

							localTopicCounts[newTopic]++;

							// If this is a new topic for this document,
							// add the topic to the dense index.
							if (localTopicCounts[newTopic] == 1) {

								// First find the point where we
								// should insert the new topic by going to
								// the end (which is the only reason we're
								// keeping
								// track of the number of non-zero
								// topics) and working backwards

								denseIndex = nonZeroTopics;

								while (denseIndex > 0
										&& localTopicIndex[denseIndex - 1] > newTopic) {

									localTopicIndex[denseIndex] = localTopicIndex[denseIndex - 1];
									denseIndex--;
								}

								localTopicIndex[denseIndex] = newTopic;
								nonZeroTopics++;
							}

							// update the coefficients for the non-zero topics
							cachedCoefficients[newTopic] = (alpha[newTopic] + localTopicCounts[newTopic])
									/ (tokensPerTopic[newTopic] + betaSum);

							topicBetaMass += beta * localTopicCounts[newTopic]
									/ (tokensPerTopic[newTopic] + betaSum);

						}
					}
				}

				// We've just resampled all tokens UP TO the current limit,
				// now sample the token AT the current limit.

				type = index1;

				// Check for out-of-vocabulary words
				if (type >= typeTopicCounts.length
						|| typeTopicCounts[type] == null) {
					continue;
				}

				currentTypeTopicCounts = typeTopicCounts[type];

				int index = 0;
				int currentTopic, currentValue;

				topicTermMass = 0.0;

				while (index < currentTypeTopicCounts.length
						&& currentTypeTopicCounts[index] > 0) {
					currentTopic = currentTypeTopicCounts[index] & topicMask;
					currentValue = currentTypeTopicCounts[index] >> topicBits;

					score = cachedCoefficients[currentTopic] * currentValue;
					topicTermMass += score;
					topicTermScores[index] = score;

					// System.out.println("  " + currentTopic + " = " +
					// currentValue);

					index++;
				}

				/*
				 * // Debugging, to make sure we're getting the right
				 * probabilities for (int topic = 0; topic < numTopics; topic++)
				 * { index = 0; int displayCount = 0;
				 * 
				 * while (index < currentTypeTopicCounts.length &&
				 * currentTypeTopicCounts[index] > 0) { currentTopic =
				 * currentTypeTopicCounts[index] & topicMask; currentValue =
				 * currentTypeTopicCounts[index] >> topicBits;
				 * 
				 * if (currentTopic == topic) { displayCount = currentValue;
				 * break; }
				 * 
				 * index++; }
				 * 
				 * System.out.print(topic + "\t"); System.out.print("(" +
				 * localTopicCounts[topic] + " + " + alpha[topic] + ") / " + "("
				 * + alphaSum + " + " + tokensSoFar + ") * ");
				 * 
				 * System.out.println("(" + displayCount + " + " + beta + ") / "
				 * + "(" + tokensPerTopic[topic] + " + " + betaSum + ") =" +
				 * ((displayCount + beta) / (tokensPerTopic[topic] + betaSum)));
				 * 
				 * 
				 * }
				 */

				double sample = random.nextDouble()
						* (smoothingOnlyMass + topicBetaMass + topicTermMass);
				double origSample = sample;

				// Note that we've been absorbing (alphaSum + docLength) into
				// the normalizing constant. The true marginal probability needs
				// this term, so we stick it back in.
				wordProbabilities[limit] += (smoothingOnlyMass + topicBetaMass + topicTermMass)
						/ (alphaSum + tokensSoFar);

				// System.out.println("normalizer: " + alphaSum + " + " +
				// tokensSoFar);
				tokensSoFar++;

				// Make sure it actually gets set
				newTopic = -1;

				if (sample < topicTermMass) {

					i = -1;
					while (sample > 0) {
						i++;
						sample -= topicTermScores[i];
					}

					newTopic = currentTypeTopicCounts[i] & topicMask;
				} else {
					sample -= topicTermMass;

					if (sample < topicBetaMass) {
						// betaTopicCount++;

						sample /= beta;

						for (denseIndex = 0; denseIndex < nonZeroTopics; denseIndex++) {
							int topic = localTopicIndex[denseIndex];

							sample -= localTopicCounts[topic]
									/ (tokensPerTopic[topic] + betaSum);

							if (sample <= 0.0) {
								newTopic = topic;
								break;
							}
						}

					} else {
						// smoothingOnlyCount++;

						sample -= topicBetaMass;

						sample /= beta;

						newTopic = 0;
						sample -= alpha[newTopic]
								/ (tokensPerTopic[newTopic] + betaSum);

						while (sample > 0.0) {
							newTopic++;
							sample -= alpha[newTopic]
									/ (tokensPerTopic[newTopic] + betaSum);
						}

					}

				}

				if (newTopic == -1) {
					System.err.println("sampling error: " + origSample + " "
							+ sample + " " + smoothingOnlyMass + " "
							+ topicBetaMass + " " + topicTermMass);
					newTopic = numTopics - 1; // TODO is this appropriate
				}

				// Put that new topic into the counts
				oneDocTopics[limit] = newTopic;

				topicBetaMass -= beta * localTopicCounts[newTopic]
						/ (tokensPerTopic[newTopic] + betaSum);

				localTopicCounts[newTopic]++;

				// If this is a new topic for this document,
				// add the topic to the dense index.
				if (localTopicCounts[newTopic] == 1) {

					// First find the point where we
					// should insert the new topic by going to
					// the end (which is the only reason we're keeping
					// track of the number of non-zero
					// topics) and working backwards

					denseIndex = nonZeroTopics;

					while (denseIndex > 0
							&& localTopicIndex[denseIndex - 1] > newTopic) {

						localTopicIndex[denseIndex] = localTopicIndex[denseIndex - 1];
						denseIndex--;
					}

					localTopicIndex[denseIndex] = newTopic;
					nonZeroTopics++;
				}

				// update the coefficients for the non-zero topics
				cachedCoefficients[newTopic] = (alpha[newTopic] + localTopicCounts[newTopic])
						/ (tokensPerTopic[newTopic] + betaSum);

				topicBetaMass += beta * localTopicCounts[newTopic]
						/ (tokensPerTopic[newTopic] + betaSum);

				// System.out.println(type + "\t" + newTopic + "\t" +
				// logLikelihood);
			}
		}

		// Clean up our mess: reset the coefficients to values with only
		// smoothing. The next doc will update its own non-zero topics...

		for (denseIndex = 0; denseIndex < nonZeroTopics; denseIndex++) {
			int topic = localTopicIndex[denseIndex];

			cachedCoefficients[topic] = alpha[topic]
					/ (tokensPerTopic[topic] + betaSum);
		}

		return wordProbabilities;

	}
}
