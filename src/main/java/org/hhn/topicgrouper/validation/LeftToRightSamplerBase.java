package org.hhn.topicgrouper.validation;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;

public abstract class LeftToRightSamplerBase<T> {
	protected final Random random;

	// For "left to right".
	protected final TIntList linearWordIndices;
	protected final double[] pzn;
	protected final int particles;

	public LeftToRightSamplerBase(Random random, int particles, int ntopics) {
		this.particles = particles;
		this.random = random;
		linearWordIndices = new TIntArrayList();
		pzn = new double[ntopics];
	}

	public double leftToRight(Document<T> d) {
		initFields();
		return leftToRightHelp(d);
	}

	protected void initFields() {
		linearWordIndices.clear();
	}

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

	public double leftToRightDocCompletion(Document<T> refDoc, Document<T> d) {
		initFields();
		if (refDoc != null) {
			leftToRightHelp(refDoc);
		}
		return leftToRightHelp(d);
	}

	protected abstract double leftToRightForWord(int n, int wordIndex);
	
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
