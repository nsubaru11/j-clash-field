package client.view;

import model.CharacterType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public abstract class CharacterView {
	private static final int DEFAULT_COLUMNS = 3;
	private static final int DEFAULT_ROWS = 3;
	private static final Map<CharacterType, CharacterView> VIEW_CACHE = new EnumMap<>(CharacterType.class);
	private final CharacterType type;
	private final BufferedImage spriteSheet;
	private final int sheetColumns;
	private final int sheetRows;
	protected CharacterView(CharacterType type, String spriteSheetPath) {
		this(type, spriteSheetPath, DEFAULT_COLUMNS, DEFAULT_ROWS);
	}

	protected CharacterView(CharacterType type, String spriteSheetPath, int sheetColumns, int sheetRows) {
		this.type = type;
		this.sheetColumns = sheetColumns;
		this.sheetRows = sheetRows;
		try {
			this.spriteSheet = ImageIO.read(Objects.requireNonNull(getClass().getResource(spriteSheetPath)));
		} catch (IOException | NullPointerException e) {
			throw new RuntimeException("Failed to load sprite sheet: " + spriteSheetPath, e);
		}
	}

	public static CharacterView forType(CharacterType type) {
		if (type == null || type == CharacterType.NONE) return null;
		return VIEW_CACHE.computeIfAbsent(type, CharacterView::createView);
	}

	private static CharacterView createView(CharacterType type) {
		switch (type) {
			case ARCHER:
				return new ArcherView();
			case WARRIOR:
				return new WarriorView();
			case FIGHTER:
				return new FighterView();
			case WIZARD:
				return new WizardView();
			default:
				return null;
		}
	}

	public CharacterType getType() {
		return type;
	}

	public void draw(Graphics2D g2d, int x, int y, int width, int height) {
		draw(g2d, x, y, width, height, Frame.IDLE, true);
	}

	public void draw(Graphics2D g2d, int x, int y, int width, int height, Frame frame, boolean facingRight) {
		BufferedImage image = getFrame(frame);
		if (image == null) return;
		if (facingRight) {
			g2d.drawImage(image, x, y, width, height, null);
		} else {
			g2d.drawImage(image, x + width, y, -width, height, null);
		}
	}

	protected BufferedImage getFrame(Frame frame) {
		if (frame == null) frame = Frame.IDLE;
		if (spriteSheet == null) return null;
		int sheetWidth = spriteSheet.getWidth();
		int sheetHeight = spriteSheet.getHeight();
		int tileWidth = sheetWidth / sheetColumns;
		int tileHeight = sheetHeight / sheetRows;
		if (tileWidth <= 0 || tileHeight <= 0) return null;
		int sourceX = Math.max(0, Math.min(frame.column, sheetColumns - 1)) * tileWidth;
		int sourceY = Math.max(0, Math.min(frame.row, sheetRows - 1)) * tileHeight;
		return spriteSheet.getSubimage(sourceX, sourceY, tileWidth, tileHeight);
	}

	public enum Frame {
		IDLE(0, 0),
		RUN_1(1, 0),
		RUN_2(2, 0),
		JUMP(0, 1),
		SHIELD_1(1, 1),
		SHIELD_2(2, 1),
		CHARGE(0, 2),
		ATTACK(1, 2),
		ATTACK_END(2, 2);

		private final int column;
		private final int row;

		Frame(int column, int row) {
			this.column = column;
			this.row = row;
		}
	}
}
