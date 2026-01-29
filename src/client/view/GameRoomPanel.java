package client.view;

import client.model.CharacterSprite;
import model.CharacterInfo;
import model.CharacterType;
import model.GameCharacter;
import model.PlayerInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * ゲーム部屋の画面を表示するパネルです。
 */
public class GameRoomPanel extends BaseBackgroundPanel {
	// --------------- クラス定数 ---------------
	private static final int MAX_PLAYERS = 4;
	private static final Font TITLE_FONT = new Font("Meiryo", Font.BOLD, 30);
	private static final Font INFO_FONT = new Font("Meiryo", Font.PLAIN, 16);
	private static final Font STATUS_FONT = new Font("Meiryo", Font.BOLD, 12);
	private static final Font CHARACTER_FONT = new Font("Meiryo", Font.BOLD, 14);
	private static final Font DESCRIPTION_FONT = new Font("Meiryo", Font.PLAIN, 11);
	private static final Font NAME_FONT = new Font("Meiryo", Font.PLAIN, 13);
	private static final Color COLOR_LIGHT = new Color(248, 245, 238);
	private static final Color COLOR_NEUTRAL = new Color(220, 212, 202);
	private static final Color COLOR_DARK = new Color(50, 55, 60);
	private static final Color COLOR_MUTED = new Color(100, 105, 110);
	private static final Color COLOR_ACCENT = new Color(120, 170, 210);
	private static final Map<CharacterType, BufferedImage> CHARACTER_IMAGES = new EnumMap<>(CharacterType.class);

	static {
		for (CharacterType type : CharacterType.values()) {
			CharacterSprite sprite = CharacterSprite.forType(type);
			BufferedImage idleImage = sprite.getIdleImage();
			CHARACTER_IMAGES.put(type, idleImage);
		}
	}

	// --------------- フィールド ---------------
	private final JLabel roomLabel;
	private final JButton readyButton;
	private final JButton backButton;
	private final PlayerSlot[] slots;
	/**
	 * null の場合はスロットが空いていることを示す。
	 */
	private final PlayerInfo[] slotPlayers = new PlayerInfo[MAX_PLAYERS];
	private final Map<Integer, Integer> playerSlotIndex = new HashMap<>(MAX_PLAYERS, 1.0f);
	private final BitSet occupiedSlots = new BitSet(MAX_PLAYERS);
	private int localPlayerId = -1;
	private int selectedCharacterId = CharacterType.DEFAULT_ID;
	private boolean hasLocalSelectionOverride = false;

	/**
	 * GameRoomPanelを構築します。
	 */
	public GameRoomPanel() {
		setLayout(new GridBagLayout());

		// 白い背景ボード（ヘッダー、プレイヤースロット部分を管理）
		JPanel board = new BoardPanel();
		board.setLayout(new BorderLayout());
		board.setBorder(new EmptyBorder(28, 36, 36, 36));
		board.setPreferredSize(new Dimension(1200, 620));

		// region ---------- header ----------
		// ヘッダー管理用パネル（ルーム番号・ホームへ戻るボタン・準備完了ボタン表示用）
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.setBorder(new EmptyBorder(0, 0, 18, 0)); // 下側に余白

		// ルーム番号
		roomLabel = new JLabel("ルーム : -");
		roomLabel.setFont(TITLE_FONT);
		roomLabel.setForeground(COLOR_DARK);
		header.add(roomLabel, BorderLayout.WEST); // ヘッダーの左側に追加

		// ボタン管理用パネル（ホームへ戻るボタン・準備完了ボタン）
		JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		actionPanel.setOpaque(false);

		backButton = createActionButton("ホームへ戻る", withAlpha(COLOR_NEUTRAL, 220), COLOR_NEUTRAL);
		actionPanel.add(backButton);

		readyButton = createActionButton("準備完了", withAlpha(COLOR_ACCENT, 170), COLOR_ACCENT);
		actionPanel.add(readyButton);
		header.add(actionPanel, BorderLayout.EAST); // ヘッダーの右側に追加

		board.add(header, BorderLayout.NORTH);
		// endregion

		// region ---------- playerSlots ----------
		// プレイヤーのスロット表示（プレイヤーが選択しているキャラの表示など）
		JPanel slotsPanel = new JPanel(new GridLayout(1, MAX_PLAYERS, 28, 0));
		slotsPanel.setOpaque(false);
		slotsPanel.setBorder(new EmptyBorder(24, 6, 6, 6));

		slots = new PlayerSlot[MAX_PLAYERS];
		for (int i = 0; i < MAX_PLAYERS; i++) {
			PlayerSlot slot = new PlayerSlot();
			slots[i] = slot;
			slotsPanel.add(slot);
		}

		board.add(slotsPanel, BorderLayout.CENTER);
		// endregion
		add(board);
	}

