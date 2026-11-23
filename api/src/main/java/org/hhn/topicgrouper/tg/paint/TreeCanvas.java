package org.hhn.topicgrouper.tg.paint;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;

/**
 * The drawing primitives a {@link TopicTreePainter} needs, so that the same
 * layout can go to a {@link java.awt.Graphics2D} ({@link Graphics2DTreeCanvas})
 * or to an SVG file ({@link SvgTreeCanvas}). Coordinates are pixels, the origin
 * is the upper left corner.
 */
public interface TreeCanvas {
	/**
	 * The metrics used to lay out text in a font.
	 *
	 * @param font
	 *            the font
	 * @return its metrics
	 */
	public FontMetrics getFontMetrics(Font font);

	/**
	 * Draws a straight line.
	 *
	 * @param x1
	 *            start x
	 * @param y1
	 *            start y
	 * @param x2
	 *            end x
	 * @param y2
	 *            end y
	 * @param color
	 *            the color
	 * @param width
	 *            the stroke width
	 * @param dashed
	 *            whether the line is dashed
	 */
	public void drawLine(int x1, int y1, int x2, int y2, Color color, float width, boolean dashed);

	/**
	 * Fills a rectangle.
	 *
	 * @param x
	 *            left
	 * @param y
	 *            top
	 * @param width
	 *            the width
	 * @param height
	 *            the height
	 * @param color
	 *            the color
	 */
	public void fillRect(int x, int y, int width, int height, Color color);

	/**
	 * Draws a text.
	 *
	 * @param x
	 *            left end of the baseline
	 * @param y
	 *            the baseline
	 * @param text
	 *            the text
	 * @param font
	 *            the font
	 * @param color
	 *            the color
	 */
	public void drawText(int x, int y, String text, Font font, Color color);
}
