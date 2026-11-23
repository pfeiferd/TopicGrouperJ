package org.hhn.topicgrouper.eval.ecir2019;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.Csv;
import org.hhn.topicgrouper.eval.Figures;
import org.hhn.topicgrouper.eval.TopicGrouperFactory;
import org.hhn.topicgrouper.eval.Settings;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.TGSolutionListenerMultiplexer;
import org.hhn.topicgrouper.tg.paint.DefaultTopicTreePainter;
import org.hhn.topicgrouper.tg.report.FreeMindXMLTopicHierarchyWriter;
import org.hhn.topicgrouper.tg.report.MindMapSolutionReporter;
import org.hhn.topicgrouper.tg.report.store.TopicHierarchy;
import org.hhn.topicgrouper.tg.report.store.TopicHierarchyJsonWriter;

/**
 * The qualitative examples of the ECIR 2019 paper: the top words of the topics
 * of a corpus at a given number of topics (Table of Section 5, AP at 40 topics)
 * and the whole topic hierarchy as a FreeMind mind map (the demos of the
 * original code), as a JSON file for the hierarchy browser and as an SVG
 * drawing of the hierarchy down to the example number of topics
 * (<code>figs/hierarchy-&lt;name&gt;.svg</code>). With
 * {@link #run(String, DocumentProvider, int, TGSolver, boolean)} any solver
 * can be used (e.g. the sense splitting one) and the whole hierarchy can be
 * drawn in addition.
 */
public class TopicExamples {
	private static final int TOP_WORDS = 10;

	private final Settings settings;
	private final File out;

	/**
	 * Creates the experiment.
	 *
	 * @param settings
	 *            experiment size
	 * @param out
	 *            output directory
	 */
	public TopicExamples(Settings settings, File out) {
		this.settings = settings;
		this.out = out;
	}

	/**
	 * Runs Topic Grouper down to one topic, writes <code>topics-&lt;name&gt;.csv</code>
	 * with the columns <code>topic;size;rank;word;frequency</code> for the
	 * solution with {@link Settings#exampleTopics} topics (topics ordered by
	 * size) and <code>&lt;name&gt;.mm</code> with the hierarchy.
	 *
	 * @param name
	 *            name of the corpus
	 * @param provider
	 *            the documents
	 */
	public void run(String name, DocumentProvider<String> provider) {
		run(name, provider, 1);
	}

	/**
	 * Like {@link #run(String, DocumentProvider)}, grouping only words with at
	 * least the given frequency.
	 *
	 * @param name
	 *            name of the corpus
	 * @param provider
	 *            the documents
	 * @param minWordFrequency
	 *            only words occurring at least this often are grouped
	 */
	public void run(final String name, final DocumentProvider<String> provider, int minWordFrequency) {
		run(name, provider, minWordFrequency, new TopicGrouperFactory().create(provider, minWordFrequency), false);
	}

	/**
	 * Like {@link #run(String, DocumentProvider, int)} with a given solver;
	 * optionally the whole hierarchy is drawn as
	 * <code>&lt;name&gt;-full.svg</code> as well.
	 *
	 * @param name
	 *            name of the corpus (and of the output files)
	 * @param provider
	 *            the documents
	 * @param minWordFrequency
	 *            the minimum word frequency the solver was created with (for the
	 *            topic table)
	 * @param solver
	 *            the solver, running down to one topic
	 * @param fullDepth
	 *            whether the whole hierarchy is drawn in addition
	 */
	public void run(final String name, final DocumentProvider<String> provider, int minWordFrequency,
			TGSolver<String> solver, final boolean fullDepth) {
		File file = new File(out, "topics-" + name + ".csv");
		if (Csv.skipIfDone(file)) {
			return;
		}
		final Csv csv = new Csv(file, "topic", "size", "rank", "word", "frequency");
		final MindMapSolutionReporter<String> mindMap = new MindMapSolutionReporter<String>(5, false, 1.1, 0);
		TGSolutionListenerMultiplexer<String> listeners = new TGSolutionListenerMultiplexer<String>();
		listeners.addSolutionListener(mindMap);
		listeners.addSolutionListener(new TGSolutionListener<String>() {
			public void updatedSolution(int newTopicIndex, int oldTopicIndex, double improvement,
					int t1Size, int t2Size, TGSolution<String> solution) {
				if (solution.getNumberOfTopics() == settings.exampleTopics) {
					writeTopics(csv, solution, provider);
				}
			}

			public void beforeInitialization(int maxTopics, int documents) {
			}

			public void initalizing(double percentage) {
			}

			public void initialized(TGSolution<String> initialSolution) {
			}

			public void done() {
				try {
					OutputStream mm = new FileOutputStream(new File(out, name + ".mm"));
					try {
						new FreeMindXMLTopicHierarchyWriter<String>(true).writeToFile(mm,
								mindMap.getCurrentNodes().values());
					} finally {
						mm.close();
					}
					TopicHierarchy<String> hierarchy = mindMap.getHierarchy();
					new TopicHierarchyJsonWriter<String>().write(hierarchy, new File(out, name + ".json.gz"));
					writeSvg(hierarchy, new File(new Figures(out).getDirectory(), "hierarchy-" + name + ".svg"),
							settings.exampleTopics);
					if (fullDepth) {
						writeSvg(hierarchy, new File(out, name + "-full.svg"), 0);
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		try {
			solver.solve(listeners);
		} catch (RuntimeException | Error e) {
			csv.discard();
			throw e;
		}
		csv.close();
	}

	/**
	 * Draws the hierarchy down to a number of topics (the topics of that
	 * solution as collapsed lines with their top words) or completely.
	 *
	 * @param hierarchy
	 *            the recorded hierarchy
	 * @param file
	 *            the SVG file
	 * @param topics
	 *            the number of topics to stop at, 0 for the whole hierarchy
	 * @throws IOException
	 *             on write failure
	 */
	private void writeSvg(TopicHierarchy<String> hierarchy, File file, int topics) throws IOException {
		new DefaultTopicTreePainter<String>(hierarchy).setTopics(topics).setTopWords(5)
				.setLabelParts(true, true, true, false).writeSvg(file);
	}

	private void writeTopics(Csv csv, final TGSolution<String> solution, DocumentProvider<String> provider) {
		List<Integer> ids = new ArrayList<Integer>();
		for (int id : solution.getTopicIds()) {
			ids.add(id);
		}
		Collections.sort(ids, new Comparator<Integer>() {
			public int compare(Integer a, Integer b) {
				return solution.getTopicFrequency(b) - solution.getTopicFrequency(a);
			}
		});
		int rank = 0;
		for (int id : ids) {
			rank++;
			TIntCollection words = solution.getTopics()[id];
			List<int[]> byFrequency = new ArrayList<int[]>();
			TIntIterator it = words.iterator();
			while (it.hasNext()) {
				int w = it.next();
				byFrequency.add(new int[] { w, solution.getGlobalWordFrequency(w) });
			}
			Collections.sort(byFrequency, new Comparator<int[]>() {
				public int compare(int[] a, int[] b) {
					return b[1] - a[1];
				}
			});
			for (int i = 0; i < Math.min(TOP_WORDS, byFrequency.size()); i++) {
				csv.row(rank, solution.getTopicFrequency(id), i + 1,
						provider.getVocab().getWord(byFrequency.get(i)[0]), byFrequency.get(i)[1]);
			}
		}
	}
}
