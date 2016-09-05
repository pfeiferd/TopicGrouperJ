package org.hhn.topicgrouper.report;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import info.monitorenter.gui.chart.Chart2D;
import info.monitorenter.gui.chart.ITrace2D;
import info.monitorenter.gui.chart.traces.Trace2DSimple;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.validation.PerplexityCalculator;

public class BasicSolutionReporter<T> implements TGSolutionListener<T> {
	private final ITrace2D trace;
	private final PrintStream pw;
	private final int reportDetailsAtTopicSize;
	private final PerplexityCalculator<T> perplexityCalculator;
	private DocumentProvider<T> testDocumentProvider;
	private final boolean derive;
	private final boolean verbose;
	private final ImprovementAssessor assessor;

	public BasicSolutionReporter(PrintStream pw, int reportDetailsAtTopicSize,
			boolean derive) {
		this(pw, reportDetailsAtTopicSize, derive, true, true);
	}

	public BasicSolutionReporter(PrintStream pw, int reportDetailsAtTopicSize,
			boolean derive, boolean verbose, boolean bowFactor) {
		this.pw = pw;
		this.derive = derive;
		this.verbose = verbose;

		this.assessor = new ImprovementAssessor(5);
		this.perplexityCalculator = new PerplexityCalculator<T>(bowFactor);

		this.reportDetailsAtTopicSize = reportDetailsAtTopicSize;

		// Create a chart:
		Chart2D chart = new Chart2D();
		// Create an ITrace:
		// Note that dynamic charts need limited amount of values!!!
		// trace.setColor(Color.RED);

		// Add the trace to the chart. This has to be done before adding points
		// (deadlock prevention):

		trace = new Trace2DSimple();
		chart.addTrace(trace);

		// Make it visible:
		// Create a frame.
		JFrame frame = new JFrame("MinimalDynamicChart");
		// add the chart to the frame:
		frame.getContentPane().add(chart, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(boxLayout);
		frame.getContentPane().add(panel, BorderLayout.WEST);

		frame.setSize(400, 300);
		// Enable the termination button [cross on the upper right edge]:
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.setVisible(true);
	}

	public void setTestDocumentProvider(DocumentProvider<T> documentProvider) {
		this.testDocumentProvider = documentProvider;
	}

	@Override
	public void beforeInitialization(int maxTopics, int documents) {
		pw.println("Processed words / max topics: " + maxTopics);
		pw.println("Processed documents: " + documents);
	}

	private Double lastImprovement = null;

	@Override
	public void updatedSolution(int newTopicIndex, int oldTopicIndex,
			double improvement, int t1Size, int t2Size, TGSolution<T> solution) {
		if (testDocumentProvider != null) {
			double p = perplexityCalculator.computePerplexity(
					testDocumentProvider, solution);
			pw.print("Perplexity: ");
			pw.println(p);
			trace.addPoint(solution.getNumberOfTopics(), p);
		} else {
			if (derive) {
				if (lastImprovement != null) {
					// double ratio = improvement / lastImprovement;
					// Keep the graph in boundaries:
					Double ratio = assessor.addImprovement(improvement);
					if (ratio != null && !Double.isInfinite(ratio)
							&& !Double.isNaN(ratio)) {
						if (ratio > 1.3) {
							ratio = 1.3d;
						} else if (ratio < 0.9) {
							ratio = 0.9d;
						}
						trace.addPoint(
								solution.getNumberOfTopics()
										- assessor.getHalf(), ratio);
					}
				}
			} else {
				trace.addPoint(solution.getNumberOfTopics(),improvement);
			}
		}
		lastImprovement = improvement;
		if (verbose) {
			pw.print("Improvement: ");
			pw.println(improvement);
			pw.print("Likelihood: ");
			pw.println(solution.getTotalLikelhood());
			pw.print("Number of topics: ");
			pw.println(solution.getNumberOfTopics());
			pw.println("New topic: ");
			printTopic(solution, solution.getTopics()[newTopicIndex], pw);
			pw.println("All topics: ");
			printTopics(solution, pw);
			TIntCollection homonyms = solution.getHomonymns();
			if (homonyms != null) {
				pw.print("Homonyms (");
				pw.print(homonyms.size());
				pw.print("): ");
				printTopic(solution, homonyms, pw);
			}
			pw.println("*****************************");
		}
		if (solution.getNumberOfTopics() <= reportDetailsAtTopicSize) {
			pw.print("Number of topics: ");
			pw.println(solution.getNumberOfTopics());

			int[][] sortedByFrequency = new int[solution.getNumberOfTopics()][];
			TIntCollection[] topics = solution.getTopics();

			int k = 0;
			for (int i = 0; i < topics.length; i++) {
				if (topics[i] != null) {
					int fr = solution.getTopicFrequency(i);
					sortedByFrequency[k++] = new int[] { fr, i };
				}
			}
			Arrays.sort(sortedByFrequency, new Comparator<int[]>() {
				@Override
				public int compare(int[] o1, int[] o2) {
					return o2[0] - o1[0];
				}
			});
			pw.println("Topic frequencies: ");
			for (int j = 0; j < sortedByFrequency.length
					&& sortedByFrequency[j] != null; j++) {
				pw.print(sortedByFrequency[j][0]);
				pw.print("; ");
			}
			pw.println();
			pw.println("Topics: ");
			for (int j = 0; j < sortedByFrequency.length
					&& sortedByFrequency[j] != null; j++) {
				printTopicDetails(solution, topics[sortedByFrequency[j][1]], pw);
			}
			pw.println("----------------------------");
		}
	}

	@Override
	public void initalizing(double percentage) {
		pw.print("Initialization at ");
		pw.print((int) (percentage * 100));
		pw.println("%.");
	}

	@Override
	public void initialized(TGSolution<T> initialSolution) {
	}

	@Override
	public void done() {
	}

	private void printTopics(TGSolution<T> solution, PrintStream pw) {
		for (TIntCollection topic : solution.getTopics()) {
			if (topic != null) {
				printTopic(solution, topic, pw);
			}
		}
	}

	private void printTopic(TGSolution<T> solution, TIntCollection topic,
			PrintStream pw) {
		pw.print("[");
		TIntIterator iterator = topic.iterator();
		while (iterator.hasNext()) {
			pw.print(solution.getWord(iterator.next()));
			pw.print(" ");
		}
		pw.println("]");
	}

	public static <T> void printTopicDetails(TGSolution<T> solution,
			TIntCollection topic, PrintStream pw) {
		TopicInfo<T>[] tuples = new TopicInfo[topic.size()];
		int i = 0;
		TIntIterator iterator = topic.iterator();
		while (iterator.hasNext()) {
			TopicInfo<T> t = new TopicInfo<T>();
			t.b = iterator.next();
			t.a = solution.getGlobalWordFrequency(t.b);
			t.s = solution.getWord(t.b);
			tuples[i++] = t;
		}
		Arrays.sort(tuples);

		for (TopicInfo<T> tuple : tuples) {
			pw.print(tuple.s);
			pw.print("; ");
			pw.print(tuple.a);
			pw.print(";  ");
		}
		pw.println();
	}

	private static class TopicInfo<T> implements Comparable<TopicInfo<T>> {
		public int a;
		public int b;
		public T s;

		@Override
		public int compareTo(TopicInfo<T> o) {
			return a < o.a ? 1 : (a == o.a ? 0 : -1);
		}

		@Override
		public String toString() {
			return (s != null ? s.toString() : b) + "; " + a + ")";
		}
	}

}