	// 透明化するヘルパー
	private static Color withAlpha(Color base, int alpha) {
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
	}

	public boolean isReady() {
		return slotPlayers[playerSlotIndex.get(localPlayerId)].isReady();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Paint originalPaint = g2d.getPaint();
		g2d.setColor(new Color(0, 0, 0, 90));
		g2d.fillRect(0, 0, getWidth(), getHeight());
		g2d.setPaint(originalPaint);
	}

	private static CharacterSprite createCharacterSprite(CharacterType characterType) {
		CharacterType resolved = characterType == null ? CharacterType.defaultType() : characterType;
		CharacterSprite sprite = CharacterSprite.forType(resolved);
		return sprite != null ? sprite : CharacterSprite.forType(CharacterType.defaultType());
	}

	private static BufferedImage resolveCharacterImage(CharacterType characterType) {
		return CHARACTER_IMAGES.get(characterType);
	}

	private static String getCharacterDescription(CharacterType characterType) {
		return CharacterInfo.forType(characterType).getDescription();
	}

	private static CharacterType resolveCharacterType(PlayerInfo entry) {
		if (entry == null) return CharacterType.defaultType();
		GameCharacter character = entry.getCharacter();
		return character != null ? character.getType() : CharacterType.defaultType();
	}

	public void setReadyAction(ActionListener listener) {
		readyButton.addActionListener(listener);
	}

	public void setBackAction(ActionListener listener) {
		backButton.addActionListener(listener);
	}

