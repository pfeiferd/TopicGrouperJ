package org.hhn.topicgrouper.tg.report.store;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import com.google.gson.stream.JsonWriter;

/**
 * Writes a {@link TopicHierarchy} as a JSON file. The format is
 * <pre>
 * {
 *   "format": "topic-grouper-hierarchy", "version": 1,
 *   "vocab": ["the", "topic", ...],
 *   "roots": [ node, ... ]
 * }
 * </pre>
 * where <code>vocab</code> is the vocabulary of the grouped words (desensed:
 * every word once, in the order of the word ids) and every node is
 * <pre>
 * { "id": 17, "frequency": 1234,
 *   "logLikelihood": -1.5, "deltaLogLikelihood": -0.1, "totalLogLikelihood": -99.0,
 *   "marked": true,                       (only if marked)
 *   "topics": 40,                         (only for sense split nodes: the number of topics at the split)
 *   "topWords": [ {"word": 3, "sense": 1, "frequency": 20}, ... ],
 *   "children": [ node, node ] }
 * </pre>
 * for a topic and
 * <pre>
 * { "id": -3, "word": 3, "sense": 1, "documents": 12, "frequency": 20, ... }
 * </pre>
 * for a leaf; <code>word</code> is the index into <code>vocab</code>,
 * <code>sense</code> (omitted for 0) the sense number and <code>documents</code>
 * (omitted for 0) the number of documents in which the word has the sense. The
 * hierarchy is written as nested objects without recursion, so arbitrarily deep
 * hierarchies can be written. Files whose name ends with <code>.gz</code> are
 * compressed. The counterpart is {@link TopicHierarchyJsonReader}.
 *
 * @param <T>
 *            the word type (written via <code>toString()</code>)
 */
public class TopicHierarchyJsonWriter<T> {
	/** The value of the <code>format</code> field. */
	public static final String FORMAT = "topic-grouper-hierarchy";
	/** The value of the <code>version</code> field. */
	public static final int VERSION = 1;

	private final boolean pretty;

	/** Creates a writer for compact output. */
	public TopicHierarchyJsonWriter() {
		this(false);
	}

	/**
	 * Creates a writer.
	 *
	 * @param pretty
	 *            whether the output is indented
	 */
	public TopicHierarchyJsonWriter(boolean pretty) {
		this.pretty = pretty;
	}

	/**
	 * Writes a hierarchy to a file (UTF-8, gzip compressed if the name ends with <code>.gz</code>).
	 *
	 * @param hierarchy
	 *            the hierarchy
	 * @param file
	 *            the file
	 * @throws IOException
	 *             on write failure
	 */
	public void write(TopicHierarchy<T> hierarchy, File file) throws IOException {
		OutputStream out = new FileOutputStream(file);
		try {
			if (file.getName().endsWith(".gz")) {
				out = new GZIPOutputStream(out);
			}
			Writer w = new BufferedWriter(new OutputStreamWriter(out, Charset.forName("UTF-8")));
			write(hierarchy, w);
			w.flush();
		} finally {
			out.close();
		}
	}

	/**
	 * Writes a hierarchy.
	 *
	 * @param hierarchy
	 *            the hierarchy
	 * @param out
	 *            the writer (not closed)
	 * @throws IOException
	 *             on write failure
	 */
	public void write(TopicHierarchy<T> hierarchy, Writer out) throws IOException {
		Map<T, Integer> vocabIndex = new HashMap<T, Integer>();
		List<T> vocab = createVocab(hierarchy, vocabIndex);
		JsonWriter json = new JsonWriter(out);
		if (pretty) {
			json.setIndent("  ");
		}
		json.beginObject();
		json.name("format").value(FORMAT);
		json.name("version").value(VERSION);
		json.name("vocab").beginArray();
		for (T word : vocab) {
			json.value(String.valueOf(word));
		}
		json.endArray();
		json.name("roots").beginArray();
		for (MapNode<T> root : hierarchy.getRoots()) {
			writeTree(json, hierarchy, root, vocabIndex);
		}
		json.endArray();
		json.endObject();
		json.flush();
	}

