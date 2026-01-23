package client.view;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
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
		leftPanel.setBorder(BorderFactory.createEmptyBorder(400, 100, 0, 0));

		Dimension largeButtonSize = new Dimension(400, 50);
		Dimension smallButtonSize = new Dimension(195, 50);

		// ランダム参加ボタン
		matchingButton = createSimpleButton("ランダム参加", largeButtonSize, new Color(20, 92, 62));
		matchingButton.addActionListener(e -> onMatchAction.accept(MatchingPanel.MatchingMode.RANDOM));
		matchingButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		leftPanel.add(matchingButton);
		leftPanel.add(Box.createVerticalStrut(20));

		// ルーム作成・参加ボタン
		JPanel subPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		subPanel.setOpaque(false);
		subPanel.setMaximumSize(new Dimension(400, 50));
		subPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		createRoomButton = createSimpleButton("ルーム作成", smallButtonSize, new Color(27, 110, 80));
		createRoomButton.addActionListener(e -> onMatchAction.accept(MatchingPanel.MatchingMode.CREATE));
		subPanel.add(createRoomButton);
		subPanel.add(Box.createHorizontalStrut(10));

		joinRoomButton = createSimpleButton("ルーム参加", smallButtonSize, new Color(24, 102, 74));
		joinRoomButton.addActionListener(e -> onMatchAction.accept(MatchingPanel.MatchingMode.JOIN));
		subPanel.add(joinRoomButton);

		leftPanel.add(subPanel);
		leftPanel.add(Box.createVerticalStrut(20));

		// 設定ボタン
		configButton = createSimpleButton("設定", largeButtonSize, new Color(90, 96, 104)); // 色をグレーに
		configButton.setEnabled(false); // 押せないようにする
		// configButton.setDisabledTextColor(new Color(210, 210, 210));
		configButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		leftPanel.add(configButton);
		leftPanel.add(Box.createVerticalStrut(20));

		// 終了ボタン
		exitButton = createSimpleButton("終了", largeButtonSize, new Color(112, 45, 45));
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
		button.setFont(new Font("Meiryo", Font.BOLD, 20));
		button.setForeground(new Color(245, 245, 245));
		button.setBackground(color);
		button.setFocusPainted(false);
		button.setBorder(new CompoundBorder(
				new LineBorder(color.darker(), 1, true),
				new EmptyBorder(0, 14, 0, 14)));
		button.setContentAreaFilled(true);
		button.setOpaque(true);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setVerticalAlignment(SwingConstants.CENTER);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setVerticalTextPosition(SwingConstants.CENTER);
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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
