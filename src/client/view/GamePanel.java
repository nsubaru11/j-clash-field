package client.view;

import model.CharacterType;
import model.ProjectileType;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ゲーム画面を表示するパネルです。
 */
public class GamePanel extends BaseBackgroundPanel {
	// --------------- クラス定数 ---------------
	private static final int SCREEN_WIDTH = 1000;
	private static final int SCREEN_HEIGHT = 520;
	private static final int INFO_HEIGHT = 160;
	private static final int SCREEN_PADDING = 32;
	private static final int INFO_PADDING = 24;
	private static final int MAX_PLAYERS = 4;
	private static final Color SKY_COLOR = new Color(29, 184, 220);
	private static final Color GROUND_COLOR = new Color(122, 214, 98);
	private static final Color PLATFORM_COLOR = new Color(15, 15, 15);
	private static final Color CLOUD_COLOR = new Color(196, 242, 248, 220);
	private static final Color PANEL_BG = new Color(10, 10, 10, 150);
	private static final Color INFO_TEXT = new Color(250, 250, 250);
	private static final Color INFO_SUB_TEXT = new Color(210, 210, 210);
	private static final Font NAME_FONT = new Font("Meiryo", Font.BOLD, 16);
	private static final Font HP_FONT = new Font("Meiryo", Font.PLAIN, 14);
	private static final double WORLD_GROUND_Y = SCREEN_HEIGHT * 0.36;
	private static final long MOVE_ANIM_GRACE_MS = 180;
	private static final long RUN_FRAME_MS = 120;
	private static final long NORMAL_ATTACK_MS = 320;
	private static final long CHARGE_ATTACK_MS = 480;
	private static final long DEFEND_MS = 420;
	private static final long JUMP_MS = 250;
	private static final long PROJECTILE_TTL_MS = 600;
	private static final String ARROW_IMAGE = "/resorces/arrow.png";
	private static final String MAGIC_IMAGE = "/resorces/magic.png";
	private static final Map<ProjectileType, BufferedImage> PROJECTILE_IMAGES = new EnumMap<>(ProjectileType.class);

	static {
		try {
			PROJECTILE_IMAGES.put(ProjectileType.ARROW,
					ImageIO.read(Objects.requireNonNull(GamePanel.class.getResource(ARROW_IMAGE))));
			PROJECTILE_IMAGES.put(ProjectileType.MAGIC,
					ImageIO.read(Objects.requireNonNull(GamePanel.class.getResource(MAGIC_IMAGE))));
		} catch (IOException | NullPointerException e) {
			throw new RuntimeException("プロジェクタイル画像の読み込みに失敗しました", e);
		}
	}
	// --------------- フィールド ---------------
	private final ScreenPanel screenPanel;
	private final InfoPanel infoPanel;
	private final Map<Integer, PlayerState> players = new LinkedHashMap<>();
	private final Map<Long, ProjectileState> projectiles = new LinkedHashMap<>();
	private final Timer repaintTimer;
	private int localPlayerId = -1;
	private Runnable moveLeftAction;
	private Runnable moveRightAction;
	private Runnable moveUpAction;
	private Runnable moveDownAction;
	private Runnable jumpAction;
	private Runnable normalAttackAction;
	private Runnable chargeStartAction;
	private Runnable chargeAttackAction;
	private Runnable defendAction;
	private Runnable resignAction;

	/**
	 * GamePanelを構築します。
	 */
	public GamePanel() {
		setOpaque(false);
		setLayout(new GridBagLayout());
		screenPanel = new ScreenPanel();
		infoPanel = new InfoPanel();

		GridBagConstraints screenConstraints = new GridBagConstraints();
		screenConstraints.gridx = 0;
		screenConstraints.gridy = 0;
		screenConstraints.insets = new Insets(SCREEN_PADDING, SCREEN_PADDING, 12, SCREEN_PADDING);
		screenConstraints.anchor = GridBagConstraints.CENTER;
		add(screenPanel, screenConstraints);

		GridBagConstraints infoConstraints = new GridBagConstraints();
		infoConstraints.gridx = 0;
		infoConstraints.gridy = 1;
		infoConstraints.insets = new Insets(0, INFO_PADDING, INFO_PADDING, INFO_PADDING);
		infoConstraints.anchor = GridBagConstraints.CENTER;
		add(infoPanel, infoConstraints);

		repaintTimer = new Timer(1000 / 60, e -> {
			screenPanel.repaint();
			infoPanel.repaint();
		});
		repaintTimer.start();
	}

