package client.view;

import model.CharacterType;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
 * ゲーム部屋の画面を表示するパネルです。
 */
public class GameRoomPanel extends BaseBackgroundPanel {
	// --------------- クラス定数 ---------------
	private static final int MAX_PLAYERS = 4;
	private static final Font TITLE_FONT = new Font("Meiryo", Font.BOLD, 28);
	private static final Font INFO_FONT = new Font("Meiryo", Font.PLAIN, 18);
	private static final Font STATUS_FONT = new Font("Meiryo", Font.PLAIN, 14);
	private static final Color STATUS_BG = new Color(200, 200, 200);
	private static final Color STATUS_FG = new Color(40, 40, 40);
	private static final Color NAME_BG = new Color(170, 170, 170);
	private static final Color NAME_BG_LOCAL = new Color(130, 150, 170);
	private static final Color NAME_FG = new Color(30, 30, 30);
	private static final Color AVATAR_ACTIVE = new Color(70, 70, 70);
	private static final Color AVATAR_INACTIVE = new Color(180, 180, 180);
	private static final Color SHADOW_COLOR = new Color(60, 60, 60, 170);
	private static final CharacterType DEFAULT_CHARACTER = CharacterType.ARCHER;
	private static final CharacterType[] CHARACTER_OPTIONS = new CharacterType[]{
			CharacterType.ARCHER,
			CharacterType.WARRIOR,
			CharacterType.FIGHTER,
			CharacterType.WIZARD
	};
	private static final String ARCHER_IMAGE_PATH = "/resorces/archer.png";
	private static final String WARRIOR_IMAGE_PATH = "/resorces/warrior.png";
	private static final String FIGHTER_IMAGE_PATH = "/resorces/fighter.png";
	private static final String WIZARD_IMAGE_PATH = "/resorces/wizard.png";
	private static final Map<CharacterType, BufferedImage> CHARACTER_IMAGES = new EnumMap<>(CharacterType.class);

	static {
		try {
			CHARACTER_IMAGES.put(CharacterType.ARCHER,
					ImageIO.read(Objects.requireNonNull(GameRoomPanel.class.getResource(ARCHER_IMAGE_PATH))));
			CHARACTER_IMAGES.put(CharacterType.WARRIOR,
					ImageIO.read(Objects.requireNonNull(GameRoomPanel.class.getResource(WARRIOR_IMAGE_PATH))));
			CHARACTER_IMAGES.put(CharacterType.FIGHTER,
					ImageIO.read(Objects.requireNonNull(GameRoomPanel.class.getResource(FIGHTER_IMAGE_PATH))));
			CHARACTER_IMAGES.put(CharacterType.WIZARD,
					ImageIO.read(Objects.requireNonNull(GameRoomPanel.class.getResource(WIZARD_IMAGE_PATH))));
		} catch (IOException | NullPointerException e) {
			throw new RuntimeException("キャラクター画像の読み込みに失敗しました", e);
		}
	}

	// --------------- フィールド ---------------
	private final JLabel roomLabel;
	private final JButton readyButton;
	private final JButton backButton;
	private final PlayerSlot[] slots;
	private final Map<Integer, PlayerEntry> players = new LinkedHashMap<>();
	private String localPlayerName = "";
	private int selectedCharacterIndex = 0;
	private CharacterType selectedCharacter = DEFAULT_CHARACTER;
	private boolean hasLocalSelectionOverride = false;

	/**
	 * GameRoomPanelを構築します。
	 */
	public GameRoomPanel() {
		setLayout(new GridBagLayout());

		JPanel board = new JPanel(new BorderLayout());
		board.setOpaque(false);
		board.setBorder(new EmptyBorder(30, 40, 40, 40));
		board.setPreferredSize(new Dimension(1200, 620));

		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);

		roomLabel = new JLabel("ルーム : -");
		roomLabel.setFont(TITLE_FONT);
		roomLabel.setForeground(new Color(40, 40, 40));
		header.add(roomLabel, BorderLayout.WEST);

		JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		actionPanel.setOpaque(false);

		backButton = new JButton("ホームへ戻る");
		backButton.setFont(INFO_FONT);
		backButton.setForeground(new Color(40, 40, 40));
		backButton.setBackground(new Color(185, 185, 185));
		backButton.setFocusPainted(false);
		backButton.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(140, 140, 140), 1),
				BorderFactory.createEmptyBorder(8, 18, 8, 18)
		));
		actionPanel.add(backButton);

		readyButton = new JButton("準備完了");
		readyButton.setFont(INFO_FONT);
		readyButton.setForeground(new Color(40, 40, 40));
		readyButton.setBackground(new Color(200, 200, 200));
		readyButton.setFocusPainted(false);
		readyButton.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(140, 140, 140), 1),
				BorderFactory.createEmptyBorder(8, 20, 8, 20)
		));
		actionPanel.add(readyButton);
		header.add(actionPanel, BorderLayout.EAST);

		board.add(header, BorderLayout.NORTH);

		JPanel slotsPanel = new JPanel(new GridLayout(1, MAX_PLAYERS, 35, 0));
		slotsPanel.setOpaque(false);
		slotsPanel.setBorder(new EmptyBorder(40, 10, 0, 10));

		slots = new PlayerSlot[MAX_PLAYERS];
		for (int i = 0; i < MAX_PLAYERS; i++) {
			PlayerSlot slot = new PlayerSlot();
			slots[i] = slot;
			slotsPanel.add(slot);
		}

		board.add(slotsPanel, BorderLayout.CENTER);
		add(board);
	}

	public void setRoomInfo(int roomId, boolean isPublic) {
		SwingUtilities.invokeLater(() -> roomLabel.setText("ルーム : " + roomId));
	}

	private static BufferedImage resolveCharacterImage(CharacterType characterType) {
		return CHARACTER_IMAGES.get(characterType);
	}

	private static String formatCharacterLabel(CharacterType characterType) {
		switch (characterType) {
			case ARCHER:
				return "Archer";
			case WARRIOR:
				return "Warrior";
			case FIGHTER:
				return "Fighter";
			case WIZARD:
				return "Wizard";
			default:
				return " ";
		}
	}

	public void setLocalPlayerName(String playerName) {
		SwingUtilities.invokeLater(() -> {
			localPlayerName = playerName == null ? "" : playerName;
			refreshSlots();
		});
	}

	public void setReadyAction(ActionListener listener) {
		readyButton.addActionListener(listener);
	}

	public void setBackAction(ActionListener listener) {
		backButton.addActionListener(listener);
	}

	public void updatePlayerStatus(int playerId, boolean isReady, CharacterType characterType) {
		SwingUtilities.invokeLater(() -> {
			PlayerEntry entry = players.get(playerId);
			entry.setReady(isReady);
			entry.setCharacter(characterType);
			refreshSlots();
		});
	}

	public void addPlayer(int playerId, String playerName, boolean ready, CharacterType characterType) {
		SwingUtilities.invokeLater(() -> {
			int key = playerId < 0 ? playerName.hashCode() : playerId;
			players.put(key, new PlayerEntry(key, playerName, ready, characterType));
			refreshSlots();
		});
	}

	public void reset() {
		SwingUtilities.invokeLater(() -> {
			players.clear();
			roomLabel.setText("ルーム : -");
			resetSelectedCharacter();
			refreshSlots();
		});
	}

	public CharacterType getSelectedCharacterType() {
		return selectedCharacter;
	}

	private void refreshSlots() {
		List<PlayerEntry> sortedPlayers = new ArrayList<>(players.values());
		sortedPlayers.sort(Comparator
				.comparingInt(PlayerEntry::getId)
				.thenComparing(PlayerEntry::getName));

		CharacterType localCharacter = DEFAULT_CHARACTER;
		for (PlayerEntry entry : sortedPlayers) {
			if (entry.getName().equals(localPlayerName)) {
				localCharacter = entry.getCharacter();
				break;
			}
		}
		if (!hasLocalSelectionOverride) {
			syncSelectedCharacter(localCharacter);
		}
		for (int i = 0; i < slots.length; i++) {
			PlayerEntry entry = i < sortedPlayers.size() ? sortedPlayers.get(i) : null;
			boolean isLocal = entry != null && entry.getName().equals(localPlayerName);
			slots[i].setPlayer(entry, isLocal);
		}
	}

	private void resetSelectedCharacter() {
		selectedCharacterIndex = 0;
		selectedCharacter = DEFAULT_CHARACTER;
		hasLocalSelectionOverride = false;
	}

	private void changeCharacterByDelta(int delta) {
		updateSelectedCharacter(selectedCharacterIndex + delta);
	}

	private void updateSelectedCharacter(int index) {
		Runnable updater = () -> applySelectedCharacter(index);
		if (SwingUtilities.isEventDispatchThread()) {
			updater.run();
		} else {
			SwingUtilities.invokeLater(updater);
		}
	}

	private void applySelectedCharacter(int index) {
		int normalized = ((index % CHARACTER_OPTIONS.length) + CHARACTER_OPTIONS.length) % CHARACTER_OPTIONS.length;
		selectedCharacterIndex = normalized;
		selectedCharacter = CHARACTER_OPTIONS[normalized];
		hasLocalSelectionOverride = true;
		refreshSlots();
	}

	private void syncSelectedCharacter(CharacterType characterType) {
		for (int i = 0; i < CHARACTER_OPTIONS.length; i++) {
			if (CHARACTER_OPTIONS[i] == characterType) {
				selectedCharacterIndex = i;
				selectedCharacter = characterType;
				return;
			}
		}
	}

	public static final class PlayerEntry {
		private final int id;
		private final String name;
		private boolean ready;
		private CharacterType character;

		public PlayerEntry(int id, String name, boolean ready, CharacterType character) {
			this.id = id;
			this.name = name;
			this.ready = ready;
			this.character = character;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public boolean isReady() {
			return ready;
		}

		public CharacterType getCharacter() {
			return character;
		}

		public void setReady(boolean ready) {
			this.ready = ready;
		}

		public void setCharacter(CharacterType character) {
			this.character = character;
		}
	}

	private static final class AvatarPanel extends JComponent {
		private boolean active = false;
		private BufferedImage characterImage;

		private AvatarPanel() {
			setPreferredSize(new Dimension(150, 180));
			setMinimumSize(new Dimension(150, 180));
			setOpaque(false);
		}

		private void setActive(boolean active) {
			this.active = active;
			repaint();
		}

		private void setCharacter(CharacterType characterType) {
			this.characterImage = resolveCharacterImage(characterType);
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int width = getWidth();
			int height = getHeight();

			if (characterImage != null) {
				int inset = 4;
				int drawWidth = width - inset * 2;
				int drawHeight = height - inset * 2;
				double imageAspect = (double) characterImage.getWidth() / characterImage.getHeight();
				double boxAspect = (double) drawWidth / drawHeight;

				int imgWidth;
				int imgHeight;
				if (boxAspect > imageAspect) {
					imgHeight = drawHeight;
					imgWidth = (int) (drawHeight * imageAspect);
				} else {
					imgWidth = drawWidth;
					imgHeight = (int) (drawWidth / imageAspect);
				}
				int imgX = (width - imgWidth) / 2;
				int imgY = (height - imgHeight) / 2;

				if (!active) {
					Composite original = g2d.getComposite();
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
					g2d.drawImage(characterImage, imgX, imgY, imgWidth, imgHeight, null);
					g2d.setComposite(original);
				} else {
					g2d.drawImage(characterImage, imgX, imgY, imgWidth, imgHeight, null);
				}
			}
		}
	}

	private final class PlayerSlot extends JPanel {
		private final JLabel statusLabel;
		private final JLabel nameLabel;
		private final JLabel characterLabel;
		private final AvatarPanel avatarPanel;
		private final ShadowPanel shadowPanel;
		private final JLabel leftArrow;
		private final JLabel rightArrow;
		private boolean isLocalPlayer;

		private PlayerSlot() {
			setOpaque(false);
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			statusLabel = new JLabel("準備中", SwingConstants.CENTER);
			statusLabel.setFont(STATUS_FONT);
			statusLabel.setOpaque(true);
			statusLabel.setBackground(STATUS_BG);
			statusLabel.setForeground(STATUS_FG);
			statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			statusLabel.setBorder(new EmptyBorder(6, 14, 6, 14));
			add(statusLabel);
			add(Box.createVerticalStrut(12));

			JPanel avatarRow = new JPanel();
			avatarRow.setOpaque(false);
			avatarRow.setLayout(new BoxLayout(avatarRow, BoxLayout.X_AXIS));
			leftArrow = createArrowLabel("<");
			rightArrow = createArrowLabel(">");
			leftArrow.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (isLocalPlayer) changeCharacterByDelta(-1);
				}
			});
			rightArrow.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (isLocalPlayer) changeCharacterByDelta(1);
				}
			});
			avatarPanel = new AvatarPanel();

			avatarRow.add(leftArrow);
			avatarRow.add(Box.createHorizontalStrut(6));
			avatarRow.add(avatarPanel);
			avatarRow.add(Box.createHorizontalStrut(6));
			avatarRow.add(rightArrow);
			avatarRow.setAlignmentX(Component.CENTER_ALIGNMENT);
			add(avatarRow);

			add(Box.createVerticalStrut(6));
			shadowPanel = new ShadowPanel();
			shadowPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
			add(shadowPanel);
			add(Box.createVerticalStrut(12));

			characterLabel = new JLabel(" ", SwingConstants.CENTER);
			characterLabel.setFont(new Font("Meiryo", Font.PLAIN, 12));
			characterLabel.setForeground(new Color(80, 80, 80));
			characterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			add(characterLabel);
			add(Box.createVerticalStrut(6));

			nameLabel = new JLabel("-", SwingConstants.CENTER);
			nameLabel.setFont(STATUS_FONT);
			nameLabel.setOpaque(true);
			nameLabel.setBackground(NAME_BG);
			nameLabel.setForeground(NAME_FG);
			nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			nameLabel.setBorder(new EmptyBorder(6, 20, 6, 20));
			add(nameLabel);
		}

		private JLabel createArrowLabel(String text) {
			JLabel label = new JLabel(text);
			label.setFont(new Font("Meiryo", Font.BOLD, 22));
			label.setForeground(Color.WHITE);
			label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			label.setVisible(false);
			return label;
		}

		private void setPlayer(PlayerEntry entry, boolean isLocal) {
			isLocalPlayer = isLocal;
			if (entry == null) {
				statusLabel.setText("空き");
				nameLabel.setText("-");
				nameLabel.setBackground(NAME_BG);
				avatarPanel.setActive(false);
				characterLabel.setText(" ");
				avatarPanel.setCharacter(null);
				leftArrow.setVisible(false);
				rightArrow.setVisible(false);
				return;
			}
			statusLabel.setText(entry.isReady() ? "準備完了!" : "準備中");
			nameLabel.setText(entry.getName());
			nameLabel.setBackground(isLocal ? NAME_BG_LOCAL : NAME_BG);
			avatarPanel.setActive(true);

			CharacterType characterType = isLocal ? selectedCharacter : entry.getCharacter();
			characterLabel.setText(formatCharacterLabel(characterType));
			avatarPanel.setCharacter(characterType);
			leftArrow.setVisible(isLocal);
			rightArrow.setVisible(isLocal);
		}
	}

	private static final class ShadowPanel extends JComponent {
		private ShadowPanel() {
			setPreferredSize(new Dimension(160, 20));
			setMinimumSize(new Dimension(160, 20));
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int width = getWidth();
			int height = getHeight();
			g2d.setColor(SHADOW_COLOR);
			g2d.fillOval(20, height / 3, width - 40, height / 3);
		}
	}
}
