package org.hhn.topicgrouper.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.AbstractTGTester;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.EHACTopicGrouper;

import external.com.apporiented.algorithm.clustering.Cluster;
import external.com.apporiented.algorithm.clustering.visualization.DendrogramPanel;

public class SimpleDendroGramDemo extends AbstractTGTester<String> {
	private ClusterSolutionReporter<String> clusterSolutionReporter;

	public SimpleDendroGramDemo() throws IOException {
		super(null);
	}
	
	@Override
	protected TGSolver<String> createSolver(
			DocumentProvider<String> documentProvider) {
		return new EHACTopicGrouper<String>(1, documentProvider, 1);
	}
	
	@Override
	protected DocumentProvider<String> createDocumentProvider() {
		return new TWCLDAPaperDocumentGenerator(new Random(45), new double[] {
				5, 0.5, 0.5, 0.5 }, 6000, 10, 10, 60, 60, 0, null, 0.8, 0.8);
	}

	@Override
	protected TGSolutionListener<String> createSolutionListener(PrintStream out, boolean fast) {
		return clusterSolutionReporter = new ClusterSolutionReporter<String>();
	}

	@Override
	protected void done(boolean fast) {
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
		new SimpleDendroGramDemo().run();
	}
}
