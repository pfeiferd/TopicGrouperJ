package org.hhn.topicgrouper.paper;

/*

Question: How often does line 50 get executed?

E(i) = expectation for |{(t, null) \in sl at step i}|

E(1) = 0

for i = 1 ... |V| - 2 {

|T| = |V| - i + 1

p(c(line 49) | step i) = E(i) / |T|

p(c(line 71) | i) <= 2 / (|T| - E(i))

E(i + 1) = E(i) + (1 - p(c(line 49) | step i)) * p(c(line 71) | i) - p(c(line 49) | step i)

}

*/
public class RecurrenceRelDeferredComp {
	public static void main(String[] args) {
		int v = 10000;
		double[] e = new double[v];
		e[1] = 0;
		double sumPicks = 0;
		for (int i = 1; i < v - 1; i++) {
			int t = v - i + 1;
			double pcline49 = e[i] / t; 
			double pcline71 =  2 / (t - e[i]);
			e[i + 1] = e[i] + (1 - pcline49) * pcline71 - pcline49;
			System.out.println(e[i]);
//			System.out.println(e[i] / t);
			sumPicks += e[i] / t;
		}
		System.out.println(sumPicks);
	}
}
