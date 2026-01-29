package server.controller;

import model.CharacterType;
import model.GameCharacter;
import model.PlayerInfo;
import model.ResultData;
import network.Protocol;
import server.model.Archer;
import server.model.BattleField;
import server.model.Fighter;
import server.model.GameSession;
import server.model.Projectile;
import server.model.Warrior;
import server.model.Wizard;

import java.io.Closeable;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * ゲームルームのクラスです。
 */
class GameRoom extends Thread implements Closeable {
	// -------------------- クラス定数 --------------------
	private static final Logger logger = Logger.getLogger(GameRoom.class.getName());
	private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);
	private static final int MAX_PLAYERS = 4;
	private static final int FPS = 60;
	private static final long FRAME_TIME = 1000_000_000L / FPS;

	// -------------------- インスタンス定数 --------------------
	private final int roomId;
	private final ConcurrentLinkedQueue<ServerCommand> commandQueue;
	private final ConcurrentHashMap<ClientHandler, PlayerInfo> playerMap;
	private final GameSession gameSession;

	// -------------------- インスタンス変数 --------------------
	private volatile Runnable disconnectListener;
	private final boolean isPublic;
	private volatile boolean isClosed;

	public GameRoom(final boolean isPublic) {
		roomId = ID_GENERATOR.incrementAndGet();
		commandQueue = new ConcurrentLinkedQueue<>();
		playerMap = new ConcurrentHashMap<>(MAX_PLAYERS);
		gameSession = new GameSession(MAX_PLAYERS);
		this.isPublic = isPublic;
		isClosed = false;
	}

	public void run() {
		long targetTime = System.nanoTime();
		while (!isClosed) {
			targetTime += FRAME_TIME;
			while (!commandQueue.isEmpty()) {
				ServerCommand cmd = commandQueue.poll();
				handleCommand(cmd);
			}
			if (gameSession.isStarted() && gameSession.getBattleField() != null) {
				BattleField.UpdateResult result = gameSession.update();
				broadcastState(result);
				sendResultIfReady();
			}
			long waitNs = targetTime - System.nanoTime();
			if (waitNs > 0) {
				long waitMs = waitNs / 1_000_000;
				int waitNsRest = (int) (waitNs % 1_000_000);
				try {
					// noinspection BusyWait
					Thread.sleep(waitMs, waitNsRest);
				} catch (InterruptedException e) {
					logger.warning("GameRoom interrupted: " + roomId);
					close();
					break;
				}
			} else {
				logger.fine("処理落ち発生: " + waitNs + "ns");
				targetTime -= waitNs;
			}
		}
	}

	public void close() {
		synchronized (this) {
			if (isClosed) return;
			isClosed = true;
			logger.info("ルーム(ID: " + roomId + ")を閉鎖します。全プレイヤーに通知中...");
			playerMap.keySet().forEach(handler -> {
				handler.sendMessage(Protocol.gameRoomClosed());
				handler.close();
			});
			playerMap.clear();
		}
		if (disconnectListener != null) disconnectListener.run();
	}

	/**
	 * 送信するデータ
	 * 開始済み：プレイヤー数:フィールド状態
	 * 未開始：ルームID,公開フラグ,プレイヤー数:プレイヤー情報1, ...,プレイヤー情報n
	 * プレイヤー情報：プレイヤーID,プレイヤー名,キャラクター名
	 */
	public synchronized String toString() {
		if (isClosed) return "ルーム(ID: " + roomId + ")は閉鎖されています。";
		StringJoiner sj = new StringJoiner(":");
		if (gameSession.isStarted()) {
			BattleField field = gameSession.getBattleField();
			sj.add(playerMap.size() + "").add(field != null ? field.toString() : "");
		} else {
			sj.add(roomId + "," + isPublic + "," + playerMap.size());
			sj.add(playerMap.values().stream().map(this::formatPlayerEntry).collect(Collectors.joining(",")));
		}
		return sj.toString();
	}

	/** 一意識別用 */
	public synchronized boolean equals(Object obj) {
		if (!(obj instanceof GameRoom)) return false;
		return ((GameRoom) obj).roomId == roomId;
	}

	/** 一意識別用 */
	public synchronized int hashCode() {
		return roomId;
	}

	public synchronized int getRoomId() {
		return roomId;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public synchronized boolean join(final ClientHandler handler, final String playerName) {
		if (isClosed || gameSession.isStarted() || playerMap.size() >= MAX_PLAYERS) {
			logger.warning(() -> "ルーム(ID: " + roomId + ")は既に満員です。");
			return false;
		}
		handler.setMessageListener(msg -> this.commandQueue.add(new ServerCommand(handler, msg)));
		handler.setDisconnectListener(() -> handleDisconnect(handler));
		PlayerInfo newPlayer = new PlayerInfo(handler.getConnectionId(), playerName, false, new Archer());
		playerMap.put(handler, newPlayer);
		String joinSuccess = Protocol.joinSuccess(newPlayer.getId(), toString());
		String joinOpponent = Protocol.joinOpponent(newPlayer.getId(), newPlayer.getName());
		playerMap.keySet().forEach(clientHandler -> clientHandler.sendMessage(clientHandler != handler ? joinOpponent : joinSuccess));
		logger.info("ルーム(ID: " + roomId + ")にプレイヤー(ID: " + handler.getConnectionId() + ")を追加しました");
		return true;
	}

	public synchronized void setDisconnectListener(Runnable listener) {
		this.disconnectListener = listener;
	}

	private synchronized void handleCommand(ServerCommand command) {
		ClientHandler sender = command.getSender();
		PlayerInfo player = playerMap.get(sender);
		if (player == null) return;
		String body = command.getBody();
		switch (command.getCommandType()) {
			case READY:
				if (gameSession.isStarted()) break;
				int characterId = Integer.parseInt(body);
				GameCharacter character;
				switch (CharacterType.fromId(characterId)) {
					case ARCHER:
						character = new Archer();
						break;
					case FIGHTER:
						character = new Fighter();
						break;
					case WARRIOR:
						character = new Warrior();
						break;
					case WIZARD:
						character = new Wizard();
						break;
					default:
						character = new Archer();
						break;
				}
				player.setCharacter(character);
				player.setReady(true);
				String readyMessage = Protocol.readySuccess(player.getId(), characterId);
				playerMap.keySet().forEach(handler -> handler.sendMessage(readyMessage));
				logger.fine(() -> "プレイヤー(ID: " + sender.getConnectionId() + ")が準備完了です。");
				startGame();
				break;
			case UNREADY:
				if (gameSession.isStarted()) break;
				player.setReady(false);
				logger.fine(() -> "プレイヤー(ID: " + sender.getConnectionId() + ")が準備を解除しました。");
				String unreadyMessage = Protocol.unreadySuccess(player.getId());
				playerMap.keySet().forEach(handler -> handler.sendMessage(unreadyMessage));
				break;
			case MOVE_LEFT:
				broadcastGameAction(gameSession.handleAction(GameSession.ActionType.MOVE_LEFT, player), player.getId());
				break;
			case MOVE_UP:
			case JUMP:
				broadcastGameAction(gameSession.handleAction(GameSession.ActionType.MOVE_UP, player), player.getId());
				break;
			case MOVE_RIGHT:
				broadcastGameAction(gameSession.handleAction(GameSession.ActionType.MOVE_RIGHT, player), player.getId());
				break;
			case MOVE_DOWN:
				broadcastGameAction(gameSession.handleAction(GameSession.ActionType.MOVE_DOWN, player), player.getId());
				break;
			case CHARGE_START:
				broadcastGameAction(gameSession.handleAction(GameSession.ActionType.CHARGE_START, player), player.getId());
				break;
			case NORMAL_ATTACK:
				broadcastGameAction(gameSession.handleAction(GameSession.ActionType.NORMAL_ATTACK, player), player.getId());
				break;
			case CHARGE_ATTACK:
				broadcastGameAction(gameSession.handleAction(GameSession.ActionType.CHARGE_ATTACK, player), player.getId());
				break;
			case DEFEND:
				broadcastGameAction(gameSession.handleAction(GameSession.ActionType.DEFEND, player), player.getId());
				break;
			case RESIGN:
				handleResign(sender);
				break;
			case DISCONNECT:
				handleDisconnect(sender);
				break;
			default:
				break;
		}
	}

	private synchronized void startGame() {
		if (isClosed || gameSession.isStarted()) return;
		if (playerMap.size() < 2) return;
		for (PlayerInfo player : playerMap.values()) {
			if (!player.isReady()) return;
		}
		logger.info("ルーム(ID: " + roomId + ")でゲーム開始");
		gameSession.start(playerMap.values());
		String startMessage = Protocol.gameStart();
		playerMap.keySet().forEach(handler -> handler.sendMessage(startMessage));
	}

	private synchronized void handleResign(ClientHandler resigner) {
		// TODO: プレイヤーが降参した場合の処理
		// 降参したプレイヤーは観戦モードにする。
		// 残り一人になったら終わりとする。
		logger.info("ルーム(ID: " + roomId + ")でプレイヤー(ID: " + resigner.getConnectionId() + ")が降参しました。");
		PlayerInfo player = playerMap.get(resigner);
		int playerId = player != null ? player.getId() : resigner.getConnectionId();
		if (gameSession.eliminatePlayer(playerId, true)) {
			String resignMessage = Protocol.opponentResigned();
			playerMap.keySet().forEach(handler -> {
				if (handler != resigner) handler.sendMessage(resignMessage);
			});
			sendResultIfReady();
		}
	}

	private void handleDisconnect(ClientHandler handler) {
		// TODO: プレイヤーが切断した場合の処理
		// 接続が切れたプレイヤーはゲームルームからも追い出す。
		synchronized (this) {
			logger.info("ルーム(ID: " + roomId + ") でプレイヤー(ID: " + handler.getConnectionId() + ")切断しました。");
			PlayerInfo removedPlayer = playerMap.remove(handler);
			int removedPlayerId = removedPlayer != null ? removedPlayer.getId() : handler.getConnectionId();
			String disconnectMessage = Protocol.opponentDisconnected(removedPlayerId);
			playerMap.keySet().forEach(h -> h.sendMessage(disconnectMessage));
			handler.close();
			if (gameSession.isStarted()) {
				if (gameSession.eliminatePlayer(removedPlayerId, true)) {
					sendResultIfReady();
				}
			}
		}
		if (playerMap.isEmpty()) close();
	}

	private synchronized void broadcastState(BattleField.UpdateResult result) {
		// TODO: 状態を全員に通知する
		BattleField field = gameSession.getBattleField();
		if (!gameSession.isStarted() || field == null) return;

		playerMap.forEach((handler, player) -> {
			GameCharacter character = player.getCharacter();
			if (character != null && character.getPosition() != null) {
				String msg = Protocol.move(player.getId(), character.getPosition().getX(), character.getPosition().getY());
				playerMap.keySet().forEach(h -> h.sendMessage(msg));
			}
		});

		for (Projectile projectile : field.getProjectiles()) {
			if (projectile.getPosition() == null) continue;
			String msg = Protocol.projectile(projectile.getId(), projectile.getType(),
					projectile.getPosition().getX(), projectile.getPosition().getY(), projectile.getPower());
			playerMap.keySet().forEach(h -> h.sendMessage(msg));
		}

		if (result != null) {
			for (Projectile projectile : result.getRemovedProjectiles()) {
				String msg = Protocol.projectileRemove(projectile.getId());
				playerMap.keySet().forEach(h -> h.sendMessage(msg));
			}
			for (BattleField.DamageEvent damage : result.getDamageEvents()) {
				String msg = Protocol.damage(damage.getTargetId(), damage.getHp());
				playerMap.keySet().forEach(h -> h.sendMessage(msg));
			}
		}
	}

	private void broadcastAction(String message) {
		if (message == null || message.isEmpty()) return;
		playerMap.keySet().forEach(h -> h.sendMessage(message));
	}

	private void broadcastGameAction(GameSession.BroadcastAction action, int playerId) {
		if (action == null) return;
		String msg;
		switch (action) {
			case MOVE_UP:
				msg = Protocol.moveUp(playerId);
				break;
			case NORMAL_ATTACK:
				msg = Protocol.normalAttack(playerId);
				break;
			case CHARGE_START:
				msg = Protocol.chargeStart(playerId);
				break;
			case CHARGE_ATTACK:
				msg = Protocol.chargeAttack(playerId);
				break;
			case DEFEND:
				msg = Protocol.defend(playerId);
				break;
			default:
				msg = "";
				break;
		}
		broadcastAction(msg);
	}

	private void sendResultIfReady() {
		List<ResultData> results = gameSession.consumeResults();
		if (results == null || results.isEmpty()) return;
		String payload = ResultData.serializeList(results);
		if (payload.isEmpty()) return;
		String msg = Protocol.result(payload);
		playerMap.keySet().forEach(handler -> handler.sendMessage(msg));
	}

	private String formatPlayerEntry(PlayerInfo info) {
		GameCharacter character = info.getCharacter();
		CharacterType type = character != null ? character.getType() : CharacterType.defaultType();
		return info.getId() + " " + info.getName() + " " + info.isReady() + " " + type.getId();
	}
}
