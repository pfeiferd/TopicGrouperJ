package org.hhn.topicgrouper.figures;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.report.ClusterSolutionReporter;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

import external.com.apporiented.algorithm.clustering.Cluster;
import external.com.apporiented.algorithm.clustering.visualization.DendrogramPanel;

public class DendroGramDemo extends OptimizedTGTester {
	private ClusterSolutionReporter<String> clusterSolutionReporter;

	public DendroGramDemo() throws IOException {
		super(null);
	}

	@Override
	protected Solver<String> createSolver(DocumentProvider<String> documentProvider) {
		return new OptimizedTopicGrouper<String>(1, 0, documentProvider, 1);
	}
	
	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		return new TWCLDAPaperDocumentGenerator(new Random(45), new double[] {
				5, 0.5, 0.5, 0.5 }, 6000, 10, 10, 60, 60, 0, null, 0.8, 0.8);
	}

	@Override
	protected SolutionListener<String> createSolutionListener(PrintStream out) {
		return clusterSolutionReporter = new ClusterSolutionReporter<String>();
	}

	@Override
	protected void done() {
		JFrame frame = new JFrame();
		frame.setSize(400, 300);
		frame.setLocation(400, 300);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		JPanel content = new JPanel();
		DendrogramPanel dp = new DendrogramPanel();

		frame.setContentPane(content);
		content.setBackground(Color.red);
		content.setLayout(new BorderLayout());
		content.add(dp, BorderLayout.CENTER);
		dp.setBackground(Color.WHITE);
		dp.setLineColor(Color.BLACK);
		dp.setScaleValueDecimals(0);
		dp.setScaleValueInterval(1);
		dp.setShowDistances(false);

		Cluster cluster = clusterSolutionReporter.getRootCluster();
		dp.setModel(cluster);
		frame.setVisible(true);
	}

	public static void main(String[] args) throws IOException {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		new DendroGramDemo().run();
	}
}