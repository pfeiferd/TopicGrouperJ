package org.hhn.topicgrouper.eval.plot;

import java.io.StringWriter;

import junit.framework.TestCase;

/**
 * Tests the SVG line chart: structure, scales, error bars and tick labels.
 */
public class SvgLineChartTest extends TestCase {
	private int count(String s, String part) {
		int c = 0;
		for (int i = s.indexOf(part); i >= 0; i = s.indexOf(part, i + 1)) {
			c++;
		}
		return c;
	}

	public void testLinearChart() throws Exception {
		SvgLineChart chart = new SvgLineChart("number of topics", "perplexity");
		chart.add("Topic Grouper", new double[] { 10, 20, 50 }, new double[] { 500, 450, 420 });
		chart.add(new SvgLineChart.Series("LDA", new double[] { 10, 20, 50 }, new double[] { 480, 400, 390 },
				new double[] { 5, 5, 5 }));
		StringWriter sw = new StringWriter();
		chart.write(sw);
		String svg = sw.toString();
		assertTrue(svg.startsWith("<?xml"));
		assertTrue(svg.contains("<svg xmlns=\"http://www.w3.org/2000/svg\""));
		assertTrue(svg.contains(">number of topics</text>"));
		assertTrue(svg.contains(">perplexity</text>"));
		assertTrue(svg.contains(">Topic Grouper</text>"));
		assertTrue(svg.contains(">LDA</text>"));
		assertEquals(2, count(svg, "<path d=\"M"));
		// Three error bars of three lines each.
		assertTrue(count(svg, "<line ") >= 9 + 2);
		// Tick labels are plain numbers covering the data range.
		assertTrue(svg.contains(">400</text>") || svg.contains(">380</text>"));
		assertTrue(svg.contains(">50</text>"));
		assertFalse(svg.contains("E"));
	}

	public void testLogAxesAndGaps() throws Exception {
		SvgLineChart chart = new SvgLineChart("features", "accuracy").setLogX(true).setLogY(true);
		chart.add("a", new double[] { 1, 10, 100, 1000 }, new double[] { 0.1, Double.NaN, 0.5, 0.9 });
		StringWriter sw = new StringWriter();
		chart.write(sw);
		String svg = sw.toString();
		assertTrue(svg.contains(">1</text>"));
		assertTrue(svg.contains(">1000</text>"));
		// The NaN splits the line into two parts.
		assertEquals(2, count(svg, "M"));
		assertTrue(svg.contains(">0.1</text>"));
	}

	public void testFormat() {
		assertEquals("0", SvgLineChart.format(0));
		assertEquals("12", SvgLineChart.format(12.0));
		assertEquals("0.25", SvgLineChart.format(0.25));
		assertEquals("2.5", SvgLineChart.format(2.5));
		assertEquals("1000000", SvgLineChart.format(1e6));
	}

	public void testRejectsUnequalLengths() {
		try {
			new SvgLineChart.Series("x", new double[] { 1 }, new double[] { 1, 2 }, null);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
}
