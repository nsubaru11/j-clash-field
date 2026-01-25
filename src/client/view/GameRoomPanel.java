package client.view;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
	private static final String DEFAULT_CHARACTER_NAME = "Archer";
	private static final String DEFAULT_CHARACTER_IMAGE_PATH = "/client/assets/archer.png";
	private static final Map<String, BufferedImage> CHARACTER_IMAGES = new LinkedHashMap<>();

	static {
		try {
			CHARACTER_IMAGES.put(DEFAULT_CHARACTER_NAME.toLowerCase(),
					ImageIO.read(Objects.requireNonNull(GameRoomPanel.class.getResource(DEFAULT_CHARACTER_IMAGE_PATH))));
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
			PlayerSlot slot = new PlayerSlot(i == 0);
			slots[i] = slot;
			slotsPanel.add(slot);
		}

		board.add(slotsPanel, BorderLayout.CENTER);
		add(board);
	}

	public void setRoomInfo(int roomId, boolean isPublic) {
		SwingUtilities.invokeLater(() -> roomLabel.setText("ルーム : " + roomId));
	}

	public void setPlayers(Collection<PlayerEntry> playerEntries) {
		SwingUtilities.invokeLater(() -> {
			players.clear();
			for (PlayerEntry entry : playerEntries) {
				int key = entry.getId() < 0 ? entry.getName().hashCode() : entry.getId();
				players.put(key, entry);
			}
			refreshSlots();
		});
	}

	public void addPlayer(int playerId, String playerName) {
		SwingUtilities.invokeLater(() -> {
			int key = playerId < 0 ? playerName.hashCode() : playerId;
			players.put(key, new PlayerEntry(key, playerName, false, ""));
			refreshSlots();
		});
	}

	public void reset() {
		SwingUtilities.invokeLater(() -> {
			players.clear();
			roomLabel.setText("ルーム : -");
			refreshSlots();
		});
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

	private void refreshSlots() {
		List<PlayerEntry> sortedPlayers = new ArrayList<>(players.values());
		sortedPlayers.sort(Comparator
				.comparingInt(PlayerEntry::getId)
				.thenComparing(PlayerEntry::getName));
		for (int i = 0; i < slots.length; i++) {
			PlayerEntry entry = i < sortedPlayers.size() ? sortedPlayers.get(i) : null;
			boolean isLocal = entry != null && entry.getName().equals(localPlayerName);
			slots[i].setPlayer(entry, isLocal);
		}
	}

	private static String formatCharacter(PlayerEntry entry) {
		String character = entry.getCharacter();
		if (character == null || character.isEmpty() || "0".equals(character)) return DEFAULT_CHARACTER_NAME;
		int lastDot = character.lastIndexOf('.');
		return lastDot >= 0 ? character.substring(lastDot + 1) : character;
	}

	private static BufferedImage resolveCharacterImage(String characterName) {
		if (characterName == null || characterName.isEmpty()) return null;
		return CHARACTER_IMAGES.get(characterName.toLowerCase());
	}

	public static final class PlayerEntry {
		private final int id;
		private final String name;
		private final boolean ready;
		private final String character;

		public PlayerEntry(int id, String name, boolean ready, String character) {
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

		public String getCharacter() {
			return character;
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
		private final boolean showArrows;

		private PlayerSlot(boolean showArrows) {
			this.showArrows = showArrows;
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
			avatarPanel = new AvatarPanel();

			if (showArrows) {
				avatarRow.add(leftArrow);
				avatarRow.add(Box.createHorizontalStrut(6));
			}
			avatarRow.add(avatarPanel);
			if (showArrows) {
				avatarRow.add(Box.createHorizontalStrut(6));
				avatarRow.add(rightArrow);
			}
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
			label.setForeground(new Color(90, 90, 90));
			label.setVisible(false);
			return label;
		}

		private void setPlayer(PlayerEntry entry, boolean isLocal) {
			if (entry == null) {
				statusLabel.setText("空き");
				nameLabel.setText("-");
				nameLabel.setBackground(NAME_BG);
				avatarPanel.setActive(false);
				characterLabel.setText(" ");
				avatarPanel.setCharacter(null);
				setArrowVisible(false);
				return;
			}
			statusLabel.setText(entry.isReady() ? "準備完了!" : "準備中");
			nameLabel.setText(entry.getName());
			nameLabel.setBackground(isLocal ? NAME_BG_LOCAL : NAME_BG);
			avatarPanel.setActive(true);
			String character = formatCharacter(entry);
			characterLabel.setText(character.isEmpty() ? " " : character);
			avatarPanel.setCharacter(character);
			setArrowVisible(isLocal);
		}

		private void setArrowVisible(boolean visible) {
			if (!showArrows) return;
			leftArrow.setVisible(visible);
			rightArrow.setVisible(visible);
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

		private void setCharacter(String characterName) {
			this.characterImage = resolveCharacterImage(characterName);
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

				return;
			}
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
