package org.hhn.topicgrouper.report;

public class ImprovementAssessor {
	private final double[] improvementWindow;
	private final int half;
	int next;

	public ImprovementAssessor(int windowSize) {
		improvementWindow = new double[windowSize];
		half = improvementWindow.length / 2;
		next = 0;
	}
	
	public int getHalf() {
		return half;
	}

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