	public void setInputActions(
			Runnable moveLeft,
			Runnable moveRight,
			Runnable moveUp,
			Runnable moveDown,
			Runnable jump,
			Runnable normalAttack,
			Runnable chargeStart,
			Runnable chargeAttack,
			Runnable defend,
			Runnable resign
	) {
		this.moveLeftAction = moveLeft;
		this.moveRightAction = moveRight;
		this.moveUpAction = moveUp;
		this.moveDownAction = moveDown;
		this.jumpAction = jump;
		this.normalAttackAction = normalAttack;
		this.chargeStartAction = chargeStart;
		this.chargeAttackAction = chargeAttack;
		this.defendAction = defend;
		this.resignAction = resign;
	}

	public void setLocalPlayerId(int playerId) {
		this.localPlayerId = playerId;
	}

	public void clearPlayers() {
		players.clear();
		projectiles.clear();
	}

	public void setPlayerInfo(int playerId, String name, CharacterType characterType) {
		PlayerState state = players.computeIfAbsent(playerId, PlayerState::new);
		state.name = name == null ? "" : name;
		state.characterType = characterType;
	}

	public void updatePlayerPosition(int playerId, double x, double y) {
		PlayerState state = players.computeIfAbsent(playerId, PlayerState::new);
		long now = System.currentTimeMillis();
		if (state.hasPosition) {
			double dx = x - state.x;
			double dy = y - state.y;
			if (Math.abs(dx) > 0.1) state.facingRight = dx >= 0;
			if (Math.abs(dx) > 0.1 || Math.abs(dy) > 0.1) {
				state.lastMoveMs = now;
			}
		}
		state.x = x;
		state.y = y;
		state.hasPosition = true;
	}

	public void updatePlayerHp(int playerId, int hp) {
		PlayerState state = players.computeIfAbsent(playerId, PlayerState::new);
		state.hp = hp;
	}

	public void recordPlayerAction(int playerId, PlayerAction action) {
		if (action == null || action == PlayerAction.NONE) return;
		PlayerState state = players.computeIfAbsent(playerId, PlayerState::new);
		long now = System.currentTimeMillis();
		state.action = action;
		state.actionStartMs = now;
		if (action == PlayerAction.CHARGE_HOLD) {
			state.actionEndMs = Long.MAX_VALUE;
		} else {
			state.actionEndMs = now + resolveActionDuration(action);
		}
	}

	public void updateProjectile(long projectileId, ProjectileType type, double x, double y, double power) {
		long now = System.currentTimeMillis();
		ProjectileState state = projectiles.computeIfAbsent(projectileId, ProjectileState::new);
		if (state.hasPosition) {
			double dx = x - state.x;
			if (Math.abs(dx) > 0.1) state.facingRight = dx >= 0;
		}
		state.type = type;
		state.x = x;
		state.y = y;
		state.power = power;
		state.hasPosition = true;
		state.lastSeenMs = now;
	}

	public void removeProjectile(long projectileId) {
		projectiles.remove(projectileId);
	}

	private List<PlayerState> getOrderedPlayers() {
		List<PlayerState> list = new ArrayList<>(players.values());
		list.sort(Comparator.comparingInt(state -> state.playerId));
		return list;
	}

	private Color resolveCharacterColor(CharacterType type) {
		if (type == null) return Color.BLACK;
		switch (type) {
			case ARCHER:
				return new Color(40, 110, 200);
			case WARRIOR:
				return new Color(235, 77, 71);
			case FIGHTER:
				return new Color(178, 100, 218);
			case WIZARD:
				return new Color(255, 137, 40);
			default:
				return Color.BLACK;
		}
	}

