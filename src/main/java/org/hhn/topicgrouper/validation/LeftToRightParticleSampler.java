package org.hhn.topicgrouper.validation;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;

public abstract class LeftToRightParticleSampler<T> extends LeftToRightSamplerBase<T> {
	// For "left to right" with particles.
	protected final TIntList[] z;
	protected final int[][] lrTopicAssignmentCounts;

	public LeftToRightParticleSampler(Random random, int particles, int ntopics) {
		super(random, particles, ntopics);
		lrTopicAssignmentCounts = new int[particles][ntopics];

		z = new TIntList[particles];
		for (int i = 0; i < z.length; i++) {
			z[i] = new TIntArrayList();
		}
	}

	public double leftToRight(Document<T> d) {
		initFields();
		return leftToRightHelp(d);
	}

	protected void initFields() {
		super.initFields();
		for (int i = 0; i < z.length; i++) {
			Arrays.fill(lrTopicAssignmentCounts[i], 0);
			z[i].clear();
		}
	}
	
	@Override
	protected double leftToRightForWord(int n, int wordIndex) {
		linearWordIndices.add(wordIndex);

		double pn = 0;
		// According to Algorithm 3
		// Line 4
		for (int r = 0; r < z.length; r++) {
			for (int n2 = 0; n2 < n; n2++) {
				// Line 6
				int wordIndexN2 = linearWordIndices.get(n2);
				lrTopicAssignmentCounts[r][z[r].get(n2)]--;
				for (int k = 0; k < pzn.length; k++) {
					pzn[k] = getPhi(k, wordIndexN2)
							* (lrTopicAssignmentCounts[r][k] + getAlpha(k));
				}
				int zn2 = nextDiscrete(pzn);
				z[r].set(n2, zn2);
				lrTopicAssignmentCounts[r][zn2]++;
			}
			// Line 8
			for (int t = 0; t < pzn.length; t++) {
				pn += getPhi(t, wordIndex)
						* (lrTopicAssignmentCounts[r][t] + getAlpha(t))
						/ (n + getAlphaSum());
			}
			// Line 9
			for (int k = 0; k < pzn.length; k++) {
				pzn[k] = getPhi(k, wordIndex)
						* (lrTopicAssignmentCounts[r][k] + getAlpha(k));
			}
			int zn = nextDiscrete(pzn);
			z[r].add(zn);
			lrTopicAssignmentCounts[r][zn]++;
		}
		return pn / z.length;
	}

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

	protected abstract double getAlpha(int t);

	protected abstract double getAlphaSum();

	protected abstract double getPhi(int t, int wordIndex);
}
