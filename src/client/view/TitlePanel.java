package client.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * ゲーム起動直後に表示されるタイトル画面パネルです。
 * 画面クリックで接続処理を開始します。
 */
public class TitlePanel extends BaseBackgroundPanel {

	private static final String TITLE_TEXT = "J-Clash Field";
	private static final String PRESS_START_TEXT = "- Click to Start -";
	private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 64);
	private static final Font SUB_FONT = new Font("Arial", Font.BOLD, 32);

	private final Timer blinkTimer;
	private boolean showText = true;

	public TitlePanel(Runnable onStartAction) {
		// 背景画像は共通のものを使用（必要なら別のパスを指定）
		super();

		setLayout(new GridBagLayout());

		// 点滅エフェクト用タイマー
		blinkTimer = new Timer(500, e -> {
			showText = !showText;
			repaint();
		});
		blinkTimer.start();

		// 画面全体のどこをクリックしてもスタートするようにする
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				blinkTimer.stop();
				onStartAction.run();
			}
		});
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		int width = getWidth();
		int height = getHeight();
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// --- タイトルロゴ ---
		g2d.setFont(TITLE_FONT);
		FontMetrics fmTitle = g2d.getFontMetrics();
		int titleW = fmTitle.stringWidth(TITLE_TEXT);
		int titleX = (width - titleW) / 2;
		int titleY = height / 3;

		// 影
		g2d.setColor(new Color(0, 0, 0, 150));
		g2d.drawString(TITLE_TEXT, titleX + 5, titleY + 5);
		// 本体
		g2d.setColor(Color.WHITE);
		g2d.drawString(TITLE_TEXT, titleX, titleY);

		// --- Click to Start ---
		if (showText) {
			g2d.setFont(SUB_FONT);
			FontMetrics fmSub = g2d.getFontMetrics();
			int subW = fmSub.stringWidth(PRESS_START_TEXT);
			int subX = (width - subW) / 2;
			int subY = height * 3 / 4;

			g2d.setColor(Color.WHITE);
			g2d.drawString(PRESS_START_TEXT, subX, subY);
		}
	}
}