	private void bindKey(KeyStroke keyStroke, String actionKey, Runnable action) {
		InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = getActionMap();
		inputMap.put(keyStroke, actionKey);
		actionMap.put(actionKey, new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				if (action != null) action.run();
			}
		});
	}

	private void setupKeyBindings() {
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "move_left_arrow", () -> {
			recordLocalMove(-1);
			if (moveLeftAction != null) moveLeftAction.run();
		});
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "move_right_arrow", () -> {
			recordLocalMove(1);
			if (moveRightAction != null) moveRightAction.run();
		});
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "move_down_arrow", () -> {
			recordLocalMove(0);
			if (moveDownAction != null) moveDownAction.run();
		});
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "move_left", () -> {
			recordLocalMove(-1);
			if (moveLeftAction != null) moveLeftAction.run();
		});
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "move_right", () -> {
			recordLocalMove(1);
			if (moveRightAction != null) moveRightAction.run();
		});
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "jump_arrow", () -> {
			recordLocalAction(PlayerAction.JUMP);
			if (jumpAction != null) jumpAction.run();
		});
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "jump", () -> {
			recordLocalAction(PlayerAction.JUMP);
			if (jumpAction != null) jumpAction.run();
		});
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "move_down", () -> {
			recordLocalMove(0);
			if (moveDownAction != null) moveDownAction.run();
		});
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "jump", () -> {
			recordLocalAction(PlayerAction.JUMP);
			if (jumpAction != null) jumpAction.run();
		});
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0), "defend", () -> {
			recordLocalAction(PlayerAction.DEFEND);
			if (defendAction != null) defendAction.run();
		});
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "resign", () -> {
			if (resignAction != null) resignAction.run();
		});
	}

	private void recordLocalMove(int direction) {
		if (localPlayerId < 0) return;
		PlayerState state = players.computeIfAbsent(localPlayerId, PlayerState::new);
		long now = System.currentTimeMillis();
		state.lastMoveMs = now;
		if (direction != 0) state.facingRight = direction > 0;
	}

	private void recordLocalAction(PlayerAction action) {
		if (localPlayerId < 0) return;
		recordPlayerAction(localPlayerId, action);
	}

	private long resolveActionDuration(PlayerAction action) {
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

	private CharacterView.Frame resolveFrame(PlayerState state, long now) {
		CharacterView.Frame actionFrame = resolveActionFrame(state, now);
		if (actionFrame != null) return actionFrame;
		if (state.hasPosition && state.y > WORLD_GROUND_Y + 2) {
			return CharacterView.Frame.JUMP;
		}
		if (now - state.lastMoveMs <= MOVE_ANIM_GRACE_MS) {
			long phase = (now / RUN_FRAME_MS) % 2;
			return phase == 0 ? CharacterView.Frame.RUN_1 : CharacterView.Frame.RUN_2;
		}
		return CharacterView.Frame.IDLE;
	}

	private CharacterView.Frame resolveActionFrame(PlayerState state, long now) {
		if (state.action == PlayerAction.NONE) return null;
		if (now > state.actionEndMs) {
			state.action = PlayerAction.NONE;
			return null;
		}
		long elapsed = now - state.actionStartMs;
		long chargePhase1 = CHARGE_ATTACK_MS * 45 / 100;
		long chargePhase2 = CHARGE_ATTACK_MS * 70 / 100;
		switch (state.action) {
			case NORMAL_ATTACK:
				return elapsed < NORMAL_ATTACK_MS / 2 ? CharacterView.Frame.ATTACK : CharacterView.Frame.ATTACK_END;
			case CHARGE_ATTACK:
				if (elapsed < chargePhase1) return CharacterView.Frame.CHARGE;
				if (elapsed < chargePhase2) return CharacterView.Frame.ATTACK;
				return CharacterView.Frame.ATTACK_END;
			case CHARGE_HOLD:
				return CharacterView.Frame.CHARGE;
			case DEFEND:
				return ((elapsed / 120) % 2 == 0) ? CharacterView.Frame.SHIELD_1 : CharacterView.Frame.SHIELD_2;
			case JUMP:
				return CharacterView.Frame.JUMP;
			default:
				return null;
		}
	}

	public enum PlayerAction {
		NONE,
		NORMAL_ATTACK,
		CHARGE_HOLD,
		CHARGE_ATTACK,
		DEFEND,
		JUMP
	}

	private static final class PlayerState {
		private final int playerId;
		private String name = "";
		private CharacterType characterType = CharacterType.ARCHER;
		private int hp = 100;
		private double x;
		private double y;
		private boolean hasPosition;
		private boolean facingRight = true;
		private long lastMoveMs;
		private PlayerAction action = PlayerAction.NONE;
		private long actionStartMs;
		private long actionEndMs;

		private PlayerState(int playerId) {
			this.playerId = playerId;
		}
	}

	private static final class ProjectileState {
		private final long projectileId;
		private ProjectileType type;
		private double x;
		private double y;
		private double power = 1.0;
		private boolean hasPosition;
		private boolean facingRight = true;
		private long lastSeenMs;

		private ProjectileState(long projectileId) {
			this.projectileId = projectileId;
		}
	}

	private final class ScreenPanel extends JComponent {
		private static final int TILE_SIZE = 32;
		private static final int MARKER_SIZE = 18;
		private boolean rightMouseDown;

		private ScreenPanel() {
			setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
			setMinimumSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
			setOpaque(false);
			setFocusable(true);
			setupKeyBindings();
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					requestFocusInWindow();
					if (SwingUtilities.isLeftMouseButton(e)) {
						recordLocalAction(PlayerAction.NORMAL_ATTACK);
						if (normalAttackAction != null) normalAttackAction.run();
					} else if (SwingUtilities.isRightMouseButton(e)) {
						rightMouseDown = true;
						recordLocalAction(PlayerAction.CHARGE_HOLD);
						if (chargeStartAction != null) chargeStartAction.run();
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					if (SwingUtilities.isRightMouseButton(e) && rightMouseDown) {
						rightMouseDown = false;
						recordLocalAction(PlayerAction.CHARGE_ATTACK);
						if (chargeAttackAction != null) chargeAttackAction.run();
					}
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int width = getWidth();
			int height = getHeight();

			g2d.setColor(SKY_COLOR);
			g2d.fillRect(0, 0, width, height);

			g2d.setColor(CLOUD_COLOR);
			drawCloud(g2d, width * 0.08, height * 0.08, 120, 48);
			drawCloud(g2d, width * 0.38, height * 0.22, 140, 54);
			drawCloud(g2d, width * 0.78, height * 0.12, 120, 46);

			g2d.setColor(PLATFORM_COLOR);
			drawPlatform(g2d, width * 0.12, height * 0.46, width * 0.32, 10);
			drawPlatform(g2d, width * 0.52, height * 0.24, width * 0.32, 10);
			drawPlatform(g2d, width * 0.58, height * 0.52, width * 0.32, 10);

			int groundTop = (int) (height * 0.64);
			g2d.setColor(GROUND_COLOR);
			g2d.fillRect(0, groundTop, width, height - groundTop);

			long now = System.currentTimeMillis();
			drawCharacters(g2d, width, height, now);
			drawProjectiles(g2d, width, height, now);
		}

		private void drawCharacters(Graphics2D g2d, int width, int height, long now) {
			List<PlayerState> entries = getOrderedPlayers();
			if (entries.isEmpty()) return;

			double scaleX = width / (double) SCREEN_WIDTH;
			double scaleY = height / (double) SCREEN_HEIGHT;
			int spriteWidth = (int) Math.round(TILE_SIZE * 3 * scaleX);
			int spriteHeight = (int) Math.round(TILE_SIZE * 3 * scaleY);
			int index = 0;
			for (PlayerState state : entries) {
				double fallbackX = SCREEN_WIDTH * (0.2 + 0.2 * index);
				double fallbackY = SCREEN_HEIGHT * 0.36;
				double worldX = state.hasPosition ? state.x : fallbackX;
				double worldY = state.hasPosition ? state.y : fallbackY;
				int x = (int) Math.round(worldX * scaleX);
				int y = (int) Math.round(height - (worldY * scaleY));
				int drawX = x - spriteWidth / 2;
				int drawY = y - spriteHeight;
				CharacterView view = CharacterView.forType(state.characterType);
				if (view != null) {
					CharacterView.Frame frame = resolveFrame(state, now);
					view.draw(g2d, drawX, drawY, spriteWidth, spriteHeight, frame, state.facingRight);
				} else {
					g2d.setColor(resolveCharacterColor(state.characterType));
					g2d.fillRoundRect(drawX, drawY, spriteWidth, spriteHeight, 20, 20);
				}
				if (state.playerId == localPlayerId) {
					drawMarker(g2d, x, drawY - MARKER_SIZE - 6);
				}
				index++;
			}
		}

		private void drawProjectiles(Graphics2D g2d, int width, int height, long now) {
			if (projectiles.isEmpty()) return;
			double scaleX = width / (double) SCREEN_WIDTH;
			double scaleY = height / (double) SCREEN_HEIGHT;
			int baseSize = (int) Math.round(TILE_SIZE * 1.2 * Math.min(scaleX, scaleY));
			List<Long> stale = new ArrayList<>();
			for (ProjectileState projectile : projectiles.values()) {
				if (!projectile.hasPosition) continue;
				if (now - projectile.lastSeenMs > PROJECTILE_TTL_MS) {
					stale.add(projectile.projectileId);
					continue;
				}
				BufferedImage image = PROJECTILE_IMAGES.get(projectile.type);
				if (image == null) continue;
				int x = (int) Math.round(projectile.x * scaleX);
				int y = (int) Math.round(height - (projectile.y * scaleY));
				double scale = Math.min(1.4, 0.8 + 0.2 * projectile.power);
				int projectileSize = (int) Math.round(baseSize * scale);
				int drawX = x - projectileSize / 2;
				int drawY = y - projectileSize / 2;
				if (projectile.facingRight) {
					g2d.drawImage(image, drawX, drawY, projectileSize, projectileSize, null);
				} else {
					g2d.drawImage(image, drawX + projectileSize, drawY, -projectileSize, projectileSize, null);
				}
			}
			for (Long projectileId : stale) {
				projectiles.remove(projectileId);
			}
		}

		private void drawMarker(Graphics2D g2d, int centerX, int topY) {
			int half = MARKER_SIZE / 2;
			Path2D path = new Path2D.Double();
			path.moveTo(centerX - half, topY);
			path.lineTo(centerX + half, topY);
			path.lineTo(centerX, topY + MARKER_SIZE);
			path.closePath();
			g2d.setColor(new Color(0, 0, 0));
			g2d.fill(path);
		}

		private void drawCloud(Graphics2D g2d, double x, double y, int width, int height) {
			int baseX = (int) x;
			int baseY = (int) y;
			g2d.fillRoundRect(baseX, baseY + height / 3, width, height / 2, height, height);
			g2d.fillOval(baseX + width / 8, baseY, width / 3, height / 2);
			g2d.fillOval(baseX + width / 2, baseY, width / 3, height / 2);
		}

		private void drawPlatform(Graphics2D g2d, double x, double y, double width, int height) {
			g2d.fillRect((int) x, (int) y, (int) width, height);
		}
	}

	private final class InfoPanel extends JComponent {

		private InfoPanel() {
			setPreferredSize(new Dimension(SCREEN_WIDTH, INFO_HEIGHT));
			setMinimumSize(new Dimension(SCREEN_WIDTH, INFO_HEIGHT));
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int width = getWidth();
			int height = getHeight();

			g2d.setColor(PANEL_BG);
			g2d.fillRoundRect(0, 0, width, height, 24, 24);

			List<PlayerState> entries = getOrderedPlayers();
			int entryWidth = width / MAX_PLAYERS;
			int iconSize = Math.min(80, height - 40);
			int iconY = (height - iconSize) / 2 - 8;

			for (int i = 0; i < MAX_PLAYERS; i++) {
				PlayerState state = i < entries.size() ? entries.get(i) : null;
				int entryX = i * entryWidth;
				int centerX = entryX + entryWidth / 2;
				g2d.setColor(state != null ? resolveCharacterColor(state.characterType) : Color.BLACK);
				g2d.fillOval(centerX - iconSize / 2, iconY, iconSize, iconSize);

				g2d.setColor(INFO_TEXT);
				g2d.setFont(NAME_FONT);
				String name = state != null ? state.name : "-";
				FontMetrics nameMetrics = g2d.getFontMetrics();
				int nameY = iconY + iconSize + nameMetrics.getAscent() + 8;
				g2d.drawString(name, centerX - nameMetrics.stringWidth(name) / 2, nameY);

				g2d.setColor(INFO_SUB_TEXT);
				g2d.setFont(HP_FONT);
				if (state != null) {
					String hpText = "HP" + state.hp + "%";
					FontMetrics hpMetrics = g2d.getFontMetrics();
					g2d.drawString(hpText, centerX - hpMetrics.stringWidth(hpText) / 2, nameY + hpMetrics.getAscent() + 4);
				}
			}
		}
	}
}
