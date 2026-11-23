package org.hhn.topicgrouper.tg.report.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * Reads a {@link TopicHierarchy} from a JSON file written by
 * {@link TopicHierarchyJsonWriter}. The words of the result are the strings of
 * the <code>vocab</code> array; the word ids are the indices into that array,
 * senses get ids above the vocabulary size. Reading does not recurse, so
 * arbitrarily deep hierarchies can be read. The nodes of the result are in
 * creation order: the word leaves by id, then the joins and splits by the
 * number of topics, each sense leaf right before the split node that joined it.
 */
public class TopicHierarchyJsonReader {
	/** Creates a reader; it has no settings. */
	public TopicHierarchyJsonReader() {
	}

	/**
	 * Reads a hierarchy from a file (UTF-8, gzip compressed if the name ends with <code>.gz</code>).
	 *
	 * @param file
	 *            the file
	 * @return the hierarchy
	 * @throws IOException
	 *             on read failure or if the file is not a hierarchy
	 */
	public TopicHierarchy<String> read(File file) throws IOException {
		InputStream in = new FileInputStream(file);
		try {
			if (file.getName().endsWith(".gz")) {
				in = new GZIPInputStream(in);
			}
			return read(new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8"))));
		} finally {
			in.close();
		}
	}

	/**
	 * Reads a hierarchy.
	 *
	 * @param in
	 *            the reader (not closed)
	 * @return the hierarchy
	 * @throws IOException
	 *             on read failure or if the input is not a hierarchy
	 */
	public TopicHierarchy<String> read(Reader in) throws IOException {
		JsonReader json = new JsonReader(in);
		disableNestingLimit(json);
		topicsOf.clear();
		List<String> vocab = null;
		List<MapNode<String>> roots = null;
		Map<Long, Integer> typeIds = new HashMap<Long, Integer>();
		json.beginObject();
		while (json.hasNext()) {
			String name = json.nextName();
			if ("format".equals(name)) {
				String format = json.nextString();
				if (!TopicHierarchyJsonWriter.FORMAT.equals(format)) {
					throw new IOException("not a topic hierarchy: format " + format);
				}
			} else if ("vocab".equals(name)) {
				vocab = new ArrayList<String>();
				json.beginArray();
				while (json.hasNext()) {
					vocab.add(json.nextString());
				}
				json.endArray();
			} else if ("roots".equals(name)) {
				if (vocab == null) {
					throw new IOException("the vocabulary must precede the roots");
				}
				roots = new ArrayList<MapNode<String>>();
				json.beginArray();
				while (json.hasNext()) {
					roots.add(readTree(json, vocab, typeIds));
				}
				json.endArray();
			} else {
				json.skipValue();
			}
		}
		json.endObject();
		if (roots == null) {
			throw new IOException("no roots in the input");
		}
		return new TopicHierarchy<String>(order(roots, vocab.size()));
	}

	/**
	 * Gson 2.12 and later limit the nesting depth to 255 by default; the limit
	 * is lifted if the method exists.
	 */
	private void disableNestingLimit(JsonReader json) {
		try {
			Method m = JsonReader.class.getMethod("setNestingLimit", int.class);
			m.invoke(json, Integer.MAX_VALUE);
		} catch (Exception e) {
			// Older Gson without a limit.
		}
	}

	/** The fields of a node while it is being read. */
	private static class Frame {
		int id;
		int topics = Integer.MIN_VALUE;
		int frequency;
		double logLikelihood;
		double deltaLogLikelihood;
		double totalLogLikelihood;
		boolean marked;
		int word = -1;
		int sense;
		int documents;
		List<WordInfo<String>> topWords = new ArrayList<WordInfo<String>>();
		List<MapNode<String>> children = new ArrayList<MapNode<String>>();
		boolean leaf = true;
		MapNode<String> result;
	}

	private MapNode<String> readTree(JsonReader json, List<String> vocab, Map<Long, Integer> typeIds)
			throws IOException {
		List<Frame> stack = new ArrayList<Frame>();
		Frame top = new Frame();
		stack.add(top);
		json.beginObject();
		while (!stack.isEmpty()) {
			Frame f = stack.get(stack.size() - 1);
			if (json.peek() == JsonToken.END_OBJECT) {
				json.endObject();
				f.result = build(f, vocab, typeIds);
				stack.remove(stack.size() - 1);
				if (!stack.isEmpty()) {
					stack.get(stack.size() - 1).children.add(f.result);
					if (json.peek() == JsonToken.END_ARRAY) {
						json.endArray();
					} else {
						json.beginObject();
						stack.add(new Frame());
					}
				}
				continue;
			}
			String name = json.nextName();
			if ("id".equals(name)) {
				f.id = json.nextInt();
			} else if ("topics".equals(name)) {
				f.topics = json.nextInt();
			} else if ("frequency".equals(name)) {
				f.frequency = json.nextInt();
			} else if ("logLikelihood".equals(name)) {
				f.logLikelihood = json.nextDouble();
			} else if ("deltaLogLikelihood".equals(name)) {
				f.deltaLogLikelihood = json.nextDouble();
			} else if ("totalLogLikelihood".equals(name)) {
				f.totalLogLikelihood = json.nextDouble();
			} else if ("marked".equals(name)) {
				f.marked = json.nextBoolean();
			} else if ("word".equals(name)) {
				f.word = json.nextInt();
			} else if ("sense".equals(name)) {
				f.sense = json.nextInt();
			} else if ("documents".equals(name)) {
				f.documents = json.nextInt();
			} else if ("topWords".equals(name)) {
				f.leaf = false;
				json.beginArray();
				while (json.hasNext()) {
					f.topWords.add(readTopWord(json, vocab, typeIds));
				}
				json.endArray();
			} else if ("children".equals(name)) {
				f.leaf = false;
				json.beginArray();
				if (json.peek() == JsonToken.END_ARRAY) {
					json.endArray();
				} else {
					json.beginObject();
					stack.add(new Frame());
				}
			} else {
				json.skipValue();
			}
		}
		return top.result;
	}

	private WordInfo<String> readTopWord(JsonReader json, List<String> vocab, Map<Long, Integer> typeIds)
			throws IOException {
		int word = -1;
		int sense = 0;
		int frequency = 0;
		json.beginObject();
		while (json.hasNext()) {
			String name = json.nextName();
			if ("word".equals(name)) {
				word = json.nextInt();
			} else if ("sense".equals(name)) {
				sense = json.nextInt();
			} else if ("frequency".equals(name)) {
				frequency = json.nextInt();
			} else {
				json.skipValue();
			}
		}
		json.endObject();
		return createInfo(word, sense, frequency, 0, vocab, typeIds);
	}

	private WordInfo<String> createInfo(int word, int sense, int frequency, int documents, List<String> vocab,
			Map<Long, Integer> typeIds) throws IOException {
		if (word < 0 || word >= vocab.size()) {
			throw new IOException("word index out of range: " + word);
		}
		return new WordInfo<String>(typeId(word, sense, vocab.size(), typeIds), frequency, vocab.get(word), sense,
				documents);
	}

	/** The word index for a word, a unique id above the vocabulary for a sense. */
	private int typeId(int word, int sense, int vocabSize, Map<Long, Integer> typeIds) {
		if (sense <= 0) {
			return word;
		}
		Long key = ((long) word << 32) | sense;
		Integer id = typeIds.get(key);
		if (id == null) {
			id = vocabSize + typeIds.size();
			typeIds.put(key, id);
		}
		return id;
	}

	private MapNode<String> build(Frame f, List<String> vocab, Map<Long, Integer> typeIds) throws IOException {
		List<WordInfo<String>> top = f.topWords;
		if (f.leaf) {
			if (f.word >= 0) {
				top.add(createInfo(f.word, f.sense, f.frequency, f.documents, vocab, typeIds));
			}
		} else if (f.children.isEmpty()) {
			throw new IOException("topic " + f.id + " without children");
		}
		MapNode<String> left = f.children.isEmpty() ? null : f.children.get(0);
		MapNode<String> right = f.children.size() < 2 ? null : f.children.get(1);
		if (f.children.size() > 2) {
			throw new IOException("topic " + f.id + " has more than two children");
		}
		MapNode<String> node = new MapNode<String>(f.id, left, right, top, f.totalLogLikelihood, f.logLikelihood,
				f.deltaLogLikelihood, f.frequency);
		node.setMarked(f.marked);
		if (left != null) {
			left.setParent(node);
		}
		if (right != null) {
			right.setParent(node);
		}
		if (f.topics != Integer.MIN_VALUE) {
			topicsOf.put(node, f.topics);
		}
		return node;
	}

	/** The "topics" field of split nodes while ordering (identity keyed). */
	private final Map<MapNode<String>, Integer> topicsOf = new java.util.IdentityHashMap<MapNode<String>, Integer>();

	/**
	 * Puts the nodes into creation order: word leaves by descending id (0, -1,
	 * -2, ...), then the inner nodes by descending number of topics, joins before
	 * splits at the same number, with every sense leaf directly before its parent.
	 */
	private List<MapNode<String>> order(List<MapNode<String>> roots, int words) {
		List<MapNode<String>> all = new ArrayList<MapNode<String>>();
		for (MapNode<String> root : roots) {
			List<MapNode<String>> stack = new ArrayList<MapNode<String>>();
			stack.add(root);
			while (!stack.isEmpty()) {
				MapNode<String> n = stack.remove(stack.size() - 1);
				all.add(n);
				if (n.getRightNode() != null) {
					stack.add(n.getRightNode());
				}
				if (n.getLeftNode() != null) {
					stack.add(n.getLeftNode());
				}
			}
		}
		List<MapNode<String>> wordLeaves = new ArrayList<MapNode<String>>();
		List<MapNode<String>> inner = new ArrayList<MapNode<String>>();
		for (MapNode<String> n : all) {
			if (n.isLeaf()) {
				if (!n.isSenseLeaf()) {
					wordLeaves.add(n);
				}
			} else {
				inner.add(n);
			}
		}
		Collections.sort(wordLeaves, new Comparator<MapNode<String>>() {
			public int compare(MapNode<String> a, MapNode<String> b) {
				return b.getId() - a.getId();
			}
		});
		Collections.sort(inner, new Comparator<MapNode<String>>() {
			public int compare(MapNode<String> a, MapNode<String> b) {
				int ta = topics(a);
				int tb = topics(b);
				if (ta != tb) {
					return tb - ta;
				}
				boolean sa = topicsOf.containsKey(a);
				boolean sb = topicsOf.containsKey(b);
				if (sa != sb) {
					return sa ? 1 : -1;
				}
				return a.getId() - b.getId();
			}
		});
		List<MapNode<String>> res = new ArrayList<MapNode<String>>(all.size());
		res.addAll(wordLeaves);
		for (MapNode<String> n : inner) {
			if (n.getLeftNode() != null && n.getLeftNode().isSenseLeaf()) {
				res.add(n.getLeftNode());
			}
			if (n.getRightNode() != null && n.getRightNode().isSenseLeaf()) {
				res.add(n.getRightNode());
			}
			res.add(n);
		}
		topicsOf.clear();
		return res;
	}

	private int topics(MapNode<String> n) {
		Integer t = topicsOf.get(n);
		return t == null ? n.getId() : t;
	}
}
