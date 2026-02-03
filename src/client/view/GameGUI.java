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
	private static final String ICON_IMAGE_PATH = "/resources/icon.png";

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
	private final JComponent rootPane;

	/**
	 * GameGUIを構築し、メインウィンドウを初期化します。
	 */
	public GameGUI(JComponent rootPane) {
		this.rootPane = rootPane;

		// フレームの基本設定
		setTitle("J-Clash Field");
		if (ICON_IMAGE != null) setIconImage(ICON_IMAGE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setResizable(false);  // リサイズ禁止
		setUndecorated(true);
		setExtendedState(JFrame.MAXIMIZED_BOTH);

		// レイアウトとパネルの初期化
		add(rootPane);

		requestFocus();
		
		setVisible(true);
	}
}
