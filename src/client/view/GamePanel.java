package client.view;

import client.model.GameCharacterClient;
import model.CharacterType;
import model.GameCharacter;
import model.PlayerInfo;
import model.ProjectileType;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
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
	private static final int SCREEN_WIDTH = 1280;
	private static final int SCREEN_HEIGHT = 720;
	private static final int INFO_HEIGHT = 160;
	private static final int SCREEN_PADDING = 32;
	private static final int INFO_PADDING = 24;
	private static final int MAX_PLAYERS = 4;
	private static final Color PANEL_BG = new Color(10, 10, 10, 150);
	private static final Color INFO_TEXT = new Color(250, 250, 250);
	private static final Color INFO_SUB_TEXT = new Color(210, 210, 210);
	private static final Font NAME_FONT = new Font("Meiryo", Font.BOLD, 16);
	private static final Font HP_FONT = new Font("Meiryo", Font.PLAIN, 14);
	private static final double WORLD_GROUND_Y = SCREEN_HEIGHT * 0.255;
	private static final long PROJECTILE_TTL_MS = 600;
	private static final int DEFEND_HOLD_INTERVAL_MS = 120;
	private static final String ARROW_IMAGE = "/resources/arrow.png";
	private static final String MAGIC_IMAGE = "/resources/magic.png";
	private static final String BACKGROUND_IMAGE = "/resources/gameBackGround.png";
	private static final Map<ProjectileType, BufferedImage> PROJECTILE_IMAGES = new EnumMap<>(ProjectileType.class);
	private static BufferedImage backgroundImage;

	static {
		try {
			PROJECTILE_IMAGES.put(ProjectileType.ARROW,
					ImageIO.read(Objects.requireNonNull(GamePanel.class.getResource(ARROW_IMAGE))));
			PROJECTILE_IMAGES.put(ProjectileType.MAGIC,
					ImageIO.read(Objects.requireNonNull(GamePanel.class.getResource(MAGIC_IMAGE))));
			backgroundImage = ImageIO.read(Objects.requireNonNull(GamePanel.class.getResource(BACKGROUND_IMAGE)));
		} catch (IOException | NullPointerException e) {
			throw new RuntimeException("画像の読み込みに失敗しました", e);
		}
	}
	// --------------- フィールド ---------------
	private final ScreenPanel screenPanel;
	private final InfoPanel infoPanel;
	private final Map<Integer, PlayerInfo> players = new LinkedHashMap<>();
	private final Map<Long, ProjectileState> projectiles = new LinkedHashMap<>();
	private final Timer repaintTimer;
	private final Timer defendTimer;
	private int localPlayerId = -1;
	private Runnable moveLeftAction;
	private Runnable moveRightAction;
	private Runnable moveUpAction;
	private Runnable moveDownAction;
	private Runnable normalAttackAction;
	private Runnable chargeStartAction;
	private Runnable chargeAttackAction;
	private Runnable defendAction;
	private Runnable resignAction;
	private boolean leftKeyDown;
	private boolean rightKeyDown;
	private boolean defendKeyDown;

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

		defendTimer = new Timer(DEFEND_HOLD_INTERVAL_MS, e -> {
			if (!defendKeyDown) return;
			if (defendAction != null) defendAction.run();
		});
		defendTimer.setRepeats(true);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(new Color(0, 0, 0, 90));
		g2d.fillRect(0, 0, getWidth(), getHeight());
	}

	public void setInputActions(
			Runnable moveLeft,
			Runnable moveRight,
			Runnable moveUp,
			Runnable moveDown,
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

	public void setPlayerInfo(PlayerInfo info) {
		if (info == null) return;
		players.put(info.getId(), info);
		ensureCharacter(info);
	}

	private PlayerInfo ensurePlayer(int playerId) {
		PlayerInfo info = players.get(playerId);
		if (info == null) {
			info = new PlayerInfo(playerId, "", false, createCharacterSprite(CharacterType.defaultType()));
			players.put(playerId, info);
		}
		ensureCharacter(info);
		return info;
	}

	private void ensureCharacter(PlayerInfo info) {
		if (info == null) return;
		if (info.getCharacter() == null) {
			info.setCharacter(createCharacterSprite(CharacterType.defaultType()));
		}
	}

	private GameCharacterClient createCharacterSprite(CharacterType characterType) {
		CharacterType resolved = characterType == null ? CharacterType.defaultType() : characterType;
		GameCharacterClient sprite = GameCharacterClient.forType(resolved);
		return sprite != null ? sprite : GameCharacterClient.forType(CharacterType.defaultType());
	}

	public void updatePlayerPosition(int playerId, double x, double y, double facingX, double facingY) {
		PlayerInfo info = ensurePlayer(playerId);
		GameCharacter character = info.getCharacter();
		if (character == null) return;
		long now = System.currentTimeMillis();
		character.setFacingDirection(facingX, facingY);
		if (character instanceof GameCharacterClient) {
			GameCharacterClient sprite = (GameCharacterClient) character;
			sprite.recordPosition(x, y, now);
			sprite.recordFacing(facingX, facingY);
		} else {
			character.setPosition(x, y);
		}
	}

	public void updatePlayerHp(int playerId, int hp) {
		PlayerInfo info = ensurePlayer(playerId);
		GameCharacter character = info.getCharacter();
		if (character == null) return;
		character.setHp(hp);
	}

	public void recordPlayerAction(int playerId, GameCharacterClient.Action action) {
		if (action == null || action == GameCharacterClient.Action.NONE) return;
		PlayerInfo info = ensurePlayer(playerId);
		GameCharacter character = info.getCharacter();
		if (!(character instanceof GameCharacterClient)) return;
		long now = System.currentTimeMillis();
		((GameCharacterClient) character).recordAction(action, now);
	}

	public void updateProjectile(long projectileId, ProjectileType type, double x, double y, double power, double vx, double vy) {
		long now = System.currentTimeMillis();
		ProjectileState state = projectiles.computeIfAbsent(projectileId, ProjectileState::new);
		if (Math.abs(vx) > 0.1 || Math.abs(vy) > 0.1) {
			state.angle = Math.atan2(-vy, vx);
		} else if (state.hasPosition) {
			double dx = x - state.x;
			double dy = y - state.y;
			if (Math.abs(dx) > 0.1 || Math.abs(dy) > 0.1) {
				state.angle = Math.atan2(-dy, dx);
			}
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

	private List<PlayerInfo> getOrderedPlayers() {
		List<PlayerInfo> list = new ArrayList<>(players.values());
		return list;
	}

	private Color resolveCharacterColor(GameCharacter character) {
		CharacterType type = character != null ? character.getType() : null;
		return type != null ? type.getAccentColor() : Color.BLACK;
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
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false), "move_left", () -> {
			leftKeyDown = true;
			recordLocalMove(-1);
			moveLeftAction.run();
		});
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, true), "move_left_release", () -> leftKeyDown = false);
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, false), "move_right", () -> {
			rightKeyDown = true;
			recordLocalMove(1);
			moveRightAction.run();
		});
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, true), "move_right_release", () -> rightKeyDown = false);
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "jump", this::triggerJump);
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "move_down", () -> {
			recordLocalMove(0);
			moveDownAction.run();
		});
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "jump", this::triggerJump);
		Runnable defendPress = () -> {
			if (defendKeyDown) return;
			defendKeyDown = true;
			if (defendAction != null) defendAction.run();
			defendTimer.start();
		};
		Runnable defendRelease = () -> {
			defendKeyDown = false;
			defendTimer.stop();
		};
		bindKey(KeyStroke.getKeyStroke("pressed SHIFT"), "defend_press", defendPress);
		bindKey(KeyStroke.getKeyStroke("shift pressed SHIFT"), "defend_press_shift", defendPress);
		bindKey(KeyStroke.getKeyStroke("released SHIFT"), "defend_release", defendRelease);
		bindKey(KeyStroke.getKeyStroke("shift released SHIFT"), "defend_release_shift", defendRelease);
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "resign", () -> {
			if (resignAction != null) resignAction.run();
		});
	}

	private void recordLocalMove(int direction) {
		PlayerInfo info = ensurePlayer(localPlayerId);
		GameCharacter character = info.getCharacter();
		if (character instanceof GameCharacterClient) {
			((GameCharacterClient) character).recordMove(direction, System.currentTimeMillis());
		}
	}

	private void recordLocalAction(GameCharacterClient.Action action) {
		recordPlayerAction(localPlayerId, action);
	}

	private void triggerJump() {
		recordLocalAction(GameCharacterClient.Action.JUMP);
		if (moveUpAction != null) moveUpAction.run();
		if (leftKeyDown && moveLeftAction != null) {
			recordLocalMove(-1);
			moveLeftAction.run();
		} else if (rightKeyDown && moveRightAction != null) {
			recordLocalMove(1);
			moveRightAction.run();
		}
	}

	private static final class ProjectileState {
		private final long projectileId;
		private ProjectileType type;
		private double x;
		private double y;
		private double power = 1.0;
		private boolean hasPosition;
		private double angle;
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
						recordLocalAction(GameCharacterClient.Action.NORMAL_ATTACK);
						if (normalAttackAction != null) normalAttackAction.run();
					} else if (SwingUtilities.isRightMouseButton(e)) {
						rightMouseDown = true;
						recordLocalAction(GameCharacterClient.Action.CHARGE_HOLD);
						if (chargeStartAction != null) chargeStartAction.run();
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					if (SwingUtilities.isRightMouseButton(e) && rightMouseDown) {
						rightMouseDown = false;
						recordLocalAction(GameCharacterClient.Action.CHARGE_ATTACK);
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

			if (backgroundImage != null) {
				g2d.drawImage(backgroundImage, 0, 0, width, height, null);
			} else {
				g2d.setColor(Color.BLACK);
				g2d.fillRect(0, 0, width, height);
			}

			g2d.setColor(new Color(240, 240, 240, 180));
			g2d.setStroke(new BasicStroke(3));
			g2d.drawRoundRect(1, 1, width - 3, height - 3, 24, 24);

			long now = System.currentTimeMillis();
			drawCharacters(g2d, width, height, now);
			drawProjectiles(g2d, width, height, now);
		}

		private void drawCharacters(Graphics2D g2d, int width, int height, long now) {
			List<PlayerInfo> entries = getOrderedPlayers();
			if (entries.isEmpty()) return;

			double scaleX = width / (double) SCREEN_WIDTH;
			double scaleY = height / (double) SCREEN_HEIGHT;
			int spriteWidth = (int) Math.round(TILE_SIZE * 3 * scaleX);
			int spriteHeight = (int) Math.round(TILE_SIZE * 3 * scaleY);
			int index = 0;
			for (PlayerInfo info : entries) {
				double slotCenter = (index + 0.5) / (double) MAX_PLAYERS;
				double fallbackX = SCREEN_WIDTH * slotCenter;
				double fallbackY = SCREEN_HEIGHT * 0.36;
				double worldX = fallbackX;
				double worldY = fallbackY;
				GameCharacter character = info.getCharacter();
				if (character != null && character.getHp() <= 0) {
					index++;
					continue;
				}
				GameCharacterClient view = character instanceof GameCharacterClient ? (GameCharacterClient) character : null;
				if (character != null && character.getPosition() != null) {
					boolean usePosition = view == null || view.hasPosition();
					if (usePosition) {
						worldX = character.getPosition().getX();
						worldY = character.getPosition().getY();
					}
				}
				int x = (int) Math.round(worldX * scaleX);
				int y = (int) Math.round(height - (worldY * scaleY));
				int drawX = x - spriteWidth / 2;
				int drawY = y - spriteHeight;
				if (view != null) {
					GameCharacterClient.Frame frame = view.resolveFrame(now, worldY, WORLD_GROUND_Y);
					view.draw(g2d, drawX, drawY, spriteWidth, spriteHeight, frame, view.isFacingRight());
				} else {
					g2d.setColor(resolveCharacterColor(character));
					g2d.fillRoundRect(drawX, drawY, spriteWidth, spriteHeight, 20, 20);
				}
				if (info.getId() == localPlayerId) {
					drawMarker(g2d, x, drawY - MARKER_SIZE - 6);
				}
				index++;
			}
		}

		private void drawProjectiles(Graphics2D g2d, int width, int height, long now) {
			if (projectiles.isEmpty()) return;
			double scaleX = width / (double) SCREEN_WIDTH;
			double scaleY = height / (double) SCREEN_HEIGHT;
			int baseSize = (int) Math.round(TILE_SIZE * 1.5 * Math.min(scaleX, scaleY));
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
				AffineTransform original = g2d.getTransform();
				g2d.translate(x, y);
				g2d.rotate(projectile.angle);
				g2d.drawImage(image, -projectileSize / 2, -projectileSize / 2, projectileSize, projectileSize, null);
				g2d.setTransform(original);
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

			List<PlayerInfo> entries = getOrderedPlayers();
			int entryWidth = width / MAX_PLAYERS;
			int iconSize = Math.min(80, height - 40);
			int iconY = (height - iconSize) / 2 - 8;

			for (int i = 0; i < MAX_PLAYERS; i++) {
				PlayerInfo state = i < entries.size() ? entries.get(i) : null;
				int entryX = i * entryWidth;
				int centerX = entryX + entryWidth / 2;
				GameCharacter character = state != null ? state.getCharacter() : null;
				g2d.setColor(resolveCharacterColor(character));
				g2d.fillOval(centerX - iconSize / 2, iconY, iconSize, iconSize);

				g2d.setColor(INFO_TEXT);
				g2d.setFont(NAME_FONT);
				String name = state != null ? state.getName() : "-";
				FontMetrics nameMetrics = g2d.getFontMetrics();
				int nameY = iconY + iconSize + nameMetrics.getAscent() + 8;
				g2d.drawString(name, centerX - nameMetrics.stringWidth(name) / 2, nameY);

				g2d.setColor(INFO_SUB_TEXT);
				g2d.setFont(HP_FONT);
				if (state != null) {
					int hp = character != null ? character.getHp() : 0;
					String hpText = "HP" + hp;
					FontMetrics hpMetrics = g2d.getFontMetrics();
					g2d.drawString(hpText, centerX - hpMetrics.stringWidth(hpText) / 2, nameY + hpMetrics.getAscent() + 4);
				}
			}

			g2d.setColor(new Color(240, 240, 240, 180));
			g2d.setStroke(new BasicStroke(3));
			g2d.drawRoundRect(1, 1, width - 3, height - 3, 24, 24);
		}
	}
}
