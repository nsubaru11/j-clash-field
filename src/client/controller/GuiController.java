package client.controller;

import client.model.GameCharacterClient;
import client.view.GameGUI;
import client.view.GamePanel;
import client.view.GameRoomPanel;
import client.view.HomePanel;
import client.view.LoadPanel;
import client.view.MatchConfigPanel;
import client.view.ResultPanel;
import client.view.TitlePanel;
import model.CharacterType;
import model.GameCharacter;
import model.PlayerInfo;
import model.ProjectileType;
import model.ResultData;
import network.Command;
import network.CommandType;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public final class GuiController {
	// -------------------- クラス定数 --------------------
	private static final Logger logger = Logger.getLogger(GuiController.class.getName());
	private static final String CARD_TITLE = "title";
	private static final String CARD_LOAD = "load";
	private static final String CARD_HOME = "home";
	private static final String CARD_GAME_ROOM = "game_room";
	private static final String CARD_GAME = "game";
	private static final String CARD_RESULT = "result";

	// -------------------- インスタンス定数 --------------------
	private final JLayeredPane rootPane; // ルートパネル
	private final CardLayout cardLayout; // カードレイアウト
	private final JPanel cardPanel; // 各画面パネルを保持する親パネル
	private final TitlePanel titlePanel;
	private final LoadPanel loadPanel;
	private final HomePanel homePanel;
	private final MatchConfigPanel matchConfigPanel;
	private final GameRoomPanel gameRoomPanel;
	private final GamePanel gamePanel;
	private final ResultPanel resultPanel;
	private final Map<Integer, PlayerInfo> playerSnapshots = new LinkedHashMap<>(4, 1.0f);
	private final NetworkController network;
	private final ConcurrentLinkedQueue<Command> commandQueue;
	private volatile MatchConfigPanel.MatchMode lastMatchMode = MatchConfigPanel.MatchMode.RANDOM;
	private volatile int playerId = 0;

	public GuiController(NetworkController network) {
		this.network = network;
		cardLayout = new CardLayout();
		cardPanel = new JPanel(cardLayout);

		titlePanel = new TitlePanel(this::startConnection);
		loadPanel = new LoadPanel();
		homePanel = new HomePanel(this::showMatchConfig, e -> {
			int result = JOptionPane.showConfirmDialog(cardPanel, "終了します。よろしいですか？", "終了確認", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				network.setDisconnectListener(null);
				network.disconnect();
				System.exit(0);
			}
		});

		matchConfigPanel = new MatchConfigPanel();
		matchConfigPanel.setVisible(false);
		matchConfigPanel.setStartGameListener(e -> joinRoom());
		matchConfigPanel.setCancelListener(e -> showHome());
		gameRoomPanel = new GameRoomPanel();
		gameRoomPanel.setBackAction(e -> {
			int result = JOptionPane.showConfirmDialog(cardPanel,
					"接続を切断し、ホームに戻ります。よろしいですか？",
					"確認",
					JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				network.setDisconnectListener(null);
				network.disconnect();
				gameRoomPanel.removePlayer(playerId);
				showHome();
			}
		});
		gameRoomPanel.setReadyAction(e -> {
			if (gameRoomPanel.isReady()) {
				network.unready();
			} else {
				network.ready(gameRoomPanel.getSelectedCharacterType());
			}
		});

		gamePanel = new GamePanel();
		gamePanel.setInputActions(
				network::moveLeft,
				network::moveRight,
				network::moveUp,
				network::moveDown,
				network::normalAttack,
				network::chargeStart,
				network::chargeAttack,
				network::defend,
				network::resign
		);

		resultPanel = new ResultPanel();
		resultPanel.setBackAction(e -> showGameRoom());

		rootPane = new JLayeredPane();
		rootPane.setLayout(new OverlayLayout(rootPane));
		rootPane.add(cardPanel, JLayeredPane.DEFAULT_LAYER);
		rootPane.add(matchConfigPanel, JLayeredPane.PALETTE_LAYER);
		rootPane.add(loadPanel, JLayeredPane.MODAL_LAYER);

		cardPanel.add(titlePanel, CARD_TITLE);
		cardPanel.add(homePanel, CARD_HOME);
		cardPanel.add(gameRoomPanel, CARD_GAME_ROOM);
		cardPanel.add(gamePanel, CARD_GAME);
		cardPanel.add(resultPanel, CARD_RESULT);

		GameGUI gui = new GameGUI(rootPane);
		gui.setVisible(true);
		commandQueue = new ConcurrentLinkedQueue<>();
	}

	private void startConnection() {
		Runtime.getRuntime().addShutdownHook(new Thread(network::close));
		showLoad();
		network.connect(msg -> this.commandQueue.add(new Command(msg)), this::handleServerDisconnect, () -> {
			new GameLoopThread().start();
			loadPanel.setNextScreen(this::showHome);
			completeLoad();
		}, () -> SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(cardPanel, "サーバーに接続できませんでした。", "接続エラー", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}));
	}

	private synchronized void joinRoom() {
		String userName = matchConfigPanel.getUserName();
		MatchConfigPanel.MatchMode mode = matchConfigPanel.getCurrentMode();
		matchConfigPanel.setVisible(false);
		gameRoomPanel.reset();
		showLoad();
		loadPanel.setNextScreen(this::showGameRoom);
		Runnable sendJoin = () -> {
			switch (mode) {
				case CREATE:
					network.createRoom(userName);
					break;
				case JOIN:
					network.joinRoom(userName, matchConfigPanel.getRoomId());
					break;
				case RANDOM:
				default:
					network.joinRoom(userName, -1);
					break;
			}
		};
		if (network.isConnected()) {
			sendJoin.run();
		} else {
			network.connect(msg -> this.commandQueue.add(new Command(msg)), this::handleServerDisconnect, sendJoin, () -> SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(cardPanel, "サーバーに接続できませんでした。", "接続エラー", JOptionPane.ERROR_MESSAGE);
				loadPanel.setNextScreen(() -> showMatchConfig(lastMatchMode));
				completeLoad();
			}));
		}
	}

	private void showHome() {
		SwingUtilities.invokeLater(() -> {
			gameRoomPanel.reset();
			matchConfigPanel.reset();
			matchConfigPanel.setVisible(false);
			cardLayout.show(cardPanel, CARD_HOME);
		});
	}

	private void showLoad() {
		loadPanel.startLoading();
	}

	private void showMatchConfig(MatchConfigPanel.MatchMode mode) {
		lastMatchMode = mode;
		SwingUtilities.invokeLater(() -> {
			matchConfigPanel.setupForMode(mode);
			matchConfigPanel.reset();
			matchConfigPanel.setVisible(true);
			matchConfigPanel.requestFocusInWindow();
		});
	}

	private void showGameRoom() {
		SwingUtilities.invokeLater(() -> cardLayout.show(cardPanel, CARD_GAME_ROOM));
	}

	private void showGame() {
		resetSnapshotsForNewMatch();
		SwingUtilities.invokeLater(() -> {
			resultPanel.reset();
			gamePanel.clearPlayers();
			for (PlayerInfo snapshot : playerSnapshots.values()) {
				gamePanel.setPlayerInfo(snapshot);
			}
			gamePanel.setLocalPlayerId(playerId);
			cardLayout.show(cardPanel, CARD_GAME);
			gamePanel.requestFocusInWindow();
		});
	}

	private void showResultWithData(List<ResultData> results) {
		SwingUtilities.invokeLater(() -> {
			resultPanel.setResults(playerSnapshots, results, playerId);
			cardLayout.show(cardPanel, CARD_RESULT);
		});
	}

	private void completeLoad() {
		loadPanel.completeLoading();
	}

	private void handleCommand(Command command) {
		// TODO: コマンド処理
		String body = command.getBody();
		switch (command.getCommandType()) {
			case GAME_START:
				showGame();
				break;
			case GAME_OVER:
				break;
			case MOVE:
				handleMove(body);
				break;
			case MOVE_UP:
			case NORMAL_ATTACK:
			case CHARGE_START:
			case CHARGE_ATTACK:
			case DEFEND:
				handlePlayerAction(command.getCommandType(), body);
				break;
			case PROJECTILE:
				handleProjectile(body);
				break;
			case PROJECTILE_REMOVE:
				handleProjectileRemove(body);
				break;
			case DAMAGE:
				handleDamage(body);
				break;
			case DEAD:
				break;
			case OPPONENT_RESIGNED:
				break;
			case OPPONENT_DISCONNECTED:
				gameRoomPanel.removePlayer(Integer.parseInt(body));
				break;
			case JOIN_SUCCESS:
				handleJoinSuccess(body);
				break;
			case JOIN_FAILED:
				handleJoinFailed();
				break;
			case JOIN_OPPONENT:
				handleJoinOpponent(body);
				break;
			case READY_SUCCESS:
				String[] playerInfo = body.split(",");
				int playerId = Integer.parseInt(playerInfo[0]);
				int characterId = Integer.parseInt(playerInfo[1]);
				gameRoomPanel.setReady(playerId, characterId);
				updatePlayerSnapshot(playerId, null, CharacterType.fromId(characterId));
				break;
			case UNREADY_SUCCESS:
				gameRoomPanel.setUnready(Integer.parseInt(body));
				break;
			case RESULT:
				showResultWithData(ResultData.parseList(body));
				break;
			case GAME_ROOM_CLOSED:
			case SERVER_CLOSED:
				handleServerDisconnect();
				break;
			default:
				break;
		}
	}

	private void handleServerDisconnect() {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(cardPanel,
					"サーバーから切断されました。",
					"通信エラー",
					JOptionPane.ERROR_MESSAGE);
			gameRoomPanel.reset();
			matchConfigPanel.reset();
			matchConfigPanel.setVisible(false);
			cardLayout.show(cardPanel, CARD_HOME);
		});
	}

	private void handleJoinSuccess(String body) {
		String[] parts = body.split(":", 2);
		int joinedPlayerId = Integer.parseInt(parts[0]);
		this.playerId = joinedPlayerId;
		playerSnapshots.clear();
		String roomState = parts[1];
		String[] roomStateParts = roomState.split(":", 2);
		String roomInfoPart = roomStateParts[0];
		String playerInfoPart = roomStateParts[1]; // 自分が参加成功した時点でサーバーから送られてくるメッセージには自分の情報が入っている

		// 部屋情報
		String[] roomInfo = roomInfoPart.split(",");
		int roomId = Integer.parseInt(roomInfo[0]);
		boolean isPublic = Boolean.parseBoolean(roomInfo[1]);

		// プレイヤー情報
		String[] playerEntries = playerInfoPart.split(",");
		for (String playerInfo : playerEntries) {
			String[] fields = playerInfo.split("\\s");
			int playerId = Integer.parseInt(fields[0]);
			String playerName = fields[1];
			boolean isReady = Boolean.parseBoolean(fields[2]);
			int id = Integer.parseInt(fields[3]);
			CharacterType characterType = CharacterType.fromId(id);
			gameRoomPanel.addPlayer(playerId, playerName, isReady, characterType);
			updatePlayerSnapshot(playerId, playerName, characterType);
		}

		gameRoomPanel.setLocalPlayerId(joinedPlayerId);
		gameRoomPanel.setRoomInfo(roomId, isPublic);
		loadPanel.setNextScreen(this::showGameRoom);
		completeLoad();
	}

	private void handleJoinFailed() {
		SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(cardPanel,
				"ルームに参加できませんでした。",
				"参加エラー",
				JOptionPane.WARNING_MESSAGE));
		loadPanel.setNextScreen(() -> showMatchConfig(lastMatchMode));
		completeLoad();
	}

	private void handleJoinOpponent(String body) {
		String[] fields = body.split(",", 2);
		int playerId = Integer.parseInt(fields[0]);
		String playerName = fields[1];
		gameRoomPanel.addPlayer(playerId, playerName, false, CharacterType.defaultType());
		updatePlayerSnapshot(playerId, playerName, CharacterType.defaultType());
	}

	private void handleMove(String body) {
		String[] parts = body.split(":", 2);
		int movedPlayerId = Integer.parseInt(parts[0]);
		String[] coords = parts[1].split(",", 4);
		double x = Double.parseDouble(coords[0]);
		double y = Double.parseDouble(coords[1]);
		double facingX = Double.parseDouble(coords[2]);
		double facingY = Double.parseDouble(coords[3]);
		SwingUtilities.invokeLater(() -> gamePanel.updatePlayerPosition(movedPlayerId, x, y, facingX, facingY));
	}

	private void handlePlayerAction(CommandType actionType, String body) {
		int actedPlayerId = Integer.parseInt(body);
		GameCharacterClient.Action action;
		switch (actionType) {
			case NORMAL_ATTACK:
				action = GameCharacterClient.Action.NORMAL_ATTACK;
				break;
			case CHARGE_START:
				action = GameCharacterClient.Action.CHARGE_HOLD;
				break;
			case CHARGE_ATTACK:
				action = GameCharacterClient.Action.CHARGE_ATTACK;
				break;
			case DEFEND:
				action = GameCharacterClient.Action.DEFEND;
				break;
			case MOVE_UP:
				action = GameCharacterClient.Action.JUMP;
				break;
			default:
				action = GameCharacterClient.Action.NONE;
				break;
		}
		if (action == GameCharacterClient.Action.NONE) return;
		SwingUtilities.invokeLater(() -> gamePanel.recordPlayerAction(actedPlayerId, action));
	}

	private void handleProjectile(String body) {
		String[] parts = body.split(",", 7);
		long projectileId = Long.parseLong(parts[0]);
		int typeId = Integer.parseInt(parts[1]);
		double x = Double.parseDouble(parts[2]);
		double y = Double.parseDouble(parts[3]);
		double power = Double.parseDouble(parts[4]);
		double vx = Double.parseDouble(parts[5]);
		double vy = Double.parseDouble(parts[6]);
		ProjectileType type = ProjectileType.fromId(typeId);
		SwingUtilities.invokeLater(() -> gamePanel.updateProjectile(projectileId, type, x, y, power, vx, vy));
	}

	private void handleProjectileRemove(String body) {
		long projectileId = Long.parseLong(body);
		SwingUtilities.invokeLater(() -> gamePanel.removeProjectile(projectileId));
	}

	private void handleDamage(String body) {
		String[] parts = body.split(",", 2);
		int targetId = Integer.parseInt(parts[0]);
		int hp = Integer.parseInt(parts[1]);
		SwingUtilities.invokeLater(() -> gamePanel.updatePlayerHp(targetId, hp));
	}

	/**
	 * playerName/characterType が null の場合は既存値を保持する。
	 */
	private void updatePlayerSnapshot(int playerId, String playerName, CharacterType characterType) {
		PlayerInfo snapshot = playerSnapshots.get(playerId);
		if (snapshot == null) {
			GameCharacter character = characterType != null ? GameCharacterClient.forType(characterType) : null;
			snapshot = new PlayerInfo(playerId, playerName, false, character);
			playerSnapshots.put(playerId, snapshot);
		}
		if (playerName != null) snapshot.setName(playerName);
		if (characterType != null) {
			GameCharacter current = snapshot.getCharacter();
			if (current == null || current.getType() != characterType) {
				snapshot.setCharacter(GameCharacterClient.forType(characterType));
			}
		}
	}

	private void resetSnapshotsForNewMatch() {
		if (playerSnapshots.isEmpty()) return;
		Map<Integer, PlayerInfo> refreshed = new LinkedHashMap<>(playerSnapshots.size(), 1.0f);
		for (PlayerInfo snapshot : playerSnapshots.values()) {
			if (snapshot == null) continue;
			CharacterType type = CharacterType.defaultType();
			GameCharacter existing = snapshot.getCharacter();
			if (existing != null && existing.getType() != null) {
				type = existing.getType();
			}
			GameCharacter character = GameCharacterClient.forType(type);
			PlayerInfo refreshedInfo = new PlayerInfo(snapshot.getId(), snapshot.getName(), snapshot.isReady(), character);
			refreshed.put(refreshedInfo.getId(), refreshedInfo);
		}
		playerSnapshots.clear();
		playerSnapshots.putAll(refreshed);
	}

	private final class GameLoopThread extends Thread {
		private static final int FPS = 60;
		private static final long FRAME_TIME = 1000_000_000L / FPS;

		@Override
		public void run() {
			long targetTime = System.nanoTime();
			while (true) {
				targetTime += FRAME_TIME;
				while (!commandQueue.isEmpty()) {
					Command cmd = commandQueue.poll();
					handleCommand(cmd);
				}
				long waitNs = targetTime - System.nanoTime();
				if (waitNs > 0) {
					long waitMs = waitNs / 1_000_000;
					int waitNsRest = (int) (waitNs % 1_000_000);
					try {
						// noinspection BusyWait
						Thread.sleep(waitMs, waitNsRest);
					} catch (InterruptedException e) {
						logger.warning("");
						break;
					}
				} else {
					logger.fine("処理落ち発生: " + waitNs + "ns");
					targetTime -= waitNs;
				}
			}
		}
	}

}
