package client.view;

import javax.swing.*;
import java.awt.*;

/**
 * 共通の描画ヘルパを持つパネルの抽象基底クラスです。
 */
@SuppressWarnings("unused")
abstract class BaseDecoratedPanel extends JPanel {
	protected BaseDecoratedPanel() {
		setOpaque(false);
	}

	protected static Color withAlpha(Color base, int alpha) {
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
	}

	@Override
	protected final void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g.create();
		try {
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			paintPanel(g2d);
		} finally {
			g2d.dispose();
		}
	}

	/**
	 * 派生クラスの描画処理を定義します。
	 */
	protected abstract void paintPanel(Graphics2D g2d);

	protected final void paintShadowedRoundedRect(Graphics2D g2d, int x, int y, int width, int height, Color topColor, Color bottomColor, Color borderColor) {
		if (width <= 0 || height <= 0) return;
		int arc = 24; // 角の角度
		int shadowSize = 8; // 影のサイズ
		Color shadowColor = withAlpha(Color.BLACK, 40); // 影の色

		// 影の位置とサイズ
		int shadowX = x + shadowSize, shadowY = y + shadowSize;
		int shadowW = width - shadowSize * 2, shadowH = height - shadowSize * 2;
		if (shadowW <= 0 || shadowH <= 0) return;

		g2d.setColor(shadowColor);
		g2d.fillRoundRect(shadowX, shadowY + 3, shadowW, shadowH, arc, arc);

		Paint originPaint = g2d.getPaint();
		g2d.setPaint(new GradientPaint(0, shadowY, topColor, 0, shadowY + shadowH, bottomColor));
		g2d.fillRoundRect(shadowX, shadowY, shadowW, shadowH, arc, arc);
		g2d.setPaint(originPaint);

		if (borderColor != null) {
			g2d.setColor(borderColor);
			g2d.drawRoundRect(shadowX, shadowY, shadowW - 1, shadowH - 1, arc, arc);
		}
	}
}
