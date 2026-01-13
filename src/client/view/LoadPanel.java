package client.view;

import javax.swing.*;
import java.awt.*;

/**
 * ゲーム起動時のロード画面を表示するパネルです。
 * ロード完了後にホーム画面へ遷移します。
 */
public class LoadPanel extends BaseBackgroundPanel {
	// --------------- フィールド ---------------
	/** プログレスバー */
	private final JProgressBar progressBar;
	/** アニメーション用タイマー */
	private final Timer timer;

	/** 現在の進捗値 */
	private int progress = 0;
	private boolean isLoaded = false;
	private Runnable runnable;

	/** LoadPanelを構築します。 */
	public LoadPanel() {
		setLayout(new GridBagLayout());

		// プログレスバーの設定
		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		progressBar.setPreferredSize(new Dimension(this.getWidth() / 2, this.getWidth() / 20));
		progressBar.setFont(new Font("Monospaced", Font.PLAIN, 14));
		progressBar.setBackground(Color.WHITE);
		progressBar.setForeground(new Color(0, 200, 0));

		// レイアウト設定
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(300, 0, 0, 0);
		add(progressBar, gbc);

		// ロードアニメーション用のタイマー
		timer = new Timer(30, e -> updateProgress());
	}

	/**
	 * ロード処理を開始します。
	 */
	public void startLoading() {
		progress = 0;
		progressBar.setValue(0);
		isLoaded = false;
		timer.start();
	}

	public void setLoaded(Runnable runnable) {
		this.runnable = runnable;
	}

	public void completeLoading() {
		isLoaded = true;
	}

	/**
	 * プログレスバーを更新します。
	 */
	private void updateProgress() {
		if (!isLoaded && progress >= 90) {
			// 接続待ち状態（点滅させたりテキストを変えたりしても良い）
			progressBar.setString("Connecting...");
			return;
		}

		progress++;
		progressBar.setValue(progress);

		if (progress < 100) {
			int dots = progress % 4;
			StringBuilder loadStr = new StringBuilder("Loading");
			for (int i = 0; i < dots; i++) loadStr.append('.');
			String loadString = String.format("%-10s%3d%%", loadStr, progress);
			progressBar.setString(loadString);
		} else {
			progressBar.setString("Done!");
		}

		if (progress >= 100) {
			timer.stop();
			Timer transitionTimer = new Timer(500, e -> {
				runnable.run();
				((Timer) e.getSource()).stop();
			});
			transitionTimer.setRepeats(false);
			transitionTimer.start();
		}
	}
}
