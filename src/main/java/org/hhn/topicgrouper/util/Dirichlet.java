/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.	For further
   information, see the file `LICENSE' included with this distribution. */

package org.hhn.topicgrouper.util;

import cc.mallet.util.Randoms;

/**
 * Various useful functions related to Dirichlet distributions.
 * 
 * @author Andrew McCallum and David Mimno
 */

public class Dirichlet {
	double magnitude = 1;
	double[] partition;

	Randoms random = null;

	/** A dirichlet parameterized with a single vector of positive reals */
	public Dirichlet(double[] p) {
		magnitude = 0;
		partition = new double[p.length];

		// Add up the total
		for (int i = 0; i < p.length; i++) {
			magnitude += p[i];
		}

		for (int i = 0; i < p.length; i++) {
			partition[i] = p[i] / magnitude;
		}
	}

	/**
	 * A symmetric dirichlet: E(X_i) = E(X_j) for all i, j
	 * 
	 * @param n
	 *            The number of dimensions
	 * @param alpha
	 *            The parameter for each dimension
	 */
	public Dirichlet(int size, double alpha) {
		magnitude = size * alpha;

		partition = new double[size];

		partition[0] = 1.0 / size;
		for (int i = 1; i < size; i++) {
			partition[i] = partition[0];
		}
	}

	private void initRandom() {
		if (random == null) {
			random = createRandom();
		}
	}

	protected Randoms createRandom() {
		return new Randoms();
	}

	public double[] nextDistribution() {
		double distribution[] = new double[partition.length];
		initRandom();

		// For each dimension, draw a sample from Gamma(mp_i, 1)
		double sum = 0;
		for (int i = 0; i < distribution.length; i++) {
			distribution[i] = random.nextGamma(partition[i] * magnitude, 1);
			if (distribution[i] <= 0) {
				distribution[i] = 0.0001;
			}
			sum += distribution[i];
		}

		// Normalize
		for (int i = 0; i < distribution.length; i++) {
			distribution[i] /= sum;
		}

		return distribution;
	}
}
