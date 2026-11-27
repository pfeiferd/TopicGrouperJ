package org.hhn.topicgrouper.plsa.impl;

import gnu.trove.iterator.TIntIterator;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.validation.AbstractTopicModelerWithProvider;

/**
 * Probabilistic latent semantic analysis (Hofmann, "Probabilistic Latent Semantic Indexing",
 * SIGIR 1999) trained with the EM algorithm: the E-step computes the posteriors
 * p(z|d,w) from the current p(z|d) and p(w|z), the M-step re-estimates p(w|z) (phi) and p(z|d)
 * from the word counts weighted by the posteriors. Both steps are fused and run over the
 * words that occur in a document only, so the posteriors are never stored: the memory is
 * that of p(w|z) and p(z|d) plus the word counts of the documents, and the time per
 * iteration is linear in the number of distinct document-word pairs times the number of
 * topics. Since PLSA has no Dirichlet prior, {@link #getAlpha(int)} derives one from the
 * topic probabilities and a configurable concentration.
 *
 * @param <T> the type of the words
 */
public class PLSA<T> extends AbstractTopicModelerWithProvider<T> {
	/**
	 * The training documents.
	 */
	private final List<Document<T>> docs;
	/**
	 * The number of training documents.
	 */
	private final int docSize;

	/**
	 * docWords[d], the indices of the words occurring in document d, ascending.
	 */
	private final int[][] docWords;

	/**
	 * docCounts[d][i], the number of occurrences of word docWords[d][i] in document d.
	 */
	private final int[][] docCounts;

	/**
	 * docTopicPros[d][z], the probability p(z|d) of topic z in document d.
	 */
	private final double[][] docTopicPros;

	/**
	 * phiNumerators[z][w], the posterior weighted counts sum_d n(d,w) p(z|d,w) of the
	 * current M-step.
	 */
	private final double[][] phiNumerators;

	/**
	 * topicMass[z], the posterior weighted number of tokens sum_{d,w} n(d,w) p(z|d,w) of
	 * the last M-step (the denominator of p(w|z)).
	 */
	private final double[] topicMass;

	/**
	 * Concentration used to derive alpha from the topic probabilities.
	 */
	private double alphaConc;

	/**
	 * Creates an untrained model; call {@link #train(int)} to fit it.
	 *
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
		docWords = new int[docSize][];
		docCounts = new int[docSize][];
		for (int docIndex = 0; docIndex < docSize; docIndex++) {
			Document<T> doc = docs.get(docIndex);
			int[] words = new int[doc.getWordIndices().size()];
			TIntIterator it = doc.getWordIndices().iterator();
			for (int i = 0; i < words.length; i++) {
				words[i] = it.next();
			}
			// A fixed order makes the floating point sums reproducible.
			Arrays.sort(words);
			int[] counts = new int[words.length];
			for (int i = 0; i < words.length; i++) {
				counts[i] = doc.getWordFrequency(words[i]);
			}
			docWords[docIndex] = words;
			docCounts[docIndex] = counts;
		}
		docTopicPros = new double[docSize][nTopics];
		phiNumerators = new double[nTopics][nWords];
		topicMass = new double[nTopics];
	}

	/**
	 * @param alphaConc the concentration used to derive alpha
	 */
	@Override
	public void setAlphaConc(double alphaConc) {
		this.alphaConc = alphaConc;
	}

