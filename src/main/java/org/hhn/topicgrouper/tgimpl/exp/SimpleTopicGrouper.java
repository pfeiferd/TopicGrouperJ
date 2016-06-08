package org.hhn.topicgrouper.tgimpl.exp;

import gnu.trove.TIntCollection;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solution;
import org.hhn.topicgrouper.tgimpl.AbstractTopicGrouper;

public class SimpleTopicGrouper<T> extends AbstractTopicGrouper<T> {
	public SimpleTopicGrouper(int minWordFrequency, double lambda,
			DocumentProvider<T> documentProvider) {
		super(minWordFrequency, lambda, documentProvider);
	}

	protected double computeTopicLogLikelihood(TIntList topic1, int topicSize1,
			TIntList topic2, int topicSize2) {
		double res = 0;
		double logTopicSize = Math.log(topicSize1 + topicSize2);
		for (int i = 0; i < documents.size(); i++) {
			if (documentSizes[i] > 0) {
				double sum = 0;
				double topicFrPerDocument = 0;
				for (int j = 0; j < topic1.size(); j++) {
					double fr = documents.get(i)
							.getWordFrequency(topic1.get(j));
					topicFrPerDocument += fr;
					if (fr > 0) {
						sum += fr
								* (Math.log(documentProvider
										.getWordFrequency(topic1.get(j))) - logTopicSize);
					}
				}
				if (topic2 != null) {
					for (int j = 0; j < topic2.size(); j++) {
						double fr = documents.get(i).getWordFrequency(
								topic2.get(j));
						topicFrPerDocument += fr;
						if (fr > 0) {
							sum += fr
									* (Math.log(documentProvider
											.getWordFrequency(topic2.get(j))) - logTopicSize);
						}
					}
				}
				if (topicFrPerDocument > 0) {
					res += sum
							+ topicFrPerDocument
							* (Math.log(topicFrPerDocument) - logDocumentSizes[i]);
				}
			}
		}

		return res;
	}

	protected double computeTopicEntropy(TIntList topic1, TIntList topic2) {
		double res = 0;
		for (int i = 0; i < documents.size(); i++) {
			double topicFrPerDocument = 0;
			for (int j = 0; j < topic1.size(); j++) {
				topicFrPerDocument += documents.get(i).getWordFrequency(
						topic1.get(j));
			}
			if (topic2 != null) {
				for (int j = 0; j < topic2.size(); j++) {
					topicFrPerDocument += documents.get(i).getWordFrequency(
							topic2.get(j));
				}
			}
			if (topicFrPerDocument > 0) {
				res += topicFrPerDocument / documentSizes[i]
						* (Math.log(topicFrPerDocument) - logDocumentSizes[i]);
			}
		}
		return res;
	}

	@Override
	public void solve(SolutionListener<T> solutionListener) {
		final List<TIntList> topics = new ArrayList<TIntList>();
		final List<Double> topicLikelihoods = new ArrayList<Double>();
		final List<Integer> topicSizes = new ArrayList<Integer>();
		Solution<T> solution = new Solution<T>() {
			@Override
			public List<TIntList> getTopics() {
				return topics;
			}
			
			@Override
			public int getTopicForWord(int wordIndex) {
				throw new UnsupportedOperationException("not yet implemented");
			}

			@Override
			public T getWord(int wordIndex) {
				return SimpleTopicGrouper.this.documentProvider
						.getWord(wordIndex);
			}

			@Override
			public int getIndex(T word) {
				return SimpleTopicGrouper.this.documentProvider
						.getIndex(word);
			}
			
			@Override
			public int getGlobalWordFrequency(int wordIndex) {
				return documentProvider.getWordFrequency(wordIndex);
			}

			@Override
			public int getTopicFrequency(int topicIndex) {
				return topicSizes.get(topicIndex);
			}

			@Override
			public int getNumberOfTopics() {
				return topics.size();
			}

			@Override
			public double getTotalLikelhood() {
				double res = 0;
				for (double tl : topicLikelihoods) {
					res += tl;
				}
				return res;
			}

			@Override
			public TIntCollection[] getTopicsAlt() {
				return null;
			}

			@Override
			public TIntCollection getHomonymns() {
				return null;
			}

			@Override
			public double[] getTopicLikelihoods() {
				return null;
			}
		};

		solutionListener.beforeInitialization(-1, documentSizes.length);
		
		// Initialization
		int ne = documentProvider.getNumberOfWords();
		for (int i = 0; i < ne; i++) {
			TIntList topic = new TIntArrayList();
			if (documentProvider.getWordFrequency(i) >= minWordFrequency) {
				topic.add(i);
				topics.add(topic);
				int topicSize = getTopicSize(topic);
				topicSizes.add(topicSize);
				double tl = computeTopicLogLikelihood(topic, topicSize, null, 0);
				if (lambda != 0) {
					tl += lambda * computeTopicEntropy(topic, null);
				}

				topicLikelihoods.add(tl);
			}
		}
		solutionListener.initialized(solution);

		while (topics.size() > 1) {
			int s = 0, t = 0;
			double bestDiff = Double.NEGATIVE_INFINITY;
			double bestL = 0;
			for (int i = 1; i < topics.size(); i++) {
				for (int j = 0; j < i; j++) {
					TIntList a = topics.get(i);
					TIntList b = topics.get(j);
					double newL = computeTopicLogLikelihood(a,
							topicSizes.get(i), b, topicSizes.get(j));
					if (lambda != 0) {
						newL += lambda * computeTopicEntropy(a, b);
					}
					double newDiff = newL - topicLikelihoods.get(i)
							- topicLikelihoods.get(j);
					if (newDiff > bestDiff) {
						bestDiff = newDiff;
						bestL = newL;
						s = i;
						t = j;
					}
				}
			}
			// s > t!
			TIntList t1 = topics.get(s);
			TIntList t2 = topics.get(t);
			TIntList newTopic = t1;
			newTopic.addAll(t2);
			topics.remove(t);
			topicLikelihoods.set(s, bestL);
			int t1Size = topicSizes.get(s);
			int t2Size = topicSizes.get(t);
			topicSizes.set(s, t1Size + t2Size);
			topicLikelihoods.remove(t);
			topicSizes.remove(t);

			solutionListener.updatedSolution(s, t, bestDiff, t1Size, t2Size,
					solution);
		}
	}

	protected int getTopicSize(TIntList topic) {
		int topicSize = 0;
		for (int i = 0; i < topic.size(); i++) {
			topicSize += documentProvider.getWordFrequency(topic.get(i));
		}
		return topicSize;
	}
}
