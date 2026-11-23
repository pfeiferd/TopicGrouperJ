package org.hhn.topicgrouper.tg.report;

/**
 * Sliding-window helper that compares a (delayed) improvement value with the average of the other values in the window.
 */
public class ImprovementAssessor {
	/**
	 * Circular buffer of the most recent improvements.
	 */
	private final double[] improvementWindow;
	/**
	 * Half the window size, i.e. the delay of the assessed value.
	 */
	private final int half;
	/**
	 * Total number of improvements added so far.
	 */
	int next;

	/**
	 * Creates an assessor.
	 * @param windowSize number of improvements kept in the window
	 */
	public ImprovementAssessor(int windowSize) {
		improvementWindow = new double[windowSize];
		half = improvementWindow.length / 2;
		next = 0;
	}
	
	/**
	 * Returns half the window size, the offset of the assessed value behind the latest one.
	 * @return half the window size
	 */
	public int getHalf() {
		return half;
	}

	/**
	 * Adds an improvement and, once the window is full, returns the ratio of the window's middle value to the average of the remaining values.
	 * @param improvement the latest improvement
	 * @return the ratio, or null while the window is not yet full
	 */
	public Double addImprovement(double improvement) {
		improvementWindow[next % improvementWindow.length] = improvement;
		next++;
		if (next >= improvementWindow.length) {
			double avg = 0;
			int middle = (next - half - 1) % improvementWindow.length ;
			for (int i = 0; i < improvementWindow.length; i++) {
				if (i != middle) {
					avg += improvementWindow[i];
				}
			}
			avg = avg / (improvementWindow.length - 1);
			return improvementWindow[middle] / avg;
		}
		return null;
	}
}
