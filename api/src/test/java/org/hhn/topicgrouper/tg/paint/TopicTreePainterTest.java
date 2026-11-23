package org.hhn.topicgrouper.tg.paint;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.hhn.topicgrouper.tg.report.store.MapNode;
import org.hhn.topicgrouper.tg.report.store.TopicHierarchy;
import org.hhn.topicgrouper.tg.report.store.WordInfo;

/**
 * Tests the tree painter on the SVG and the Graphics2D canvas.
 */
public class TopicTreePainterTest extends TestCase {
	/**
	 * ((a, b), (c, d'1)) with a sense leaf; ids: leaves 0..-2, sense -3, joins 3, 2, 1.
	 */
	private TopicHierarchy<String> createHierarchy() {
		List<MapNode<String>> nodes = new ArrayList<MapNode<String>>();
		MapNode<String> a = leaf(0, "a", 0, 30);
		MapNode<String> b = leaf(-1, "b", 0, 20);
		MapNode<String> c = leaf(-2, "c", 0, 10);
		nodes.add(a);
		nodes.add(b);
		nodes.add(c);
		MapNode<String> ab = join(3, a, b, -2);
		nodes.add(ab);
		MapNode<String> d1 = leaf(-3, "d", 1, 5);
		nodes.add(d1);
		MapNode<String> cd = join(4, c, d1, -0.5); // a split node: id >= number of words (3)
		nodes.add(cd);
		MapNode<String> root = join(1, ab, cd, -40);
		root.setMarked(true);
		nodes.add(root);
		return new TopicHierarchy<String>(nodes);
	}

	private MapNode<String> leaf(int id, String word, int sense, int fr) {
		List<WordInfo<String>> list = new ArrayList<WordInfo<String>>();
		list.add(new WordInfo<String>(sense == 0 ? -id : 100 - id, fr, word, sense, sense == 0 ? 0 : 3));
		return new MapNode<String>(id, null, null, list, -100, 0, 0, fr);
	}

	private MapNode<String> join(int id, MapNode<String> l, MapNode<String> r, double delta) {
		List<WordInfo<String>> top = new ArrayList<WordInfo<String>>();
		top.addAll(l.getTopTopicWordInfos());
		top.addAll(r.getTopTopicWordInfos());
		MapNode<String> n = new MapNode<String>(id, l, r, top, -100, -50, delta, l.getTopicFrequency()
				+ r.getTopicFrequency());
		l.setParent(n);
		r.setParent(n);
		return n;
	}

	private int count(String s, String part) {
		int c = 0;
		for (int i = s.indexOf(part); i >= 0; i = s.indexOf(part, i + 1)) {
			c++;
		}
		return c;
	}

	public void testSvgOfWholeHierarchy() throws Exception {
		TopicHierarchy<String> h = createHierarchy();
		DefaultTopicTreePainter<String> painter = new DefaultTopicTreePainter<String>(h);
		painter.setLabelParts(true, true, true, false).setNodeIndent(2, true).setDashedCostThreshold(0.1);
		StringWriter sw = new StringWriter();
		painter.writeSvg(sw);
		String svg = sw.toString();
		assertTrue(svg.startsWith("<?xml"));
		assertTrue(svg.contains("<svg xmlns=\"http://www.w3.org/2000/svg\""));
		assertEquals(svg, 7, count(svg, "<text "));
		assertTrue(svg, svg.contains(">a [30]</text>"));
		assertTrue(svg, svg.contains(">d'1 [5, 3 docs]</text>"));
		assertTrue(svg, svg.contains("font-style=\"italic\""));
		assertTrue(svg, svg.contains("(1) a, b, c, d'1 [65, 4 words, c=0.615]"));
		assertTrue(svg, svg.contains("(3) a, b [50, 2 words, c=0.040]"));
		painter.setShowTopicSize(false);
		sw = new StringWriter();
		painter.writeSvg(sw);
		assertTrue(sw.toString(), sw.toString().contains("(1) a, b, c, d'1 [65, c=0.615]"));
		painter.setShowTopicSize(true);
		// The marked root has no connector (it is the top line); the costs of (3) and (4) are
		// 2/50 = 0.04 and 0.5/15 = 0.033, both below the threshold: nothing is dashed.
		assertEquals(svg, 0, count(svg, "stroke-dasharray"));
		// One connector per non-root row (6) and one vertical line per expanded node (3).
		assertEquals(svg, 9, count(svg, "<line "));
		painter.setDashedCostThreshold(0.035);
		sw = new StringWriter();
		painter.writeSvg(sw);
		assertEquals(sw.toString(), 1, count(sw.toString(), "stroke-dasharray"));
		assertEquals(sw.toString(), 1,
				count(sw.toString(), "font-weight=\"bold\" fill=\"#000000\" xml:space=\"preserve\">(3)"));
	}

