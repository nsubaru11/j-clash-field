package client.model;

import model.CharacterType;
import model.GameCharacter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class CharacterSprite extends GameCharacter {
	private static final int DEFAULT_COLUMNS = 3;
	private static final int DEFAULT_ROWS = 3;
	private static final Map<String, BufferedImage> SPRITE_CACHE = new HashMap<>();
	private static final long MOVE_ANIM_GRACE_MS = 180;
	private static final long RUN_FRAME_MS = 120;
	private static final long NORMAL_ATTACK_MS = 320;
	private static final long CHARGE_ATTACK_MS = 480;
	private static final long DEFEND_MS = 420;
	private static final long JUMP_MS = 250;
	private final Color accentColor;
	/**
	 * スプライトシートが指定されていない場合は null になる。
	 */
	private final BufferedImage spriteSheet;
	private final int sheetColumns;
	private final int sheetRows;
	private final RenderState renderState = new RenderState();
	private boolean hasPosition;

	protected CharacterSprite(CharacterType type, String spriteSheetPath) {
		this(type, spriteSheetPath, DEFAULT_COLUMNS, DEFAULT_ROWS, Color.BLACK);
	}

	protected CharacterSprite(CharacterType type, String spriteSheetPath, Color accentColor) {
		this(type, spriteSheetPath, DEFAULT_COLUMNS, DEFAULT_ROWS, accentColor);
	}

	protected CharacterSprite(CharacterType type, String spriteSheetPath, int sheetColumns, int sheetRows) {
		this(type, spriteSheetPath, sheetColumns, sheetRows, Color.BLACK);
	}

	protected CharacterSprite(CharacterType type, String spriteSheetPath, int sheetColumns, int sheetRows, Color accentColor) {
		super(type);
		this.sheetColumns = sheetColumns;
		this.sheetRows = sheetRows;
		this.accentColor = accentColor == null ? Color.BLACK : accentColor;
		this.spriteSheet = loadSpriteSheet(spriteSheetPath);
	}

	/**
	 * 未対応 or null の場合は null を返す(描画なし扱い)。
	 */
	public static CharacterSprite forType(CharacterType type) {
		if (type == null) return null;
		return createSprite(type);
	}

	private static CharacterSprite createSprite(CharacterType type) {
		switch (type) {
			case ARCHER:
				return new ArcherSprite();
			case WARRIOR:
				return new WarriorSprite();
			case FIGHTER:
				return new FighterSprite();
			case WIZARD:
				return new WizardSprite();
			default:
				return null;
		}
	}

	private static BufferedImage loadSpriteSheet(String spriteSheetPath) {
		if (spriteSheetPath == null || spriteSheetPath.isEmpty()) return null;
		BufferedImage cached = SPRITE_CACHE.get(spriteSheetPath);
		if (cached != null) return cached;
		try {
			BufferedImage image = ImageIO.read(Objects.requireNonNull(CharacterSprite.class.getResource(spriteSheetPath)));
			SPRITE_CACHE.put(spriteSheetPath, image);
			return image;
		} catch (IOException | NullPointerException e) {
			throw new RuntimeException("Failed to load sprite sheet: " + spriteSheetPath, e);
		}
	}

	@Override
	public double getGravity() {
		return 0;
	}

	@Override
	public void specialAttack() {
		// client-side visual only
	}

	public Color getAccentColor() {
		return accentColor;
	}

	public RenderState getRenderState() {
		return renderState;
	}

	public boolean hasPosition() {
		return hasPosition;
	}

	public void recordPosition(double x, double y, long now) {
		if (hasPosition && position != null) {
			renderState.recordMovementDelta(x - position.getX(), y - position.getY(), now);
		}
		setPosition(x, y);
		hasPosition = true;
	}

	public void recordMove(int direction, long now) {
		renderState.recordMove(direction, now);
	}

	public void recordFacing(double x, double y) {
		renderState.recordFacing(x, y);
	}

	public void recordAction(Action action, long now) {
		renderState.recordAction(action, now);
	}

	public boolean isFacingRight() {
		return renderState.isFacingRight();
	}

	public void draw(Graphics2D g2d, int x, int y, int width, int height) {
		draw(g2d, x, y, width, height, Frame.IDLE, true);
	}

	public BufferedImage getIdleImage() {
		return getFrame(Frame.IDLE);
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

	public Frame resolveFrame(long now, double worldY, double groundY) {
		return resolveFrame(renderState, now, worldY, groundY);
	}

	public Frame resolveFrame(RenderState state, long now, double worldY, double groundY) {
		if (state == null) return Frame.IDLE;
		Frame actionFrame = resolveActionFrame(state, now);
		if (actionFrame != null) return actionFrame;
		if (worldY > groundY + 2) {
			return Frame.JUMP;
		}
		if (now - state.lastMoveMs <= MOVE_ANIM_GRACE_MS) {
			long phase = (now / RUN_FRAME_MS) % 2;
			return phase == 0 ? Frame.RUN_1 : Frame.RUN_2;
		}
		return Frame.IDLE;
	}

	private Frame resolveActionFrame(RenderState state, long now) {
		if (state.action == Action.NONE) return null;
		if (now > state.actionEndMs) {
			state.action = Action.NONE;
			return null;
		}
		long elapsed = now - state.actionStartMs;
		long chargePhase1 = CHARGE_ATTACK_MS * 45 / 100;
		long chargePhase2 = CHARGE_ATTACK_MS * 70 / 100;
		switch (state.action) {
			case NORMAL_ATTACK:
				return elapsed < NORMAL_ATTACK_MS / 2 ? Frame.ATTACK : Frame.ATTACK_END;
			case CHARGE_ATTACK:
				if (elapsed < chargePhase1) return Frame.CHARGE;
				if (elapsed < chargePhase2) return Frame.ATTACK;
				return Frame.ATTACK_END;
			case CHARGE_HOLD:
				return Frame.CHARGE;
			case DEFEND:
				return ((elapsed / 120) % 2 == 0) ? Frame.SHIELD_1 : Frame.SHIELD_2;
			case JUMP:
				return Frame.JUMP;
			default:
				return null;
		}
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

	public enum Action {
		NONE,
		NORMAL_ATTACK,
		CHARGE_HOLD,
		CHARGE_ATTACK,
		DEFEND,
		JUMP
	}

	public static final class RenderState {
		private boolean facingRight = true;
		private long lastMoveMs;
		private Action action = Action.NONE;
		private long actionStartMs;
		private long actionEndMs;

		public void recordMove(int direction, long now) {
			lastMoveMs = now;
		}

		public void recordMovementDelta(double dx, double dy, long now) {
			if (Math.abs(dx) > 0.1 || Math.abs(dy) > 0.1) {
				lastMoveMs = now;
			}
		}

		public void recordFacing(double x, double y) {
			if (x > 0) facingRight = true;
			else if (x < 0) facingRight = false;
		}

		public void recordAction(Action action, long now) {
			if (action == null || action == Action.NONE) return;
			this.action = action;
			actionStartMs = now;
			actionEndMs = action == Action.CHARGE_HOLD ? Long.MAX_VALUE : now + resolveActionDuration(action);
		}

		public boolean isFacingRight() {
			return facingRight;
		}
	}

	private static long resolveActionDuration(Action action) {
		switch (action) {
			case NORMAL_ATTACK:
				return NORMAL_ATTACK_MS;
			case CHARGE_ATTACK:
				return CHARGE_ATTACK_MS;
			case CHARGE_HOLD:
				return 0;
			case DEFEND:
				return DEFEND_MS;
			case JUMP:
				return JUMP_MS;
			default:
				return 0;
		}
	}
}
