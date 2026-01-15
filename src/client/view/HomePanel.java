package client.view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

/**
 * ゲームのホーム画面を表示するパネルです。
 * 背景画像、タイトル、およびゲーム開始・終了ボタンを提供します。
 */
public class HomePanel extends BaseBackgroundPanel {
	// --------------- クラス定数 ---------------
	/** スタートボタンの画像のパス */
	private static final String START_IMAGE_PATH = "/client/assets/start.png";
	/** 終了ボタンの画像のパス */
	private static final String FINISH_IMAGE_PATH = "/client/assets/finish.png";
	/** スタートボタンの画像 */
	private static final BufferedImage START_IMAGE;
	/** 終了ボタンの画像 */
	private static final BufferedImage FINISH_IMAGE;
	/** タイトルテキスト */
	private static final String TITLE_TEXT = "Game Game";
	/** タイトルフォント */
	private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 64);

	static {
		try {
			START_IMAGE = ImageIO.read(Objects.requireNonNull(HomePanel.class.getResourceAsStream(START_IMAGE_PATH)));
			FINISH_IMAGE = ImageIO.read(Objects.requireNonNull(HomePanel.class.getResourceAsStream(FINISH_IMAGE_PATH)));
		} catch (final IOException | NullPointerException e) {
			throw new RuntimeException("ボタン画像の読み込みに失敗しました", e);
		}
	}

	// --------------- フィールド ---------------
	/** スタートボタン */
	private final JButton startButton;
	/** 終了ボタン */
	private final JButton finishButton;

	/** スタートボタンの通常アイコン */
	private ImageIcon startIconNormal;
	/** スタートボタンの押下時アイコン */
	private ImageIcon startIconPressed;
	/** 終了ボタンの通常アイコン */
	private ImageIcon finishIconNormal;
	/** 終了ボタンの押下時アイコン */
	private ImageIcon finishIconPressed;

	/** HomePanelを構築します。 */
	public HomePanel(ActionListener startGameListener, ActionListener exitListener) {
		// 画面サイズの取得
		Dimension dimension = this.getSize();
		int width = dimension.width;
		int height = dimension.height;
		if (width == 0 || height == 0) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			width = screenSize.width;
			height = screenSize.height;
		}

		// 画面構成の設定
		setLayout(new GridBagLayout());
		setBackground(this.getBackground());
		GridBagConstraints gbc = new GridBagConstraints();

		// ボタンサイズの計算
		int buttonSize = Math.min(width / 6, height / 6);
		prepareImages(buttonSize);

		// finish ボタンの配置
		finishButton = new JButton();
		initButton(finishButton, finishIconNormal, finishIconPressed, buttonSize);
		finishButton.addActionListener(exitListener);
		gbc.insets = new Insets(200, 200, 10, 10);
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 2;
		add(finishButton, gbc);

		// start ボタンの配置
		startButton = new JButton();
		initButton(startButton, startIconNormal, startIconPressed, buttonSize);
		startButton.addActionListener(startGameListener);
		gbc.insets = new Insets(200, 10, 10, 200);
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 2;
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
	 * ボタン用の画像を事前生成してキャッシュします。
	 * パフォーマンス最適化のため、クリックごとの画像生成を回避します。
	 *
	 * @param buttonSize ボタンサイズ
	 */
	private void prepareImages(final int buttonSize) {
		int pressedSize = (int) (buttonSize * 0.95);

		// スタート画像の生成
		startIconNormal = new ImageIcon(START_IMAGE.getScaledInstance(buttonSize, buttonSize, Image.SCALE_SMOOTH));
		startIconPressed = new ImageIcon(createPressedImage(START_IMAGE, pressedSize));

		// 終了画像の生成
		finishIconNormal = new ImageIcon(FINISH_IMAGE.getScaledInstance(buttonSize, buttonSize, Image.SCALE_SMOOTH));
		finishIconPressed = new ImageIcon(createPressedImage(FINISH_IMAGE, pressedSize));
	}

	/**
	 * 押下時の画像を生成します。
	 * サイズ縮小と半透明化を適用します。
	 *
	 * @param source 元画像
	 * @param size   縮小後のサイズ
	 * @return 押下時の画像
	 */
	private Image createPressedImage(final BufferedImage source, final int size) {
		Image scaled = source.getScaledInstance(size, size, Image.SCALE_SMOOTH);
		BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = result.createGraphics();
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
		g2d.drawImage(scaled, 0, 0, null);
		g2d.dispose();
		return result;
	}

	/**
	 * ボタンの初期化を行います。
	 *
	 * @param button       初期化対象
	 * @param normalImage  通常時の画像
	 * @param pressedImage 押下時の画像
	 * @param buttonSize   ボタンサイズ
	 */
	private void initButton(final JButton button, final ImageIcon normalImage, final ImageIcon pressedImage, final int buttonSize) {
		Dimension size = new Dimension(buttonSize, buttonSize);

		// ボタンの基本設定（枠線を消し、透明化）
		button.setIcon(normalImage);
		button.setPreferredSize(size);
		button.setMinimumSize(size);
		button.setMaximumSize(size);
		button.setBorderPainted(false);
		button.setContentAreaFilled(false);
		button.setFocusPainted(false);

		// 押下時のアクション
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent e) {
				button.setIcon(pressedImage);
			}

			@Override
			public void mouseReleased(final MouseEvent e) {
				button.setIcon(normalImage);
			}
		});
	}
}