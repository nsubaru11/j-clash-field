package client;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;

public final class UiScaleConfig {
	private static final int BASE_WIDTH = 1920;
	private static final int BASE_HEIGHT = 1080;
	private static final double BASE_DPI = 96.0;
	private static volatile double scaleFactor = 1.0;

	private UiScaleConfig() {
	}

	public static void apply() {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
		double scaleBySize = Math.min(screen.getWidth() / BASE_WIDTH, screen.getHeight() / BASE_HEIGHT);
		double scaleByDpi = dpi / BASE_DPI;
		scaleFactor = Math.max(1.0, Math.max(scaleBySize, scaleByDpi));
		applyFontScale(scaleFactor);
	}

	public static int scale(int value) {
		return (int) Math.round(value * scaleFactor);
	}

	public static Dimension scale(int width, int height) {
		return new Dimension(scale(width), scale(height));
	}

	public static Insets scaleInsets(int top, int left, int bottom, int right) {
		return new Insets(scale(top), scale(left), scale(bottom), scale(right));
	}

	public static Font scaleFont(Font font) {
		return font.deriveFont((float) scale(font.getSize()));
	}

	public static Font scaleFont(String name, int style, int size) {
		return new Font(name, style, scale(size));
	}

	private static void applyFontScale(double scale) {
		UIDefaults defaults = UIManager.getDefaults();
		for (Enumeration<?> e = defaults.keys(); e.hasMoreElements(); ) {
			Object key = e.nextElement();
			Object value = defaults.get(key);
			if (value instanceof Font) {
				Font font = (Font) value;
				float newSize = (float) Math.max(12.0, Math.round(font.getSize2D() * scale));
				defaults.put(key, font.deriveFont(newSize));
			}
		}
	}
}