	public void testPruning() throws Exception {
		TopicHierarchy<String> h = createHierarchy();
		DefaultTopicTreePainter<String> painter = new DefaultTopicTreePainter<String>(h).setTopics(2);
		StringWriter sw = new StringWriter();
		painter.writeSvg(sw);
		String svg = sw.toString();
		assertEquals(3, count(svg, "<text "));
		assertTrue(svg.contains("(3) a, b [50, 2 words]"));
		assertTrue(svg.contains("(4) c, d'1 [15, 2 words]"));

		painter = new DefaultTopicTreePainter<String>(h).setMaxDepth(1);
		sw = new StringWriter();
		painter.writeSvg(sw);
		assertEquals(3, count(sw.toString(), "<text "));

		painter = new DefaultTopicTreePainter<String>(h).setMinTopicSize(3);
		sw = new StringWriter();
		painter.writeSvg(sw);
		assertEquals(3, count(sw.toString(), "<text "));

		List<MapNode<String>> roots = new ArrayList<MapNode<String>>();
		roots.add(h.get(3));
		painter = new DefaultTopicTreePainter<String>(h, roots);
		sw = new StringWriter();
		painter.writeSvg(sw);
		assertEquals(3, count(sw.toString(), "<text "));
		assertFalse(sw.toString().contains(">c ["));
	}

	public void testGraphics2DCanvasAndSize() throws Exception {
		TopicHierarchy<String> h = createHierarchy();
		DefaultTopicTreePainter<String> painter = new DefaultTopicTreePainter<String>(h);
		Dimension size = painter.getSize();
		assertTrue(size.width > 50);
		assertTrue(size.height > 7 * 12);
		BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		Dimension painted = painter.paintTree(g);
		g.dispose();
		// The same layout; the text widths of the two metrics may differ by a few pixels.
		assertEquals(size.height, painted.height);
		assertTrue(Math.abs(size.width - painted.width) < size.width * 0.1);
		// Something was drawn: not all pixels are black (the image starts black).
		boolean nonBlack = false;
		for (int x = 0; x < image.getWidth() && !nonBlack; x++) {
			for (int y = 0; y < image.getHeight() && !nonBlack; y++) {
				nonBlack = (image.getRGB(x, y) & 0xffffff) != 0;
			}
		}
		assertTrue(nonBlack);
		File file = File.createTempFile("tree", ".svg");
		try {
			painter.writeSvg(file);
			assertTrue(file.length() > 200);
		} finally {
			file.delete();
		}
	}

	public void testDeepHierarchy() throws Exception {
		List<MapNode<String>> nodes = new ArrayList<MapNode<String>>();
		int words = 10000;
		for (int i = 0; i < words; i++) {
			nodes.add(leaf(-i, "w" + i, 0, 1));
		}
		MapNode<String> current = nodes.get(0);
		for (int i = 1; i < words; i++) {
			current = join(words - i, current, nodes.get(i), -1);
			nodes.add(current);
		}
		TopicHierarchy<String> h = new TopicHierarchy<String>(nodes);
		SvgTreeCanvas canvas = new SvgTreeCanvas();
		Dimension size = new DefaultTopicTreePainter<String>(h).paintTree(canvas);
		assertEquals(2 * words - 1, count(canvas.getBody(), "<text "));
		// Connectors for all but the root plus one vertical line per inner node.
		assertEquals(2 * words - 2 + words - 1, count(canvas.getBody(), "<line "));
		assertTrue(size.height > words);
	}

	public void testApproximateMetrics() {
		java.awt.Font font = new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 12);
		java.awt.FontMetrics fm = new SvgTreeCanvas.ApproximateFontMetrics(font);
		assertTrue(fm.getAscent() > fm.getDescent());
		assertEquals(fm.getAscent() + fm.getDescent() + fm.getLeading(), fm.getHeight());
		assertTrue(fm.stringWidth("topic") > fm.stringWidth("i"));
		assertEquals(0, fm.stringWidth(""));
		assertTrue(new SvgTreeCanvas.ApproximateFontMetrics(font.deriveFont(java.awt.Font.BOLD)).stringWidth("topic") >= fm
				.stringWidth("topic"));
	}

	public void testSvgEscaping() throws Exception {
		List<MapNode<String>> nodes = new ArrayList<MapNode<String>>();
		nodes.add(leaf(0, "a<b&\"c\"", 0, 1));
		TopicHierarchy<String> h = new TopicHierarchy<String>(nodes);
		StringWriter sw = new StringWriter();
		new DefaultTopicTreePainter<String>(h).writeSvg(sw);
		assertTrue(sw.toString().contains(">a&lt;b&amp;&quot;c&quot; [1]</text>"));
	}
}