	/**
	 * Collects the words of the leaves: the words (sense 0) in the order of
	 * their ids, then words that only occur as senses (should not happen).
	 */
	private List<T> createVocab(TopicHierarchy<T> hierarchy, Map<T, Integer> vocabIndex) {
		List<WordInfo<T>> infos = new ArrayList<WordInfo<T>>();
		for (MapNode<T> node : hierarchy.getNodes()) {
			WordInfo<T> info = node.getWordInfo();
			if (info != null && !info.isSense()) {
				infos.add(info);
			}
		}
		java.util.Collections.sort(infos, new java.util.Comparator<WordInfo<T>>() {
			public int compare(WordInfo<T> a, WordInfo<T> b) {
				return a.getWordId() < b.getWordId() ? -1 : (a.getWordId() == b.getWordId() ? 0 : 1);
			}
		});
		List<T> vocab = new ArrayList<T>();
		for (WordInfo<T> info : infos) {
			if (!vocabIndex.containsKey(info.getWord())) {
				vocabIndex.put(info.getWord(), vocab.size());
				vocab.add(info.getWord());
			}
		}
		for (MapNode<T> node : hierarchy.getNodes()) {
			WordInfo<T> info = node.getWordInfo();
			if (info != null && !vocabIndex.containsKey(info.getWord())) {
				vocabIndex.put(info.getWord(), vocab.size());
				vocab.add(info.getWord());
			}
		}
		return vocab;
	}

	private static class Frame<T> {
		final MapNode<T> node;
		final List<MapNode<T>> children;
		int next;

		Frame(MapNode<T> node, List<MapNode<T>> children) {
			this.node = node;
			this.children = children;
		}
	}

	private void writeTree(JsonWriter json, TopicHierarchy<T> hierarchy, MapNode<T> root,
			Map<T, Integer> vocabIndex) throws IOException {
		List<Frame<T>> stack = new ArrayList<Frame<T>>();
		stack.add(new Frame<T>(root, hierarchy.children(root)));
		while (!stack.isEmpty()) {
			Frame<T> f = stack.get(stack.size() - 1);
			if (f.next == 0) {
				writeNodeStart(json, hierarchy, f.node, vocabIndex);
			}
			if (f.next < f.children.size()) {
				MapNode<T> child = f.children.get(f.next++);
				stack.add(new Frame<T>(child, hierarchy.children(child)));
			} else {
				if (!f.node.isLeaf()) {
					json.endArray();
				}
				json.endObject();
				stack.remove(stack.size() - 1);
			}
		}
	}

	private void writeNodeStart(JsonWriter json, TopicHierarchy<T> hierarchy, MapNode<T> node,
			Map<T, Integer> vocabIndex) throws IOException {
		json.beginObject();
		json.name("id").value(node.getId());
		if (node.isLeaf()) {
			WordInfo<T> info = node.getWordInfo();
			if (info != null) {
				json.name("word").value(vocabIndex.get(info.getWord()));
				if (info.isSense()) {
					json.name("sense").value(info.getSense());
				}
				if (info.getDocuments() > 0) {
					json.name("documents").value(info.getDocuments());
				}
			}
		} else {
			int i = hierarchy.indexOf(node);
			if (i >= 0 && hierarchy.getTopicsAfter(i) != node.getId()) {
				json.name("topics").value(hierarchy.getTopicsAfter(i));
			}
		}
		json.name("frequency").value(node.getTopicFrequency());
		json.name("logLikelihood").value(node.getLikelihood());
		json.name("deltaLogLikelihood").value(node.getDeltaLikelihood());
		json.name("totalLogLikelihood").value(node.getTotalLikelihood());
		if (node.isMarked()) {
			json.name("marked").value(true);
		}
		if (!node.isLeaf()) {
			json.name("topWords").beginArray();
			for (WordInfo<T> info : node.getTopTopicWordInfos()) {
				json.beginObject();
				json.name("word").value(vocabIndex.get(info.getWord()));
				if (info.isSense()) {
					json.name("sense").value(info.getSense());
				}
				json.name("frequency").value(info.getFrequency());
				json.endObject();
			}
			json.endArray();
			json.name("children").beginArray();
		}
	}
}
