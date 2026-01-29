package client.view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

/**
 * ゲーム起動時のロード画面を表示するパネルです。
 * ロード完了後にホーム画面へ遷移します。
 * TODO: JOptionPane の UI を改善する
 */
public class LoadPanel extends BaseDecoratedPanel {
	// --------------- フィールド ---------------
	/** 背景画像のパス */
	private static final String BACKGROUND_IMAGE_PATH = "/resorces/loading.png";
	/** 背景画像 */
	private static final BufferedImage BACKGROUND_IMAGE;
	private static final int ANIMATION_DELAY = 10; // 更新間隔 (ms)
	private static final int SLIDE_SPEED = 180;     // スライド速度 (px/frame)
	private static final long MIN_WAIT_TIME = 500;
	private static final Color BACKGROUND_COLOR = new Color(20, 20, 40); // 背景色
	private static final String LOADING_TEXT = "CONNECTING";
	private static final Font LOADING_FONT = new Font("Arial", Font.BOLD, 48);

	static {
		try {
			BACKGROUND_IMAGE = ImageIO.read(Objects.requireNonNull(BaseBackgroundPanel.class.getResource(BACKGROUND_IMAGE_PATH)));
		} catch (final IOException | NullPointerException e) {
			throw new RuntimeException("背景画像の読み込みに失敗しました", e);
		}
	}

	private final Timer animationTimer;
	private boolean isLoaded = false;
	private State state = State.HIDDEN;
	private int xPosition; // スライドするカバーの x 座標
	private Runnable onSwitchScreen;
	private long startTime;

	/** LoadPanelを構築します。 */
	public LoadPanel() {
		super();
		setLayout(null);
		setBackground(BACKGROUND_COLOR);
		animationTimer = new Timer(ANIMATION_DELAY, e -> {
			updateAnimation();
			repaint();
		});
	}

	/**
	 * アニメーションの更新処理
	 */
	private void updateAnimation() {
		int width = getWidth();

		switch (state) {
			case SLIDE_IN:
				startTime = System.currentTimeMillis();
				xPosition += SLIDE_SPEED;
				if (xPosition >= 0) {
					xPosition = 0;
					state = State.WAITING;
				}
				break;

			case WAITING:
				long waitTime = System.currentTimeMillis() - startTime;
				if (isLoaded && waitTime >= MIN_WAIT_TIME) {
					if (onSwitchScreen != null) onSwitchScreen.run();
					state = State.SLIDE_OUT;
				}
				break;

			case SLIDE_OUT:
				xPosition += SLIDE_SPEED;
				if (xPosition >= width) {
					stopAnimation();
					setVisible(false);
				}
				break;

			default:
				break;
		}
	}

	@Override
	protected void paintPanel(Graphics2D g2d) {
		if (state == State.HIDDEN) return;

		int width = getWidth();
		int height = getHeight();

		g2d.drawImage(BACKGROUND_IMAGE, xPosition, 0, width, height, null);

		// テキスト
		g2d.setColor(Color.WHITE);
		g2d.setFont(LOADING_FONT);
		FontMetrics fm = g2d.getFontMetrics();
		StringBuilder dots = new StringBuilder(3);
		int dotCount = (int) ((System.currentTimeMillis() - startTime) / 1000 % 3);
		for (int i = 0; i <= dotCount; i++) dots.append(".");
		int textWidth = fm.stringWidth(LOADING_TEXT);
		int textX = xPosition + (width - textWidth - 3) / 2;
		int textY = (height + fm.getAscent()) / 2;

		g2d.drawString(LOADING_TEXT + dots, textX, textY);
	}

	/**
	 * ロード処理を開始します。
	 */
	public void startLoading() {
		SwingUtilities.invokeLater(() -> {
			isLoaded = false;
			state = State.SLIDE_IN;
			int width = Math.max(1, getWidth());
			xPosition = -width;
			setVisible(true);
			animationTimer.start();
		});
	}

	/**
	 * 通信完了通知（GuiControllerから呼ばれる）
	 */
	public void completeLoading() {
		SwingUtilities.invokeLater(() -> isLoaded = true);
	}

	/**
	 * ロード終了後のコールバック設定
	 */
	public void setNextScreen(Runnable onSwitchScreen) {
		SwingUtilities.invokeLater(() -> this.onSwitchScreen = onSwitchScreen);
	}

	private void stopAnimation() {
		SwingUtilities.invokeLater(() -> {
			animationTimer.stop();
			state = State.HIDDEN;
		});
	}

	// アニメーションの状態定義
	private enum State {
		HIDDEN,
		SLIDE_IN,
		WAITING,
		SLIDE_OUT
	}
}
