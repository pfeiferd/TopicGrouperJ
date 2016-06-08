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
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solution;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.validation.PerplexityCalculator;

public class BasicSolutionReporter<T> implements SolutionListener<T> {
	private final ITrace2D trace;
	private final PrintStream pw;
	private final int reportDetailsAtTopicSize;
	private final PerplexityCalculator<T> perplexityCalculator;
	private DocumentProvider<T> testDocumentProvider;
	private final boolean derive;

	public BasicSolutionReporter(PrintStream pw, int reportDetailsAtTopicSize,
			boolean derive) {
		this.pw = pw;
		this.derive = derive;

		this.perplexityCalculator = new PerplexityCalculator<T>();

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
	
	private Double lastImprovement = Double.NaN;

	@Override
	public void updatedSolution(int newTopicIndex, int oldTopicIndex,
			double improvement, int t1Size, int t2Size, Solution<T> solution) {
		if (testDocumentProvider != null) {
			double p = perplexityCalculator.computePerplexity(
					testDocumentProvider, solution);
			pw.print("Perplexity: ");
			pw.println(p);
			trace.addPoint(solution.getNumberOfTopics(), p);
		} else {
			if (derive) {
				if (lastImprovement != Double.NaN) {
					trace.addPoint(solution.getNumberOfTopics(), improvement
							/ lastImprovement);
				}
			}
			else {
				trace.addPoint(solution.getNumberOfTopics(), improvement);				
			}
		}
		lastImprovement = improvement;
		pw.print("Improvement: ");
		pw.println(improvement);
		pw.print("Likelihood: ");
		pw.println(solution.getTotalLikelhood());
		List<? extends TIntCollection> topics = solution.getTopics();
		pw.print("Number of topics: ");
		pw.println(solution.getNumberOfTopics());
		pw.println("New topic: ");
		if (topics != null) {
			printTopic(solution, topics.get(newTopicIndex), pw);
		} else {
			printTopic(solution, solution.getTopicsAlt()[newTopicIndex], pw);
		}
		pw.println("All topics: ");
		if (topics != null) {
			printTopics(solution, pw);
		} else {
			printTopics2(solution, pw);
		}
		TIntCollection homonyms = solution.getHomonymns();
		if (homonyms != null) {
			pw.print("Homonyms: ");
			printTopic(solution, homonyms, pw);
		}
		pw.println("*****************************");
	}

	@Override
	public void initalizing(double percentage) {
		pw.print("Initialization at ");
		pw.print((int) (percentage * 100));
		pw.println("%.");
	}
	
	@Override
	public void initialized(Solution<T> initialSolution) {
	}

	private void printTopics(Solution<T> solution, PrintStream pw) {
		for (TIntCollection topic : solution.getTopics()) {
			printTopic(solution, topic, pw);
		}
	}

	private void printTopics2(Solution<T> solution, PrintStream pw) {
		int counter = 0;
		for (TIntCollection topic : solution.getTopicsAlt()) {
			if (topic != null) {
				printTopic(solution, topic, pw);
				counter++;
			}
		}
		if (counter == reportDetailsAtTopicSize) {
			for (TIntCollection topic : solution.getTopicsAlt()) {
				if (topic != null) {
					printTopicDetails(solution, topic, pw);
				}
			}
			System.out.print("Topic frequencies: [");
			int i = 0;
			for (TIntCollection topic : solution.getTopicsAlt()) {
				if (topic != null) {
					System.out.print(solution.getTopicFrequency(i));
					System.out.print(" ");
				}
				i++;
			}
			System.out.println("]");
		}
	}

	private void printTopic(Solution<T> solution, TIntCollection topic,
			PrintStream pw) {
		pw.print("[");
		TIntIterator iterator = topic.iterator();
		while (iterator.hasNext()) {
			pw.print(solution.getWord(iterator.next()));
			pw.print(" ");
		}
		pw.println("]");
	}

	private void printTopicDetails(Solution<T> solution, TIntCollection topic,
			PrintStream pw) {
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

		pw.println(Arrays.asList(tuples));
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
			return (s != null ? s.toString() : b) + " (" + a + ")";
		}
	}

}
