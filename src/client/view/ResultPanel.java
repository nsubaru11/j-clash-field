package client.view;

import client.model.CharacterSprite;
import model.CharacterType;
import model.GameCharacter;
import model.PlayerInfo;
import model.ResultData;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * ゲーム終了後の結果画面を表示するパネルです。
 */
public class ResultPanel extends BaseBackgroundPanel {
	// --------------- クラス定数 ---------------
	private static final int MAX_PLAYERS = 4;
	private static final Font TITLE_FONT = new Font("Meiryo", Font.BOLD, 28);
	private static final Font RESULT_FONT = new Font("Meiryo", Font.BOLD, 18);
	private static final Font NAME_FONT = new Font("Meiryo", Font.BOLD, 14);
	private static final Font STAT_FONT = new Font("Meiryo", Font.PLAIN, 12);
	private static final Font BUTTON_FONT = new Font("Meiryo", Font.BOLD, 16);
	private static final Color COLOR_LIGHT = new Color(248, 245, 238);
	private static final Color COLOR_NEUTRAL = new Color(220, 212, 202);
	private static final Color COLOR_DARK = new Color(50, 55, 60);
	private static final Color COLOR_MUTED = new Color(100, 105, 110);
	private static final Color COLOR_ACCENT = new Color(120, 170, 210);
	private static final Color COLOR_WIN = new Color(70, 160, 110);
	private static final Color COLOR_LOSE = new Color(140, 90, 90);
	private static final Color COLOR_DRAW = new Color(150, 140, 90);
	private static final Map<CharacterType, BufferedImage> CHARACTER_IMAGES = new EnumMap<>(CharacterType.class);

	static {
		for (CharacterType type : CharacterType.values()) {
			CharacterSprite sprite = CharacterSprite.forType(type);
			BufferedImage idleImage = sprite.getIdleImage();
			CHARACTER_IMAGES.put(type, idleImage);
		}
	}

	// --------------- フィールド ---------------
	private final JLabel titleLabel;
	private final JButton backButton;
	private final PlayerSlot[] slots;

	/**
	 * ResultPanelを構築します。
	 */
	public ResultPanel() {
		setLayout(new GridBagLayout());

		JPanel board = new BoardPanel();
		board.setLayout(new BorderLayout());
		board.setBorder(new EmptyBorder(28, 36, 36, 36));
		board.setPreferredSize(new Dimension(1200, 620));

		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.setBorder(new EmptyBorder(0, 0, 18, 0));

		titleLabel = new JLabel("結果", SwingConstants.LEFT);
		titleLabel.setFont(TITLE_FONT);
		titleLabel.setForeground(COLOR_DARK);
		header.add(titleLabel, BorderLayout.WEST);

		JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		actionPanel.setOpaque(false);
		backButton = createActionButton("ゲームルームへ戻る", withAlpha(COLOR_NEUTRAL, 220), COLOR_NEUTRAL);
		actionPanel.add(backButton);
		header.add(actionPanel, BorderLayout.EAST);

		board.add(header, BorderLayout.NORTH);

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
		add(board);
	}

	// 透明化するヘルパー
	private static Color withAlpha(Color base, int alpha) {
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
	}

	private static BufferedImage resolveCharacterImage(CharacterType characterType) {
		return CHARACTER_IMAGES.get(characterType);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setColor(new Color(0, 0, 0, 90));
		g2d.fillRect(0, 0, getWidth(), getHeight());
	}

	public void setBackAction(ActionListener listener) {
		backButton.addActionListener(listener);
	}

	public void reset() {
		SwingUtilities.invokeLater(() -> {
			for (PlayerSlot slot : slots) {
				slot.setResult(null, null, false);
			}
		});
	}

	public void setResults(Map<Integer, PlayerInfo> playerSnapshots, List<ResultData> results, int localPlayerId) {
		SwingUtilities.invokeLater(() -> {
			List<ResultData> sorted = new ArrayList<>();
			if (results != null) sorted.addAll(results);
			sorted.sort(Comparator.comparingInt(ResultData::getId));

			for (int i = 0; i < slots.length; i++) {
				ResultData data = i < sorted.size() ? sorted.get(i) : null;
				PlayerInfo info = data != null && playerSnapshots != null ? playerSnapshots.get(data.getId()) : null;
				boolean isLocal = data != null && data.getId() == localPlayerId;
				slots[i].setResult(info, data, isLocal);
			}
		});
	}

	private JButton createActionButton(String text, Color background, Color border) {
		JButton button = new JButton(text);
		button.setFont(BUTTON_FONT);
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
			setPreferredSize(new Dimension(140, 170));
			setMinimumSize(new Dimension(140, 170));
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
				g2d.fillRoundRect(frameX, frameY, frameWidth, frameHeight, 24, 24);

				Color accent = characterType != null ? characterType.getAccentColor() : COLOR_NEUTRAL;
				int alpha = active ? 90 : 40;
				g2d.setColor(withAlpha(accent, alpha));
				g2d.fillOval(frameX + 10, frameY + 12, frameWidth - 20, frameHeight - 24);

				g2d.setColor(withAlpha(COLOR_NEUTRAL, 170));
				g2d.drawRoundRect(frameX, frameY, frameWidth - 1, frameHeight - 1, 24, 24);
			}

