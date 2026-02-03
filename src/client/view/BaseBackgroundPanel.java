package client.view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

@SuppressWarnings("unused")
abstract class BaseBackgroundPanel extends JPanel {
	/**
	 * 背景画像のパス（デフォルト）
	 */
	protected static final String DEFAULT_BACKGROUND_IMAGE_PATH = "/resources/background.png";
	/**
	 * 背景画像（デフォルト）
	 */
	protected static final BufferedImage DEFAULT_BACKGROUND_IMAGE;

	static {
		try {
			DEFAULT_BACKGROUND_IMAGE = ImageIO.read(Objects.requireNonNull(BaseBackgroundPanel.class.getResource(DEFAULT_BACKGROUND_IMAGE_PATH)));
		} catch (final IOException | NullPointerException e) {
			throw new RuntimeException("背景画像の読み込みに失敗しました", e);
		}
	}

	/**
	 * このパネルで使用する背景画像
	 */
	protected final BufferedImage backgroundImage;

	/**
	 * デフォルトの背景画像を使用するコンストラクタ
	 */
	protected BaseBackgroundPanel() {
		this.backgroundImage = DEFAULT_BACKGROUND_IMAGE;
	}

	/**
	 * コンストラクタで画像パスを受け取る
	 *
	 * @param imagePath 背景画像のパス。null の場合はデフォルト画像を使用する。
	 */
	protected BaseBackgroundPanel(final String imagePath) {
		if (imagePath == null || imagePath.equals(DEFAULT_BACKGROUND_IMAGE_PATH)) {
			this.backgroundImage = DEFAULT_BACKGROUND_IMAGE;
		} else {
			try {
				backgroundImage = ImageIO.read(Objects.requireNonNull(BaseBackgroundPanel.class.getResource(imagePath)));
			} catch (final IOException | NullPointerException e) {
				throw new RuntimeException("背景画像の読み込みに失敗しました", e);
			}
		}
	}

	@Override
	protected void paintComponent(final Graphics g) {
		super.paintComponent(g);

		int panelWidth = getWidth();
		int panelHeight = getHeight();
		int imageWidth = backgroundImage.getWidth();
		int imageHeight = backgroundImage.getHeight();
		double imageAspect = (double) imageWidth / imageHeight;
		double panelAspect = (double) panelWidth / panelHeight;

		// 背景画像を描画
		int drawWidth, drawHeight;
		int imgX, imgY;
		if (panelAspect > imageAspect) {
			// パネルの方が横長 → 横幅を合わせて縦をトリミング
			drawWidth = panelWidth;
			drawHeight = (int) (panelWidth / imageAspect);
			imgX = 0;
			imgY = (panelHeight - drawHeight) / 2;
		} else {
			// パネルの方が縦長 → 縦幅を合わせて横をトリミング
			drawHeight = panelHeight;
			drawWidth = (int) (panelHeight * imageAspect);
			imgY = 0;
			imgX = (panelWidth - drawWidth) / 2;
		}
		g.drawImage(backgroundImage, imgX, imgY, drawWidth, drawHeight, this);
	}
}
