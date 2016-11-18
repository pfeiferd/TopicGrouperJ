package org.hhn.topicgrouper.paper;

import org.apache.commons.math3.distribution.BinomialDistribution;

public class RecurrenceRelationForDeferredJCUpdatesInTG {
	static Double[][] h;
	static double c = 10;

	public static void main(String[] args) {
		for (int i = 1; i < 1000; i++) {
			h = new Double[i + 1][i + 1];
			System.out.println(i + "; " + s(i, 0));
		}
	}

	public static double s(int n, int e) {
		if (h[n][e] == null) {
			if (n <= 1) {
				h[n][e] = 0d;
			} else {
				double pUpdate = ((double) e) / n;
				if (n - 2 < e) {
					h[n][e] = pUpdate == 0 ? 0 : pUpdate * (1 + s(1, e - 1)) + (1 - pUpdate)
							* s(n - 1, e);
				}
				else if (n == 2) {
					h[n][e] = pUpdate == 0 ? 0 : pUpdate * (1 + s(1, e - 1)) + (1 - pUpdate)
							* s(n - 1, e + 1);					
				}
				else {
					double res = 0;
					double pNull = (n - 2 >= c ? c : n - 2) / (n - 2);
					BinomialDistribution b = new BinomialDistribution(
							n - e - 2, pNull);
					for (int i = 0; i <= n - e - 2; i++) {
						res += b.probability(i) * s(n - 1, e + i);
					}

					h[n][e] = (pUpdate == 0 ? 0 : pUpdate * (1 + s(n, e - 1))) + (1 - pUpdate)
							* res;
				}
			}
		}
		return h[n][e];
	}
}
