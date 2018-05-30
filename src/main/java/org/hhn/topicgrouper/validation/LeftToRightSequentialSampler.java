package org.hhn.topicgrouper.validation;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;
import java.util.Random;

public abstract class LeftToRightSequentialSampler<T> extends
		LeftToRightSamplerBase<T> {
	protected final int[] h;
	protected final int[] counts;
	protected final TIntList zz;

	public LeftToRightSequentialSampler(Random random, int particles,
			int ntopics) {
		super(random, particles, ntopics);
		h = new int[ntopics];
		counts = new int[ntopics];
		zz = new TIntArrayList();
	}

	/*
	 * According to
	 * https://link.springer.com/content/pdf/10.1007/978-3-642-05224-8_6.pdf
	 * section 3.6
	 */
	protected double leftToRightForWord(int n, int wordIndex) {
		linearWordIndices.add(wordIndex);

		double pn = 0;
		Arrays.fill(counts, 0);
		zz.clear();
		for (int r = 0; r < particles; r++) {
			Arrays.fill(h, 0);
			// Wrong in the paper: 
			// n2 < n and not n2 <= n (!) corresponding to 1)a)i) which should only loop to l - 1, instead of l
			for (int n2 = 0; n2 < n; n2++) {
				int wordIndexN2 = linearWordIndices.get(n2);
				// Flaw in the paper: initialization case (r = 0) not covered
				// there...
				if (r > 0) {
					counts[zz.get(n2)]--;
				}
				for (int k = 0; k < pzn.length; k++) {
					pzn[k] = getPhi(k, wordIndexN2) * (counts[k] + getAlpha(k));
				}
				int zn2 = nextDiscrete(pzn);
				h[zn2]++;
				if (r == 0) {
					zz.add(zn2);
				} else {
					zz.set(n2, zn2);
				}
				counts[zn2]++;
			}
			for (int t = 0; t < pzn.length; t++) {
				pn += getPhi(t, wordIndex) * (h[t] + getAlpha(t))
						/ (n + getAlphaSum());
			}
		}
		return pn / particles;
	}
}
