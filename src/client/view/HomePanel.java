package client.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * ゲームのホーム画面を表示するパネルです。
 * 背景画像、タイトル、およびゲームメニューボタンを提供します。
 */
public class HomePanel extends BaseBackgroundPanel {
	// --------------- クラス定数 ---------------
	/** タイトルテキスト */
	private static final String TITLE_TEXT = "Action Game";
	/** タイトルフォント */
	private static final Font TITLE_FONT = new Font("Meiryo", Font.BOLD, 64);

	// --------------- フィールド ---------------
	/** ランダム参加ボタン */
	private final JButton matchingButton;
	/** ルーム作成ボタン */
	private final JButton createRoomButton;
	/** ルーム参加ボタン */
	private final JButton joinRoomButton;
	/** 設定ボタン */
	private final JButton configButton;
	/** 終了ボタン */
	private final JButton exitButton;

	/**
	 * HomePanelを構築します。
	 */
	public HomePanel(Consumer<MatchingPanel.MatchingMode> onMatchAction, ActionListener exitListener) {
		setLayout(new BorderLayout());

		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.setOpaque(false);
		leftPanel.setBorder(BorderFactory.createEmptyBorder(350, 100, 0, 0));

		Dimension largeButtonSize = new Dimension(200, 50);
		Dimension smallButtonSize = new Dimension(95, 50);

		// ランダム参加ボタン
		matchingButton = createSimpleButton("ランダム参加", largeButtonSize, new Color(34, 139, 34));
		matchingButton.addActionListener(e -> onMatchAction.accept(MatchingPanel.MatchingMode.RANDOM));
		matchingButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		leftPanel.add(matchingButton);
		leftPanel.add(Box.createVerticalStrut(20));

		// ルーム作成・参加ボタン
		JPanel subPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		subPanel.setOpaque(false);
		subPanel.setMaximumSize(new Dimension(210, 50));
		subPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		createRoomButton = createSimpleButton("ルーム作成", smallButtonSize, new Color(34, 139, 34));
		createRoomButton.addActionListener(e -> onMatchAction.accept(MatchingPanel.MatchingMode.CREATE));
		subPanel.add(createRoomButton);

		joinRoomButton = createSimpleButton("ルーム参加", smallButtonSize, new Color(34, 139, 34));
		joinRoomButton.addActionListener(e -> onMatchAction.accept(MatchingPanel.MatchingMode.JOIN));
		subPanel.add(joinRoomButton);

		leftPanel.add(subPanel);
		leftPanel.add(Box.createVerticalStrut(20));

		// 設定ボタン
		configButton = createSimpleButton("設定", largeButtonSize, Color.GRAY); // 色をグレーに
		configButton.setEnabled(false); // 押せないようにする
		configButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		leftPanel.add(configButton);
		leftPanel.add(Box.createVerticalStrut(20));

		// 終了ボタン
		exitButton = createSimpleButton("終了", largeButtonSize, new Color(139, 69, 69));
		exitButton.addActionListener(exitListener);
		exitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		leftPanel.add(exitButton);

		add(leftPanel, BorderLayout.WEST);
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

		// タイトルは画面中央上部に配置
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
	private JButton createSimpleButton(final String text, final Dimension size, final Color color) {
		JButton button = new JButton(text);

		button.setPreferredSize(size);
		button.setMinimumSize(size);
		button.setMaximumSize(size);
		button.setFont(new Font("Dialog", Font.BOLD, 20));
		button.setForeground(Color.WHITE);
		button.setBackground(color);
		button.setFocusPainted(false);
		button.setBorder(null);

		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				if (button.isEnabled()) button.setBackground(color.brighter());
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (button.isEnabled()) button.setBackground(color);
			}
		});
		return button;
	}
}