	private JButton createActionButton(String text, Color background, Color border) {
		JButton button = new JButton(text);
		button.setFont(INFO_FONT);
		button.setForeground(COLOR_DARK);
		button.setBackground(background);
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(border, 1, true),
				BorderFactory.createEmptyBorder(8, 20, 8, 20)
		));
		button.setContentAreaFilled(true);
		button.setOpaque(true);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				if (button.isEnabled()) button.setBackground(background.brighter());
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (button.isEnabled()) button.setBackground(background);
			}
		});
		return button;
	}

	public void setRoomInfo(int roomId, boolean isPublic) {
		SwingUtilities.invokeLater(() -> roomLabel.setText((isPublic ? "" : "非") + "公開ルーム : " + roomId));
	}

	public void setReady(int playerId, int characterId) {
		updateReadyStatus(playerId, true, true, characterId);
	}

	public void setUnready(int playerId) {
		updateReadyStatus(playerId, false, false, CharacterType.DEFAULT_ID);
	}

	public void setLocalPlayerId(int playerId) {
		SwingUtilities.invokeLater(() -> {
			localPlayerId = playerId;
			updateSlotForPlayer(localPlayerId);
		});
	}

	public void reset() {
		SwingUtilities.invokeLater(() -> {
			playerSlotIndex.clear();
			occupiedSlots.clear();
			for (int i = 0; i < slots.length; i++) {
				slotPlayers[i] = null;
				slots[i].setPlayer(null, false);
			}
			roomLabel.setText("ルーム : -");
			localPlayerId = -1;
			selectedCharacterId = CharacterType.DEFAULT_ID;
			hasLocalSelectionOverride = false;
		});
	}

	public CharacterType getSelectedCharacterType() {
		return CharacterType.fromId(selectedCharacterId);
	}

	public void addPlayer(int playerId, String playerName, boolean ready, CharacterType characterType) {
		SwingUtilities.invokeLater(() -> {
			int slotIndex = ensureSlot(playerId);
			PlayerInfo entry = slotPlayers[slotIndex];
			if (entry == null) {
				entry = new PlayerInfo(playerId, playerName, ready, createCharacterSprite(characterType));
				slotPlayers[slotIndex] = entry;
			} else {
				entry.setName(playerName);
				entry.setReady(ready);
				updateEntryCharacter(entry, characterType);
			}
			updateSlotView(slotIndex);
		});
	}

	public void removePlayer(int playerId) {
		SwingUtilities.invokeLater(() -> {
			int slotIndex = playerSlotIndex.remove(playerId);
			occupiedSlots.clear(slotIndex);
			slotPlayers[slotIndex] = null;
			slots[slotIndex].setPlayer(null, false);
			if (playerId == localPlayerId) {
				localPlayerId = -1;
				selectedCharacterId = CharacterType.DEFAULT_ID;
				hasLocalSelectionOverride = false;
			}
		});
	}

	private void updateSelectedCharacter(int index) {
		Runnable updater = () -> applySelectedCharacter(selectedCharacterId + index);
		if (SwingUtilities.isEventDispatchThread()) {
			updater.run();
		} else {
			SwingUtilities.invokeLater(updater);
		}
	}

	private void applySelectedCharacter(int index) {
		int len = CharacterType.values().length;
		selectedCharacterId = ((index % len) + len) % len;
		hasLocalSelectionOverride = true;
		updateLocalSlot();
	}

	private void updateEntryCharacter(PlayerInfo entry, CharacterType characterType) {
		CharacterType resolved = characterType == null ? CharacterType.defaultType() : characterType;
		GameCharacter current = entry.getCharacter();
		if (current == null || current.getType() != resolved) {
			entry.setCharacter(createCharacterSprite(resolved));
		}
	}

	private void updateReadyStatus(int playerId, boolean isReady, boolean updateCharacter, int characterId) {
		SwingUtilities.invokeLater(() -> {
			int slotIndex = ensureSlot(playerId);
			PlayerInfo entry = slotPlayers[slotIndex];
			if (entry == null) {
				CharacterType characterType = CharacterType.fromId(characterId);
				entry = new PlayerInfo(playerId, "", isReady, createCharacterSprite(characterType));
				slotPlayers[slotIndex] = entry;
			} else {
				entry.setReady(isReady);
				if (updateCharacter) {
					updateEntryCharacter(entry, CharacterType.fromId(characterId));
				}
			}
			updateSlotView(slotIndex);
		});
	}

	private int ensureSlot(int playerId) {
		Integer existing = playerSlotIndex.get(playerId);
		if (existing != null) return existing;
		int slotIndex = occupiedSlots.nextClearBit(0);
		occupiedSlots.set(slotIndex);
		playerSlotIndex.put(playerId, slotIndex);
		return slotIndex;
	}

	private void updateSlotForPlayer(int playerId) {
		int slotIndex = playerSlotIndex.get(playerId);
		updateSlotView(slotIndex);
	}

	private void updateLocalSlot() {
		updateSlotForPlayer(localPlayerId);
	}

	private void updateSlotView(int slotIndex) {
		PlayerInfo entry = slotPlayers[slotIndex];
		boolean isLocal = entry != null && entry.getId() == localPlayerId;
		if (isLocal && !hasLocalSelectionOverride) {
			selectedCharacterId = resolveCharacterType(entry).getId();
		}
		slots[slotIndex].setPlayer(entry, isLocal);
	}

	private static final class BoardPanel extends BaseDecoratedPanel {
		private BoardPanel() {
		}

		@Override
		protected void paintPanel(Graphics2D g2d) {
			int width = getWidth();
			int height = getHeight();
			paintShadowedRoundedRect(
					g2d,
					0,
					0,
					width,
					height,
					withAlpha(COLOR_LIGHT, 230),
					withAlpha(COLOR_NEUTRAL, 230),
					withAlpha(COLOR_NEUTRAL, 180)
			);
		}
	}

	private static final class AvatarPanel extends BaseDecoratedPanel {
		private boolean active = false;
		private CharacterType characterType;
		private BufferedImage characterImage;

		private AvatarPanel() {
			setPreferredSize(new Dimension(150, 180));
			setMinimumSize(new Dimension(150, 180));
		}

		private void setActive(boolean active) {
			this.active = active;
			repaint();
		}

		private void setCharacter(CharacterType characterType) {
			this.characterType = characterType;
			this.characterImage = characterType == null ? null : resolveCharacterImage(characterType);
			repaint();
		}

		@Override
		protected void paintPanel(Graphics2D g2d) {
			int width = getWidth();
			int height = getHeight();

			int frameInset = 8;
			int frameX = frameInset;
			int frameY = frameInset;
			int frameWidth = width - frameInset * 2;
			int frameHeight = height - frameInset * 2;
			if (frameWidth > 0 && frameHeight > 0) {
				g2d.setColor(withAlpha(COLOR_LIGHT, 210));
				g2d.fillRoundRect(frameX, frameY, frameWidth, frameHeight, 28, 28);

				Color accent = characterType != null ? characterType.getAccentColor() : COLOR_NEUTRAL;
				int alpha = active ? 90 : 40;
				g2d.setColor(withAlpha(accent, alpha));
				g2d.fillOval(frameX + 10, frameY + 12, frameWidth - 20, frameHeight - 24);

				g2d.setColor(withAlpha(COLOR_NEUTRAL, 170));
				g2d.drawRoundRect(frameX, frameY, frameWidth - 1, frameHeight - 1, 28, 28);
			}

			if (characterImage != null) {
				int inset = 16;
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
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
					g2d.drawImage(characterImage, imgX, imgY, imgWidth, imgHeight, null);
					g2d.setComposite(original);
				} else {
					g2d.drawImage(characterImage, imgX, imgY, imgWidth, imgHeight, null);
				}
			}
		}
	}

	private static final class ShadowPanel extends BaseDecoratedPanel {
		private ShadowPanel() {
			setPreferredSize(new Dimension(160, 20));
			setMinimumSize(new Dimension(160, 20));
		}

		@Override
		protected void paintPanel(Graphics2D g2d) {
			int width = getWidth();
			int height = getHeight();
			g2d.setColor(withAlpha(COLOR_DARK, 55));
			g2d.fillOval(20, height / 3, width - 40, height / 3);
		}
	}

	private final class PlayerSlot extends BaseDecoratedPanel {
		private final JLabel statusLabel;
		private final JLabel nameLabel;
		private final JLabel characterLabel;
		private final JLabel descriptionLabel;
		private final AvatarPanel avatarPanel;
		private final ShadowPanel shadowPanel;
		private final JLabel leftArrow;
		private final JLabel rightArrow;
		private boolean isLocalPlayer;
		private boolean hasPlayer;

		private PlayerSlot() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBorder(new EmptyBorder(18, 16, 16, 16));

			statusLabel = new JLabel("準備中", SwingConstants.CENTER);
			statusLabel.setFont(STATUS_FONT);
			statusLabel.setOpaque(true);
			statusLabel.setBackground(withAlpha(COLOR_NEUTRAL, 170));
			statusLabel.setForeground(COLOR_DARK);
			statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			statusLabel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(withAlpha(COLOR_NEUTRAL, 180), 1, true),
					new EmptyBorder(4, 12, 4, 12)
			));
			add(statusLabel);
			add(Box.createVerticalStrut(10));

			JPanel avatarRow = new JPanel();
			avatarRow.setOpaque(false);
			avatarRow.setLayout(new BoxLayout(avatarRow, BoxLayout.X_AXIS));
			leftArrow = createArrowLabel("<");
			rightArrow = createArrowLabel(">");
			leftArrow.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (isLocalPlayer) updateSelectedCharacter(-1);
				}
			});
			rightArrow.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (isLocalPlayer) updateSelectedCharacter(1);
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

			add(Box.createVerticalStrut(8));
			shadowPanel = new ShadowPanel();
			shadowPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
			add(shadowPanel);
			add(Box.createVerticalStrut(10));

			characterLabel = new JLabel(" ", SwingConstants.CENTER);
			characterLabel.setFont(CHARACTER_FONT);
			characterLabel.setForeground(COLOR_DARK);
			characterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			add(characterLabel);
			add(Box.createVerticalStrut(2));

			descriptionLabel = new JLabel(" ", SwingConstants.CENTER);
			descriptionLabel.setFont(DESCRIPTION_FONT);
			descriptionLabel.setForeground(COLOR_MUTED);
			descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			add(descriptionLabel);
			add(Box.createVerticalStrut(10));

			nameLabel = new JLabel("-", SwingConstants.CENTER);
			nameLabel.setFont(NAME_FONT);
			nameLabel.setOpaque(true);
			nameLabel.setBackground(withAlpha(COLOR_NEUTRAL, 170));
			nameLabel.setForeground(COLOR_DARK);
			nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			nameLabel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(withAlpha(COLOR_NEUTRAL, 180), 1, true),
					new EmptyBorder(6, 16, 6, 16)
			));
			add(nameLabel);
		}

		@Override
		protected void paintPanel(Graphics2D g2d) {
			int width = getWidth();
			int height = getHeight();
			Color top = hasPlayer ? withAlpha(COLOR_LIGHT, 240) : withAlpha(COLOR_LIGHT, 210);
			Color bottom = hasPlayer
					? (isLocalPlayer ? withAlpha(COLOR_ACCENT, 120) : withAlpha(COLOR_NEUTRAL, 235))
					: withAlpha(COLOR_NEUTRAL, 210);
			paintShadowedRoundedRect(
					g2d,
					0,
					0,
					width,
					height,
					top,
					bottom,
					isLocalPlayer ? COLOR_ACCENT : COLOR_NEUTRAL
			);
		}

		private JLabel createArrowLabel(String text) {
			JLabel label = new JLabel(text, SwingConstants.CENTER);
			label.setFont(new Font("Meiryo", Font.BOLD, 18));
			label.setForeground(COLOR_MUTED);
			label.setOpaque(true);
			label.setBackground(withAlpha(COLOR_LIGHT, 200));
			label.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(withAlpha(COLOR_NEUTRAL, 180), 1, true),
					new EmptyBorder(2, 8, 2, 8)
			));
			label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			label.setVisible(false);
			return label;
		}

		private void setPlayer(PlayerInfo entry, boolean isLocal) {
			isLocalPlayer = isLocal;
			if (entry == null) {
				hasPlayer = false;
				statusLabel.setText("空き");
				statusLabel.setBackground(withAlpha(COLOR_NEUTRAL, 120));
				nameLabel.setText("-");
				nameLabel.setBackground(withAlpha(COLOR_NEUTRAL, 170));
				avatarPanel.setActive(false);
				characterLabel.setText(" ");
				descriptionLabel.setText(" ");
				avatarPanel.setCharacter(null);
				leftArrow.setVisible(false);
				rightArrow.setVisible(false);
				repaint();
				return;
			}
			hasPlayer = true;
			statusLabel.setText(entry.isReady() ? "準備完了!" : "準備中");
			statusLabel.setBackground(entry.isReady() ? withAlpha(COLOR_ACCENT, 140) : withAlpha(COLOR_NEUTRAL, 170));
			nameLabel.setText(entry.getName());
			nameLabel.setBackground(isLocal ? withAlpha(COLOR_ACCENT, 120) : withAlpha(COLOR_NEUTRAL, 170));
			avatarPanel.setActive(true);

			CharacterType characterType = isLocal ? CharacterType.fromId(selectedCharacterId) : resolveCharacterType(entry);
			characterLabel.setText(characterType.getName());
			descriptionLabel.setText(getCharacterDescription(characterType));
			avatarPanel.setCharacter(characterType);
			leftArrow.setVisible(isLocal);
			rightArrow.setVisible(isLocal);
			repaint();
		}
	}
}
