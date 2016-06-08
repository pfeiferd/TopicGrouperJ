import info.monitorenter.gui.chart.Chart2D;
import info.monitorenter.gui.chart.ITrace2D;
import info.monitorenter.gui.chart.traces.Trace2DSimple;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class CCostEstimator {
	private final ITrace2D trace;

	public CCostEstimator() {
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
	
	public void graph(int nmax, double p) {
		for (int i = 1; i <= nmax; i++) {
			double cost = compute(i, p);
			trace.addPoint(i, cost);
			System.out.println(i + " " + cost);
		}
	}
	
	public static int compute(int n, double p) {
		int cost = 0;
		while (n > 0) {
			cost += (int)(n * (1 - p) + n * (1 - p) / 2 * n * p);
			n = (int) (n * p + (1 - p) / 2 * n);
		}
		return cost;
	}

	public static void main(String[] args) {
		new CCostEstimator().graph(1000, 0.5);
	}
}
