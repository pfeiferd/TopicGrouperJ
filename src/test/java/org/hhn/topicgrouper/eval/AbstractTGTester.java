package org.hhn.topicgrouper.eval;

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

	public Solver<T> run() {
		DocumentProvider<T> provider = createDocumentProvider();
		Solver<T> solver = createSolver(provider);
		SolutionListener<T> solutionListener = createSolutionListener(printStream);
		startSolving();
		solver.solve(solutionListener);
		done();
		return solver;
	}
	
	protected void startSolving() {		
	}
	
	protected void done() {
	}

	protected SolutionListener<T> createSolutionListener(PrintStream out) {
		return new BasicSolutionReporter<T>(out, 4, true);
	}

	protected abstract DocumentProvider<T> createDocumentProvider();

	protected abstract Solver<T> createSolver(DocumentProvider<T> documentProvider);
}
