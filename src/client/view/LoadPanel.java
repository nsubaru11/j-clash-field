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
 */
public class LoadPanel extends JPanel {
	// --------------- フィールド ---------------
	/** 背景画像のパス */
	private static final String BACKGROUND_IMAGE_PATH = "/client/assets/loading.png";
	/** 背景画像 */
	private static final BufferedImage BACKGROUND_IMAGE;
	private static final int ANIMATION_DELAY = 10; // 更新間隔 (ms)
	private static final int SLIDE_SPEED = 180;     // スライド速度 (px/frame)
	private static final long MIN_WAIT_TIME = 500;
	private static final Color BACKGROUND_COLOR = new Color(20, 20, 40); // 背景色
	private static final String LOADING_TEXT = "CONNECTING...";
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
	private Runnable onFinishCallback;
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
					state = State.SLIDE_OUT;
				}
				break;

			case SLIDE_OUT:
				xPosition += SLIDE_SPEED;
				if (xPosition >= width) {
					stopAnimation();
					if (onFinishCallback != null) {
						onFinishCallback.run();
					}
				}
				break;

			default:
				break;
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int width = getWidth();
		int height = getHeight();

		g2d.setColor(BACKGROUND_COLOR);
		g2d.fillRect(0, 0, width, height);

		if (state == State.HIDDEN) return;

		g2d.drawImage(BACKGROUND_IMAGE, xPosition, 0, width, height, null);

		// テキスト
		g2d.setColor(Color.WHITE);
		g2d.setFont(LOADING_FONT);
		FontMetrics fm = g2d.getFontMetrics();
		int textWidth = fm.stringWidth(LOADING_TEXT);
		int textX = xPosition + (width - textWidth) / 2;
		int textY = (height + fm.getAscent()) / 2;

		g2d.drawString(LOADING_TEXT, textX, textY);
	}

	/**
	 * ロード処理を開始します。
	 */
	public void startLoading() {
		isLoaded = false;
		state = State.SLIDE_IN;
		xPosition = -getWidth();
		animationTimer.start();
	}

	/**
	 * 通信完了通知（GuiControllerから呼ばれる）
	 */
	public void completeLoading() {
		isLoaded = true;
	}

	/**
	 * ロード終了後のコールバック設定
	 */
	public void setLoaded(Runnable onFinish) {
		this.onFinishCallback = onFinish;
	}

	private void stopAnimation() {
		animationTimer.stop();
		state = State.HIDDEN;
	}

	// アニメーションの状態定義
	private enum State {
		HIDDEN,
		SLIDE_IN,
		WAITING,
		SLIDE_OUT
	}
}
