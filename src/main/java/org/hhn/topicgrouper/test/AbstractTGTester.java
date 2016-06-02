package org.hhn.topicgrouper.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.base.Solver.SolutionListener;
import org.hhn.topicgrouper.report.BasicSolutionReporter;
import org.hhn.topicgrouper.util.OutputStreamMultiplexer;

public abstract class AbstractTGTester<T> {
	private final PrintStream printStream;

	public AbstractTGTester(File outputFile) throws IOException {
		OutputStreamMultiplexer os = new OutputStreamMultiplexer();
		os.addOutputStream(System.out);
		if (outputFile != null) {
			os.addOutputStream(new FileOutputStream(outputFile));
		}
		printStream = new PrintStream(os);
	}

	public void run() {
		DocumentProvider<T> provider = createDocumentProvider();
		createSolver(provider).solve(createSolutionListener(printStream));
		done();
	}

	protected void done() {
	}

	protected SolutionListener<T> createSolutionListener(PrintStream out) {
		return new BasicSolutionReporter<T>(out, 4, true);
	}

	protected abstract DocumentProvider<T> createDocumentProvider();

	protected abstract Solver<T> createSolver(DocumentProvider<T> documentProvider);
}
