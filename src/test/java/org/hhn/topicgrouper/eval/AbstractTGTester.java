package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.report.BasicTGSolutionReporter;
import org.hhn.topicgrouper.tg.report.store.MapNode;
import org.hhn.topicgrouper.tg.report.store.MapNodeTGSolution;
import org.hhn.topicgrouper.util.OutputStreamMultiplexer;

public abstract class AbstractTGTester<T> {
	protected final PrintStream printStream;
	protected final OutputStreamMultiplexer os;

	public AbstractTGTester(File outputFile) {
		try {
			os = new OutputStreamMultiplexer();
			os.addOutputStream(System.out);
			if (outputFile != null) {
				os.addOutputStream(new FileOutputStream(outputFile));
			}
			printStream = new PrintStream(os);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void run() {
		DocumentProvider<T> provider = createDocumentProvider();
		TGSolver<T> solver = createSolver(provider);
		TGSolutionListener<T> solutionListener = createSolutionListener(
				printStream, false);
		startSolving();
		solver.solve(solutionListener);
		done(false);
	}

	public void run(List<MapNode<T>> allNodes) {
		MapNodeTGSolution<T> tgSolution = new MapNodeTGSolution<T>(allNodes);
		int maxTopics = tgSolution.getMaxTopics();
		TGSolutionListener<T> solutionListener = createSolutionListener(
				printStream, true);
		startSolving();
		for (int i = maxTopics; i > 0; i--) {
			if (isProcessSolutionForTopics(i)) {
				tgSolution.setNumberOfTopics(i);
				solutionListener.updatedSolution(-1, -1, 0, 0, 0, tgSolution);
			}
		}
		done(true);
	}

	protected boolean isProcessSolutionForTopics(int nTopics) {
		return true;
	}

	protected void startSolving() {
	}

	protected void done(boolean fast) {
		printStream.close();
	}

	protected TGSolutionListener<T> createSolutionListener(PrintStream out,
			boolean fast) {
		return new BasicTGSolutionReporter<T>(out, 4, false);
	}

	protected abstract DocumentProvider<T> createDocumentProvider();

	protected abstract TGSolver<T> createSolver(
			DocumentProvider<T> documentProvider);
}