	/**
	 * Initializes p(z|d) and p(w|z) randomly, runs the given number of EM iterations and
	 * finally computes the topic probabilities. Progress is printed to standard output.
	 *
	 * @param maxIter the number of EM iterations
	 */
	public void train(int maxIter) {
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
			afterIteration(i);
		}
		computeTopicProbabilities();
		System.out.println("done");
	}

	/**
	 * Hook called after each EM iteration; does nothing by default.
	 *
	 * @param iteration the number of the completed iteration, starting with 0
	 */
	protected void afterIteration(int iteration) {
	}

	/**
	 * Computes the log-likelihood of the training data under the current parameters,
	 * i.e. sum over documents d and words w of n(d,w) * log sum_z p(z|d) p(w|z).
	 *
	 * @return the log-likelihood
	 */
	public double computeLogLikelihood() {
		double ll = 0;
		for (int docIndex = 0; docIndex < docSize; docIndex++) {
			int[] words = docWords[docIndex];
			int[] counts = docCounts[docIndex];
			double[] theta = docTopicPros[docIndex];
			for (int i = 0; i < words.length; i++) {
				double pw = 0;
				for (int topicIndex = 0; topicIndex < nTopics; topicIndex++) {
					pw += theta[topicIndex] * phi[topicIndex][words[i]];
				}
				ll += counts[i] * Math.log(pw);
			}
		}
		return ll;
	}

	/**
	 * Computes the topic probabilities from the posteriors p(z|d,w) of the last EM iteration
	 * weighted by the word counts (zero before the first iteration).
	 */
	protected void computeTopicProbabilities() {
		int R = provider.getSize();
		for (int i = 0; i < nTopics; i++) {
			topicProb[i] = topicMass[i] / R;
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

	/**
	 * Buffer for the posteriors of the topics of one document-word pair in the E-step.
	 */
	private final double[] perTopicPro = new double[nTopics];

	/**
	 * Buffer for the posterior weighted counts of the topics of one document in the M-step.
	 */
	private final double[] perDocTopicCount = new double[nTopics];

	/**
	 * One EM iteration: for every document-word pair with n(d,w) &gt; 0 the posterior
	 * p(z|d,w) = p(z|d) p(w|z) / sum_z' p(z'|d) p(w|z') is computed (E-step) and at once
	 * added, weighted by n(d,w), to the new estimates of p(w|z) and p(z|d) (M-step):
	 * p(w|z) = sum_d n(d,w) p(z|d,w) / sum_{d,w'} n(d,w') p(z|d,w'), and
	 * p(z|d) = sum_w n(d,w) p(z|d,w) / sum_{w,z'} n(d,w) p(z'|d,w). Pairs with n(d,w) = 0
	 * contribute nothing to either sum and are skipped.
	 */
	private void em() {
		for (int topicIndex = 0; topicIndex < nTopics; topicIndex++) {
			Arrays.fill(phiNumerators[topicIndex], 0);
		}
		Arrays.fill(topicMass, 0);
		for (int docIndex = 0; docIndex < docSize; docIndex++) {
			int[] words = docWords[docIndex];
			int[] counts = docCounts[docIndex];
			double[] theta = docTopicPros[docIndex];
			Arrays.fill(perDocTopicCount, 0);
			for (int i = 0; i < words.length; i++) {
				int wordIndex = words[i];
				double total = 0.0;
				for (int topicIndex = 0; topicIndex < nTopics; topicIndex++) {
					double numerator = theta[topicIndex] * phi[topicIndex][wordIndex];
					total += numerator;
					perTopicPro[topicIndex] = numerator;
				}
				if (total == 0.0) {
					total = avoidZero(total);
				}
				int n = counts[i];
				for (int topicIndex = 0; topicIndex < nTopics; topicIndex++) {
					double weighted = n * perTopicPro[topicIndex] / total;
					phiNumerators[topicIndex][wordIndex] += weighted;
					perDocTopicCount[topicIndex] += weighted;
				}
			}
			// p(z|d): the denominator is the posterior weighted size of the document.
			double totalDenominator = 0.0;
			for (int topicIndex = 0; topicIndex < nTopics; topicIndex++) {
				totalDenominator += perDocTopicCount[topicIndex];
			}
			if (totalDenominator == 0.0) {
				totalDenominator = avoidZero(totalDenominator);
			}
			for (int topicIndex = 0; topicIndex < nTopics; topicIndex++) {
				theta[topicIndex] = perDocTopicCount[topicIndex] / totalDenominator;
			}
		}
		// p(w|z)
		for (int topicIndex = 0; topicIndex < nTopics; topicIndex++) {
			double[] numerators = phiNumerators[topicIndex];
			double totalDenominator = 0.0;
			for (int wordIndex = 0; wordIndex < nWords; wordIndex++) {
				totalDenominator += numerators[wordIndex];
			}
			topicMass[topicIndex] = totalDenominator;
			if (totalDenominator == 0.0) {
				totalDenominator = avoidZero(totalDenominator);
			}
			for (int wordIndex = 0; wordIndex < nWords; wordIndex++) {
				phi[topicIndex][wordIndex] = numerators[wordIndex] / totalDenominator;
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
	 * The document topic distributions of the training documents.
	 *
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
	 * Replaces zero by a tiny positive constant to avoid divisions by zero.
	 *
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
