package org.hhn.topicgrouper.lda.report;

import info.monitorenter.gui.chart.Chart2D;
import info.monitorenter.gui.chart.ITrace2D;
import info.monitorenter.gui.chart.traces.Trace2DSimple;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintStream;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.validation.BasicPerplexityCalculator;

/**
 * Listener that periodically computes the perplexity on test documents during training and
 * plots it in a Swing chart.
 *
 * @param <T> the type of the words
 */
public class LDAPerplexityResultReporter<T> extends BasicLDAResultReporter<T> {
	private final ITrace2D trace;
	private final BasicPerplexityCalculator<T> calculator;
	private final DocumentProvider<T> testDocumentProvider;
	private final int perplexitySteps;

	/**
	 * Opens a window with the chart.
	 *
	 * @param testDocumentProvider the test documents
	 * @param pw where to print, may be null
	 * @param perplexitySteps compute the perplexity every this many iterations
	 * @param calculator the perplexity calculator
	 */
	public LDAPerplexityResultReporter(
			DocumentProvider<T> testDocumentProvider, PrintStream pw,
			int perplexitySteps, BasicPerplexityCalculator<T> calculator) {
		super(pw, 10);
		this.calculator = calculator;
		this.testDocumentProvider = testDocumentProvider;
		this.perplexitySteps = perplexitySteps;

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

	/**
	 * @return the chart trace
	 */
	protected ITrace2D getTrace() {
		return trace;
	}

	/**
	 * Called with each computed perplexity; prints it and adds it to the chart.
	 *
	 * @param step the iteration
	 * @param value the perplexity
	 */
	protected void perplexityComputed(int step, double value) {
		if (pw != null) {
			pw.print("Perplexity: ");
			pw.println(value);
		}
		trace.addPoint(step, value);
	}

	@Override
	public void beforeInitialization(LDAGibbsSampler<T> sampler) {
	}

	@Override
	public void initalizing(LDAGibbsSampler<T> sampler, int document) {
	}

	@Override
	public void initialized(LDAGibbsSampler<T> sampler) {
	}

	@Override
	public void updatedSolution(LDAGibbsSampler<T> sampler, int iteration) {
		if (iteration > 0 && iteration % perplexitySteps == 0) {
			double result = computePerplexity(sampler);
			perplexityComputed(iteration, result);
		}
	}

	/**
	 * Prints the topics and computes the perplexity of the current sampler state.
	 *
	 * @param sampler the sampler
	 * @return the perplexity on the test documents
	 */
	protected double computePerplexity(LDAGibbsSampler<T> sampler) {
		printTopics(pw, sampler, topWords);
		calculator.setTopicModeler(sampler);
		return calculator.computePerplexity(testDocumentProvider);
	}

	@Override
	public void done(LDAGibbsSampler<T> sampler) {
	}
}
