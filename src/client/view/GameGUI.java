package client.view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;

/**
 * ゲームのメインウィンドウを管理するクラスです。
 * CardLayoutを使用して画面の切り替えを行います。
 */
public class GameGUI extends JFrame {
	// --------------- クラス定数 ---------------
	/** アプリケーションアイコンの画像 */
	private static final Image ICON_IMAGE;
	/** アプリケーションアイコンの画像パス */
	private static final String ICON_IMAGE_PATH = "/client/assets/icon.png";

	static {
		// アイコンは読み込み失敗してもアプリ動作には影響しないため、ログ出力のみで続行
		Image image = null;
		try {
			image = ImageIO.read(Objects.requireNonNull(GameGUI.class.getResourceAsStream(ICON_IMAGE_PATH)));
		} catch (final IOException | NullPointerException e) {
			System.err.println("アイコン画像の読み込みに失敗しました。\n" + e);
		}
		ICON_IMAGE = image;
	}

	// --------------- フィールド ---------------
	/** 各画面パネルを保持する親パネル */
	private final JPanel cardPanel;

	/**
	 * GameGUIを構築し、メインウィンドウを初期化します。
	 */
	public GameGUI(JPanel cardPanel) {
		// フレームの基本設定
		setTitle("Game");
		if (ICON_IMAGE != null) setIconImage(ICON_IMAGE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setUndecorated(true); // 枠を消す
		setResizable(false);  // リサイズ禁止
		setBackground(Color.BLACK);

		// グラフィックスデバイスを取得してフルスクリーン化
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();

		if (gd.isFullScreenSupported()) {
			gd.setFullScreenWindow(this);
		} else {
			// 万が一フルスクリーン非対応の場合は最大化して表示
			setExtendedState(JFrame.MAXIMIZED_BOTH);
			setVisible(true);
		}

		// レイアウトとパネルの初期化
		this.cardPanel = cardPanel;
		add(cardPanel);

		// フルスクリーンの場合、フォーカスを確実に持たせる
		requestFocus();
	}
}
