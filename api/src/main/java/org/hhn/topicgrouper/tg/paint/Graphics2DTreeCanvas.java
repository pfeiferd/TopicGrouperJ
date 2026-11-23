package org.hhn.topicgrouper.tg.paint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * A {@link TreeCanvas} drawing onto a {@link Graphics2D}, e.g. of a Swing
 * component or a {@link java.awt.image.BufferedImage}.
 */
public class Graphics2DTreeCanvas implements TreeCanvas {
	private final Graphics2D graphics;

	/**
	 * Creates the canvas; antialiasing is switched on.
	 *
	 * @param graphics
	 *            the graphics to draw onto
	 */
	public Graphics2DTreeCanvas(Graphics2D graphics) {
		this.graphics = graphics;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
	}

	@Override
	public FontMetrics getFontMetrics(Font font) {
		return graphics.getFontMetrics(font);
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2, Color color, float width, boolean dashed) {
		Stroke old = graphics.getStroke();
		Color oldColor = graphics.getColor();
		graphics.setColor(color);
		graphics.setStroke(dashed ? new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
				new float[] { 4 * width, 4 * width }, 0f) : new BasicStroke(width));
		graphics.drawLine(x1, y1, x2, y2);
		graphics.setStroke(old);
		graphics.setColor(oldColor);
	}

	@Override
	public void fillRect(int x, int y, int width, int height, Color color) {
		Color oldColor = graphics.getColor();
		graphics.setColor(color);
		graphics.fillRect(x, y, width, height);
		graphics.setColor(oldColor);
	}

	@Override
	public void drawText(int x, int y, String text, Font font, Color color) {
		Font oldFont = graphics.getFont();
		Color oldColor = graphics.getColor();
		graphics.setFont(font);
		graphics.setColor(color);
		graphics.drawString(text, x, y);
		graphics.setFont(oldFont);
		graphics.setColor(oldColor);
	}
}
