package client.view;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

/**
 * [ 右向き立ち ] [ 右向き走り1 ] [ 右向き走り2 ]
 * [ 右向きジャンプ ] [ 右向きシールド1 ] [ 右向きシールド2 ]
 * [ 攻撃チャージ ] [ 右向き攻撃 ] [ 右向き攻撃後 ]
 */
public class ArcherView {
	// 画像パス (アップロードされたファイル名に合わせています)
	private static final String IMAGE_PATH = "/archer_sprite_sheet.jpg";
	// 1コマのサイズ（画像の仕様に合わせて調整してください）
	private static final int TILE_WIDTH = 32;
	private static final int TILE_HEIGHT = 32;
	private static BufferedImage spriteSheet;

	static {
		try {
			spriteSheet = ImageIO.read(Objects.requireNonNull(ArcherView.class.getResource(IMAGE_PATH)));
		} catch (IOException e) {
			throw new RuntimeException("アーチャー画像の読み込みに失敗しました", e);
		}
	}

	public void draw() {

	}
}
