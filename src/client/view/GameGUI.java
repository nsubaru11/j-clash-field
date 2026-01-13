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
	private static final String ICON_IMAGE_PATH = "../assets/icon.png";

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
	 * 画面サイズの設定、レイアウトの構築、各パネルの生成と配置を行います。
	 */
	public GameGUI(JPanel cardPanel) {
		// フレームの基本設定
		setTitle("Game");
		if (ICON_IMAGE != null) setIconImage(ICON_IMAGE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// 画面サイズの設定（デフォルト）
		// 短い辺の60%を幅、80%を高さにする
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		double size = Math.min(screenSize.getWidth(), screenSize.getHeight());
		screenSize.setSize(size * 0.6, size * 0.8);
		setSize(screenSize);
		setLocationRelativeTo(null);

		// レイアウトとパネルの初期化
		this.cardPanel = cardPanel;
		add(cardPanel);
	}

}