			if (characterImage != null) {
				int inset = 14;
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

	private final class PlayerSlot extends BaseDecoratedPanel {
		private final JLabel resultLabel;
		private final JLabel nameLabel;
		private final JLabel killsLabel;
		private final JLabel deathsLabel;
		private final JLabel damageGivenLabel;
		private final JLabel damageTakenLabel;
		private final AvatarPanel avatarPanel;
		private boolean isLocalPlayer;
		private boolean hasPlayer;

		private PlayerSlot() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBorder(new EmptyBorder(18, 16, 16, 16));

			resultLabel = new JLabel("-", SwingConstants.CENTER);
			resultLabel.setFont(RESULT_FONT);
			resultLabel.setForeground(COLOR_DARK);
			resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			add(resultLabel);
			add(Box.createVerticalStrut(12));

			avatarPanel = new AvatarPanel();
			avatarPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
			add(avatarPanel);
			add(Box.createVerticalStrut(12));

			nameLabel = new JLabel("-", SwingConstants.CENTER);
			nameLabel.setFont(NAME_FONT);
			nameLabel.setForeground(COLOR_DARK);
			nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			add(nameLabel);
			add(Box.createVerticalStrut(10));

			JPanel statsPanel = new JPanel();
			statsPanel.setOpaque(false);
			statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
			statsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

			killsLabel = createStatLabel("Kills: -");
			deathsLabel = createStatLabel("Deaths: -");
			damageGivenLabel = createStatLabel("Damage Given: -");
			damageTakenLabel = createStatLabel("Damage Taken: -");

			statsPanel.add(killsLabel);
			statsPanel.add(Box.createVerticalStrut(4));
			statsPanel.add(deathsLabel);
			statsPanel.add(Box.createVerticalStrut(4));
			statsPanel.add(damageGivenLabel);
			statsPanel.add(Box.createVerticalStrut(4));
			statsPanel.add(damageTakenLabel);
			add(statsPanel);
		}

		private JLabel createStatLabel(String text) {
			JLabel label = new JLabel(text, SwingConstants.CENTER);
			label.setFont(STAT_FONT);
			label.setForeground(COLOR_MUTED);
			label.setAlignmentX(Component.CENTER_ALIGNMENT);
			return label;
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

		private void setResult(PlayerInfo info, ResultData data, boolean isLocal) {
			isLocalPlayer = isLocal;
			if (info == null && data == null) {
				hasPlayer = false;
				resultLabel.setText("-");
				resultLabel.setForeground(COLOR_DARK);
				nameLabel.setText("-");
				killsLabel.setText("Kills: -");
				deathsLabel.setText("Deaths: -");
				damageGivenLabel.setText("Damage Given: -");
				damageTakenLabel.setText("Damage Taken: -");
				avatarPanel.setActive(false);
				avatarPanel.setCharacter(null);
				repaint();
				return;
			}
			hasPlayer = true;
			String name = info != null ? info.getName() : "";
			if (info == null && data != null) {
				nameLabel.setText("player:" + data.getId());
			} else {
				nameLabel.setText("player: " + (name.isEmpty() ? "-" : name));
			}

			ResultData.ResultType resultType = data != null ? data.getResult() : ResultData.ResultType.LOSE;
			resultLabel.setText(resultType.getLabel());
			resultLabel.setForeground(resolveResultColor(resultType));

			int kills = data != null ? data.getKills() : 0;
			int deaths = data != null ? data.getDeaths() : 0;
			long given = data != null ? Math.round(data.getDamageGiven()) : 0;
			long taken = data != null ? Math.round(data.getDamageTaken()) : 0;

			killsLabel.setText("Kills: " + kills);
			deathsLabel.setText("Deaths: " + deaths);
			damageGivenLabel.setText("Damage Given: " + given);
			damageTakenLabel.setText("Damage Taken: " + taken);

			CharacterType characterType = resolveCharacterType(info);
			avatarPanel.setActive(true);
			avatarPanel.setCharacter(characterType);
			repaint();
		}

		private Color resolveResultColor(ResultData.ResultType resultType) {
			if (resultType == ResultData.ResultType.WIN) return COLOR_WIN;
			if (resultType == ResultData.ResultType.DRAW) return COLOR_DRAW;
			return COLOR_LOSE;
		}

		private CharacterType resolveCharacterType(PlayerInfo info) {
			if (info == null) return CharacterType.defaultType();
			GameCharacter character = info.getCharacter();
			return character != null ? character.getType() : CharacterType.defaultType();
		}
	}
}
