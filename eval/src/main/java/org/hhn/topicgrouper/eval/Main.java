package org.hhn.topicgrouper.eval;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Command line of the evaluation:
 * <code>[experiments] [--data DIR] [--out DIR] [--quick] [--lda-burn-in N] [--lda-iterations N]</code>,
 * where <code>experiments</code> is a comma separated list of experiment names,
 * paper names or <code>all</code> (see {@link Evaluation}), <code>--data</code>
 * the data directory (default <code>data</code>), <code>--out</code> the result
 * directory (default <code>results</code>), <code>--quick</code> selects the
 * small sizes of {@link Settings#quick()}, and <code>--lda-burn-in</code> and
 * <code>--lda-iterations</code> override the burn-in and sampling iterations of
 * the LDA models of the perplexity, synthetic series and homonym experiments
 * ({@link Settings#ldaBurnIn}, {@link Settings#ldaIterations}).
 */
public final class Main {
	private Main() {
	}

	/**
	 * Parses the command line and runs the selected experiments.
	 *
	 * @param args
	 *            see the class comment
	 */
	public static void main(String[] args) {
		File data = new File("data");
		File out = new File("results");
		Settings settings = Settings.full();
		int ldaBurnIn = -1;
		int ldaIterations = -1;
		Set<String> names = new LinkedHashSet<String>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--data")) {
				data = new File(args[++i]);
			} else if (args[i].equals("--out")) {
				out = new File(args[++i]);
			} else if (args[i].equals("--quick")) {
				settings = Settings.quick();
			} else if (args[i].equals("--lda-burn-in")) {
				ldaBurnIn = positiveInt(args[i], args[++i]);
			} else if (args[i].equals("--lda-iterations")) {
				ldaIterations = positiveInt(args[i], args[++i]);
			} else if (args[i].startsWith("--")) {
				usage("unknown option " + args[i]);
				return;
			} else {
				names.addAll(Arrays.asList(args[i].split(",")));
			}
		}
		if (ldaBurnIn >= 0) {
			settings.ldaBurnIn = ldaBurnIn;
		}
		if (ldaIterations >= 0) {
			settings.ldaIterations = ldaIterations;
		}
		Set<String> experiments = new LinkedHashSet<String>();
		if (names.isEmpty() || names.contains("all")) {
			for (List<String> e : Evaluation.getPapers().values()) {
				experiments.addAll(e);
			}
		}
		for (String name : names) {
			List<String> e = Evaluation.experimentsOf(name);
			if (e.isEmpty() && !name.equals("all")) {
				usage("unknown experiment " + name);
				return;
			}
			experiments.addAll(e);
		}
		Evaluation evaluation = new Evaluation(new Datasets(data, settings.maxDocuments), settings, out);
		for (String e : experiments) {
			evaluation.run(e);
		}
	}

	/**
	 * Parses the value of a numeric option.
	 *
	 * @param option
	 *            the option name (for the error message)
	 * @param value
	 *            the argument following it
	 * @return the value as a non-negative integer
	 * @throws IllegalArgumentException
	 *             if the value is not a non-negative integer
	 */
	private static int positiveInt(String option, String value) {
		try {
			int n = Integer.parseInt(value);
			if (n < 0) {
				throw new NumberFormatException();
			}
			return n;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(option + " needs a non-negative integer, got " + value);
		}
	}

	private static void usage(String error) {
		System.err.println(error);
		System.err.println("usage: [experiment|paper|all][,...] [--data DIR] [--out DIR] [--quick]"
				+ " [--lda-burn-in N] [--lda-iterations N]");
		System.err.println("papers and experiments: " + Evaluation.getPapers());
	}
}
