package client.view;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * マッチング設定画面を表示するパネルです。
 * ユーザー名の入力、ルーム番号の入力、開始/キャンセルボタンを提供します。
 */
public class MatchingPanel extends JPanel {
	// --------------- フィールド ---------------
	private final JTextField userNameField;  // ユーザー名入力フィールド
	private final JTextField roomNumberField; // ルーム番号入力フィールド
	private final JButton startButton; // 開始ボタン
	private final JButton cancelButton; // キャンセルボタン
	private final JLabel titleLabel;
	private final JLabel roomNumberLabel; // 部屋番号入力ラベル

	private String userName = ""; // ユーザーネームのキャッシュ
	private MatchingMode currentMode = MatchingMode.RANDOM; // マッチングモードのキャッシュ

	/**
	 * MatchingPanelを構築します。
	 */
	public MatchingPanel() {
		// 半透明オーバーレイ背景
		setOpaque(false);
		setLayout(new GridBagLayout());

		// ダイアログパネルの作成
		JPanel dialogPanel = createDialogPanel();

		// ダイアログ内のレイアウト設定
		dialogPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 20, 10, 20);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// タイトルラベル
		titleLabel = new JLabel("マッチング設定", SwingConstants.CENTER);
		titleLabel.setFont(new Font("Meiryo", Font.BOLD, 26));
		titleLabel.setForeground(Color.WHITE);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(20, 20, 20, 20);
		dialogPanel.add(titleLabel, gbc);

		// ユーザー名ラベル
		JLabel userNameLabel = new JLabel("ユーザー名");
		userNameLabel.setFont(new Font("Meiryo", Font.PLAIN, 16));
		userNameLabel.setForeground(Color.WHITE);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(10, 20, 5, 10);
		gbc.anchor = GridBagConstraints.WEST;
		dialogPanel.add(userNameLabel, gbc);

		// ユーザー名入力フィールド
		userNameField = new JTextField(15);
		userNameField.setFont(new Font("Meiryo", Font.PLAIN, 14));
		userNameField.setPreferredSize(new Dimension(200, 30));
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.insets = new Insets(10, 10, 5, 20);
		dialogPanel.add(userNameField, gbc);

		// 部屋番号ラベル
		roomNumberLabel = new JLabel("ルーム番号");
		roomNumberLabel.setFont(new Font("Meiryo", Font.PLAIN, 16));
		roomNumberLabel.setForeground(Color.WHITE);
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(10, 20, 5, 10);
		gbc.anchor = GridBagConstraints.WEST;
		dialogPanel.add(roomNumberLabel, gbc);

		// 部屋番号入力フィールド
		roomNumberField = new JTextField(15);
		roomNumberField.setFont(new Font("Meiryo", Font.PLAIN, 14));
		roomNumberField.setPreferredSize(new Dimension(200, 30));
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.insets = new Insets(10, 10, 5, 20);
		dialogPanel.add(roomNumberField, gbc);

		// ボタンパネル
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
		buttonPanel.setOpaque(false);

		// 開始ボタン
		startButton = createStyledButton("開始", new Color(34, 139, 34));
		buttonPanel.add(startButton);

		// キャンセルボタン
		cancelButton = createStyledButton("戻る", new Color(139, 69, 69));
		buttonPanel.add(cancelButton);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(20, 20, 20, 20);
		gbc.anchor = GridBagConstraints.CENTER;
		dialogPanel.add(buttonPanel, gbc);

		// ダイアログをパネル中央に配置
		add(dialogPanel);
		setupForMode(MatchingMode.RANDOM);
	}

	public void setupForMode(MatchingMode mode) {
		currentMode = mode;
		switch (mode) {
			case RANDOM:
				titleLabel.setText("ランダム参加");
				roomNumberLabel.setVisible(false);
				roomNumberField.setVisible(false);
				startButton.setText("参加");
				break;
			case CREATE:
				titleLabel.setText("ルーム作成");
				roomNumberLabel.setVisible(false);
				roomNumberField.setVisible(false);
				startButton.setText("作成");
				break;
			case JOIN:
				titleLabel.setText("ルーム参加");
				roomNumberLabel.setVisible(true);
				roomNumberField.setVisible(true);
				startButton.setText("参加");
				break;
		}
		revalidate();
		repaint();
	}

	public void setStartGameListener(ActionListener startGameListener) {
		startButton.addActionListener(e -> {
			if (onStartClicked()) {
				startGameListener.actionPerformed(e);
				setupForMode(currentMode);
			}
		});
	}

	public void setCancelListener(ActionListener cancelListener) {
		cancelButton.addActionListener(cancelListener);
	}

	/**
	 * パネル表示時に入力フィールドをリセットします。
	 */
	public void reset() {
		SwingUtilities.invokeLater(() -> {
			userNameField.setText(userName);
			roomNumberField.setText("");
			userNameField.requestFocusInWindow();
		});
	}

	/**
	 * 半透明のオーバーレイ背景を描画します。
	 */
	@Override
	protected void paintComponent(final Graphics g) {
		super.paintComponent(g);
		g.setColor(new Color(0, 0, 0, 150));
		g.fillRect(0, 0, getWidth(), getHeight());
	}

	/**
	 * ダイアログパネルを作成します。
	 *
	 * @return ダイアログパネル
	 */
	private JPanel createDialogPanel() {
		JPanel panel = new JPanel() {
			@Override
			protected void paintComponent(final Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				// 角丸の背景
				g2d.setColor(new Color(45, 45, 45));
				g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

				// 枠線
				g2d.setColor(new Color(100, 100, 100));
				g2d.setStroke(new BasicStroke(2));
				g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
			}
		};
		panel.setOpaque(false);
		panel.setPreferredSize(new Dimension(460, 320));
		return panel;
	}

	/**
	 * スタイル付きボタンを作成します。
	 *
	 * @param text  ボタンテキスト
	 * @param color ボタンの背景色
	 * @return スタイル付きボタン
	 */
	private JButton createStyledButton(final String text, final Color color) {
		JButton button = new JButton(text);
		button.setFont(new Font("Meiryo", Font.BOLD, 15));
		button.setForeground(Color.WHITE);
		button.setBackground(color);
		button.setPreferredSize(new Dimension(160, 40));
		button.setFocusPainted(false);
		button.setBorder(new CompoundBorder(
				new LineBorder(color.darker(), 1),
				new EmptyBorder(5, 15, 5, 15)
		));
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));

		// ホバーエフェクト
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

	public MatchingMode getCurrentMode() {
		return currentMode;
	}

	public String getUserName() {
		return userNameField.getText().trim();
	}

	public int getRoomId() {
		if (currentMode != MatchingMode.JOIN) return -1;
		try {
			return Integer.parseInt(roomNumberField.getText().trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * 開始ボタンクリック時の処理です。
	 */
	private boolean onStartClicked() {
		userName = getUserName();
		if (userName.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					"ユーザー名を入力してください。",
					"入力エラー",
					JOptionPane.WARNING_MESSAGE);
			return false;
		}
		if (currentMode == MatchingMode.JOIN) {
			int roomNumber = getRoomId();
			if (roomNumber < 0) {
				JOptionPane.showMessageDialog(this,
						"ルーム番号を正の整数で入力してください。",
						"入力エラー",
						JOptionPane.WARNING_MESSAGE);
				return false;
			}
		}
		return true;
	}

	public enum MatchingMode {
		RANDOM, // ランダムマッチ
		CREATE, // ルーム作成
		JOIN    // ルーム参加
	}
}
