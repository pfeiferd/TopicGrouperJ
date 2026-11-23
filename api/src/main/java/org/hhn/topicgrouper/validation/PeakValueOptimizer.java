package org.hhn.topicgrouper.validation;


/**
 * Searches for the maximum of a unimodal function of one parameter by repeated interval
 * shrinking. Subclasses supply the function in {@link #test(double)}.
 */
public abstract class PeakValueOptimizer {
	/**
	 * Creates an optimizer.
	 */
	public PeakValueOptimizer() {
	}

	/**
	 * Finds the argument of the maximum of {@link #test(double)} in the given interval.
	 *
	 * @param minLambda lower bound of the search interval
	 * @param maxLambda upper bound of the search interval
	 * @param steps number of interval-shrinking steps
	 * @return the estimated maximizing argument
	 */
	public double optimizeLambda(double minLambda, double maxLambda, int steps) {
		double middleRes = test((minLambda + maxLambda) / 2);
		return optimizeLambda(minLambda, maxLambda, steps, middleRes);
	}

	// Assumes that we have a function that monotically rises up to its max and
	// then monotically falls again (or just one of the two).
	// We are tying to find the max.
	/**
	 * Recursive step of the search; the function value at the interval middle is already known.
	 *
	 * @param minLambda lower bound of the search interval
	 * @param maxLambda upper bound of the search interval
	 * @param steps remaining number of steps
	 * @param res2 the value of {@link #test(double)} at the middle of the interval
	 * @return the estimated maximizing argument
	 */
	public double optimizeLambda(double minLambda, double maxLambda, int steps,
			double res2) {
		System.out.println(steps + " " + minLambda + " " + maxLambda + " "
				+ res2);
		if (steps <= 0) {
			return (minLambda + maxLambda) / 2;
		}
		double diff = maxLambda - minLambda;
		double res1 = test(minLambda + diff * 0.25);
		double res3 = test(minLambda + diff * 0.75);

		if (res1 < res2 && res2 < res3) {
			return optimizeLambda((minLambda + maxLambda) / 2, maxLambda,
					steps - 1, res3);
		} else if (res1 > res2 && res2 > res3) {
			return optimizeLambda(minLambda, (minLambda + maxLambda) / 2,
					steps - 1, res1);
		} else if (res1 < res2 && res2 > res3) {
			return optimizeLambda(minLambda + diff * 0.25, minLambda + diff
					* 0.75, steps - 1, res2);
		} else if (res1 > res3) {
			// throw new IllegalStateException("unexpected case");
			return optimizeLambda(minLambda, (minLambda + maxLambda) / 2,
					steps - 1, res1);
		} else {
			// throw new IllegalStateException("unexpected case");
			return optimizeLambda((minLambda + maxLambda) / 2, maxLambda,
					steps - 1, res3);
		}
	}

	/**
	 * Evaluates the function to be maximized.
	 *
	 * @param value the argument
	 * @return the function value at the argument
	 */
	public abstract double test(double value);
}
