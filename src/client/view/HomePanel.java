package client.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * ゲームのホーム画面を表示するパネルです。
 * 背景画像、タイトル、およびゲーム開始・終了ボタンを提供します。
 */
public class HomePanel extends BaseBackgroundPanel {
	// --------------- クラス定数 ---------------
	/** タイトルテキスト */
	private static final String TITLE_TEXT = "Action Game";
	/** タイトルフォント */
	private static final Font TITLE_FONT = new Font("Meiryo", Font.BOLD, 64);

	// --------------- フィールド ---------------
	/** スタートボタン */
	private final JButton startButton;
	/** 終了ボタン */
	private final JButton finishButton;

	/** HomePanelを構築します。 */
	public HomePanel(ActionListener startGameListener, ActionListener exitListener) {
		// 画面構成の設定
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		// finish ボタンの配置
		finishButton = createSimpleButton("終了", new Color(139, 69, 69));
		finishButton.addActionListener(exitListener);
		gbc.insets = new Insets(200, 200, 10, 10);
		gbc.gridx = 0;
		gbc.gridy = 0;
		add(finishButton, gbc);

		// start ボタンの配置
		startButton = createSimpleButton("スタート", new Color(34, 139, 34));
		startButton.addActionListener(startGameListener);
		gbc.insets = new Insets(200, 10, 10, 200);
		gbc.gridx = 1;
		gbc.gridy = 0;
		add(startButton, gbc);
	}

	@Override
	protected void paintComponent(final Graphics g) {
		super.paintComponent(g);

		// 影付き文字を描画
		int panelWidth = getWidth();
		int panelHeight = getHeight();

		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setFont(TITLE_FONT);
		FontMetrics fm = g2d.getFontMetrics();
		int textWidth = fm.stringWidth(TITLE_TEXT);
		int textX = (panelWidth - textWidth) / 2;
		int textY = panelHeight / 3 - fm.getHeight() / 3 + fm.getAscent() - 50;

		// 影の描画
		g2d.setColor(new Color(0, 0, 0, 120));
		g2d.drawString(TITLE_TEXT, textX + 3, textY + 3);

		// 文字の描画
		g2d.setColor(Color.WHITE);
		g2d.drawString(TITLE_TEXT, textX, textY);
	}

	/**
	 * ボタンの初期化を行います。
	 */
	private JButton createSimpleButton(final String text, final Color color) {
		JButton button = new JButton(text);
		Dimension size = new Dimension(200, 60);

		// ボタンの基本設定（枠線を消し、透明化）
		button.setPreferredSize(size);
		button.setMinimumSize(size);
		button.setMaximumSize(size);
		button.setFont(new Font("Meiryo", Font.BOLD, 24));
		button.setForeground(Color.WHITE);
		button.setBackground(color);
		button.setFocusPainted(false);
		button.setBorder(null);

		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				button.setBackground(color.brighter());
			}

			@Override
			public void mouseExited(MouseEvent e) {
				button.setBackground(color);
			}
		});
		return button;
	}
}