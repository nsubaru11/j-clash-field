package client.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * ゲーム終了後の結果画面を表示するパネルです。
 * 勝敗結果、スコア、およびゲーム部屋に戻るボタンを提供します。
 */
public class ResultPanel extends BaseBackgroundPanel {
	// --------------- クラス定数 ---------------
	private static final Font BUTTON_FONT = new Font("Meiryo", Font.BOLD, 18);

	// --------------- フィールド ---------------
	private final JButton backButton;

	/**
	 * ResultPanelを構築します。
	 */
	public ResultPanel() {
		setLayout(new GridBagLayout());

		backButton = new JButton("ゲームルームへ戻る");
		backButton.setFont(BUTTON_FONT);
		backButton.setForeground(new Color(40, 40, 40));
		backButton.setBackground(new Color(210, 210, 210));
		backButton.setFocusPainted(false);
		backButton.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(140, 140, 140), 1),
				new EmptyBorder(10, 24, 10, 24)
		));

		add(backButton);
	}

	public void setBackAction(ActionListener listener) {
		backButton.addActionListener(listener);
	}
}
