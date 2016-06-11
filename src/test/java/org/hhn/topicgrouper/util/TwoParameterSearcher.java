package org.hhn.topicgrouper.util;

import java.util.Arrays;


public abstract class TwoParameterSearcher {
	private final double[][] xyArea;

	private final int maxTries;

	public TwoParameterSearcher(double[][] xyArea, int maxTries) {
		this.xyArea = xyArea;
		this.maxTries = maxTries;
	}
	
	public double[][] getXyArea() {
		return xyArea;
	}

	public double search() {
		double bestRes = Double.NEGATIVE_INFINITY;
		int bestX = 0, bestY = 0;

		for (int i = 0; i < maxTries; i++) {
			double middleX = (xyArea[0][0] + xyArea[0][1]) / 2;
			double middleY = (xyArea[1][0] + xyArea[1][1]) / 2;
			for (int x = 0; x < 2; x++) {
				for (int y = 0; y < 2; y++) {
					double res = optFunction((middleX + xyArea[0][x]) / 2,
							(middleY + xyArea[1][y]) / 2);
					if (res > bestRes) {
						bestRes = res;
						bestX = x;
						bestY = y;
					}
				}

			}
			xyArea[0][bestX == 0 ? 1 : 0] = middleX;
			xyArea[1][bestY == 0 ? 1 : 0] = middleY;
		}
		return bestRes;
	}

	protected abstract double optFunction(double x, double y);
	
	public static void main(String[] args) {
		TwoParameterSearcher ps = new TwoParameterSearcher(new double[][] { {-1, 1 }, { -1, 1}}, 1000) {
			@Override
			protected double optFunction(double x, double y) {
				return -(((x + 0.5) * (x + 0.5)) + y * y);
			}
		};
		System.out.println(ps.search());
		System.out.println(Arrays.toString(ps.getXyArea()[0]));
		System.out.println(Arrays.toString(ps.getXyArea()[1]));		
	}
}
