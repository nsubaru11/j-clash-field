package server.controller;

import model.CharacterType;
import model.GameCharacter;
import model.PlayerInfo;
import model.ProjectileType;
import network.Protocol;
import server.model.Archer;
import server.model.AttackHitbox;
import server.model.BattleField;
import server.model.Fighter;
import server.model.Projectile;
import server.model.Warrior;
import server.model.Wizard;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Comparator;
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
	private final ConcurrentHashMap<Integer, Integer> facingDirections = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, Long> chargeStartTimes = new ConcurrentHashMap<>();

	// -------------------- インスタンス変数 --------------------
	private volatile Runnable disconnectListener;
	private final boolean isPublic;
	private volatile boolean isStarted;
	private volatile boolean isClosed;
	private volatile boolean isGameOver;
	private volatile int alivePlayers;
	private volatile BattleField battleField;

	public GameRoom(final boolean isPublic) {
		roomId = ID_GENERATOR.incrementAndGet();
		commandQueue = new ConcurrentLinkedQueue<>();
		playerMap = new ConcurrentHashMap<>(MAX_PLAYERS);
		this.isPublic = isPublic;
		isStarted = false;
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
			if (isStarted && battleField != null) {
				BattleField.UpdateResult result = battleField.update();
				broadcastState(result);
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
		if (isStarted) {
			sj.add(playerMap.size() + "").add(battleField.toString());
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
		if (isClosed || isStarted || playerMap.size() >= MAX_PLAYERS) {
			logger.warning(() -> "ルーム(ID: " + roomId + ")は既に満員です。");
			return false;
		}
		handler.setMessageListener(msg -> this.commandQueue.add(new ServerCommand(handler, msg)));
		handler.setDisconnectListener(() -> handleDisconnect(handler));
		PlayerInfo newPlayer = new PlayerInfo(handler.getConnectionId(), playerName, false, new Archer());
		playerMap.put(handler, newPlayer);
		facingDirections.put(newPlayer.getId(), 1);
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
				player.setReady(false);
				logger.fine(() -> "プレイヤー(ID: " + sender.getConnectionId() + ")が準備を解除しました。");
				String unreadyMessage = Protocol.unreadySuccess(player.getId());
				playerMap.keySet().forEach(handler -> handler.sendMessage(unreadyMessage));
				break;
			case MOVE_LEFT:
				setFacingDirection(player, -1);
				applyMove(player, -resolveMoveStepX(player), 0);
				break;
			case MOVE_UP:
			case JUMP:
				if (applyJump(player)) {
					broadcastAction(Protocol.moveUp(player.getId()));
				}
				break;
			case MOVE_RIGHT:
				setFacingDirection(player, 1);
				applyMove(player, resolveMoveStepX(player), 0);
				break;
			case MOVE_DOWN:
				applyMove(player, 0, -resolveMoveStepY(player));
				break;
			case CHARGE_START:
				startCharge(player);
				broadcastAction(Protocol.chargeStart(player.getId()));
				break;
			case NORMAL_ATTACK:
				applyNormalAttack(player);
				broadcastAction(Protocol.normalAttack(player.getId()));
				break;
			case CHARGE_ATTACK:
				applyChargeAttack(player);
				broadcastAction(Protocol.chargeAttack(player.getId()));
				break;
			case DEFEND:
				applyDefend(player);
				broadcastAction(Protocol.defend(player.getId()));
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
		if (isClosed || isStarted) return;
		if (playerMap.size() < 2) return;
		for (PlayerInfo player : playerMap.values()) {
			if (!player.isReady()) return;
		}
		isStarted = true;
		isGameOver = false;
		alivePlayers = playerMap.size();
		logger.info("ルーム(ID: " + roomId + ")でゲーム開始");
		battleField = new BattleField();
		double fieldWidth = battleField.getWidth();
		double groundY = battleField.getGroundY();
		List<PlayerInfo> sortedPlayers = new ArrayList<>(playerMap.values());
		sortedPlayers.sort(Comparator.comparingInt(PlayerInfo::getId));
		int index = 0;
		for (PlayerInfo player : sortedPlayers) {
			GameCharacter character = player.getCharacter();
			if (character != null) {
				double slotCenter = (index + 0.5) / (double) MAX_PLAYERS;
				double x = fieldWidth * slotCenter;
				character.setPosition(x, groundY);
				character.setGrounded(true);
				character.setOwnerId(player.getId());
				battleField.addEntity(character);
			}
			index++;
		}
		String startMessage = Protocol.gameStart();
		playerMap.keySet().forEach(handler -> handler.sendMessage(startMessage));
	}

	private synchronized void endGame() {
		isGameOver = true;
		isStarted = false;
		notifyResult();
		// TODO: ゲーム終了処理
	}

	private synchronized void notifyResult() {
		// TODO: 結果通知処理
	}

	private synchronized void handleResign(ClientHandler resigner) {
		// TODO: プレイヤーが降参した場合の処理
		// 降参したプレイヤーは観戦モードにする。
		// 残り一人になったら終わりとする。
		logger.info("ルーム(ID: " + roomId + ")でプレイヤー(ID: " + resigner.getConnectionId() + ")が降参しました。");
		alivePlayers--;
		if (alivePlayers == 1) {
			endGame();
			logger.info("ルーム(ID: " + roomId + ")で生存しているプレイヤーの人数が1人になったためゲームを終了します。");
		}
	}

	private void handleDisconnect(ClientHandler handler) {
		// TODO: プレイヤーが切断した場合の処理
		// 接続が切れたプレイヤーはゲームルームからも追い出す。
		synchronized (this) {
			logger.info("ルーム(ID: " + roomId + ") でプレイヤー(ID: " + handler.getConnectionId() + ")切断しました。");
			PlayerInfo removedPlayer = playerMap.remove(handler);
			int removedPlayerId = removedPlayer != null ? removedPlayer.getId() : handler.getConnectionId();
			facingDirections.remove(removedPlayerId);
			chargeStartTimes.remove(removedPlayerId);
			String disconnectMessage = Protocol.opponentDisconnected(removedPlayerId);
			playerMap.keySet().forEach(h -> h.sendMessage(disconnectMessage));
			handler.close();
			if (isStarted) {
				alivePlayers--;
				if (alivePlayers == 1) {
					endGame();
					logger.info("ルーム(ID: " + roomId + ")で生存しているプレイヤーの人数が1人になったためゲームを終了します。");
				}
			}
		}
		if (playerMap.isEmpty()) close();
	}

	private synchronized void broadcastState(BattleField.UpdateResult result) {
		// TODO: 状態を全員に通知する
		if (!isStarted || battleField == null) return;

		playerMap.forEach((handler, player) -> {
			GameCharacter character = player.getCharacter();
			if (character != null && character.getPosition() != null) {
				String msg = Protocol.move(player.getId(), character.getPosition().getX(), character.getPosition().getY());
				playerMap.keySet().forEach(h -> h.sendMessage(msg));
			}
		});

		for (Projectile projectile : battleField.getProjectiles()) {
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

	private void applyMove(PlayerInfo player, double dx, double dy) {
		GameCharacter character = player.getCharacter();
		if (character == null || character.getPosition() == null) return;
		double fieldWidth = getFieldWidth();
		double fieldHeight = getFieldHeight();
		double nextX = character.getPosition().getX() + dx;
		double nextY = character.getPosition().getY() + dy;
		if (nextX < 0) nextX = 0;
		if (nextX > fieldWidth) nextX = fieldWidth;
		if (nextY < 0) nextY = 0;
		if (nextY > fieldHeight) nextY = fieldHeight;
		character.setPosition(nextX, nextY);
	}

	private void applyNormalAttack(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		if (character == null) return;
		character.normalAttack();
		if (character.isRanged()) {
			spawnProjectile(player, character, 1.0);
		} else {
			spawnMeleeAttack(player, character, 1.0);
		}
	}

	private void applyChargeAttack(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		if (character == null) return;
		long chargeMs = stopCharge(player);
		character.chargeAttack(chargeMs);
		double power = character.getAttackPowerRatio();
		if (character.isRanged()) {
			spawnProjectile(player, character, power);
		} else {
			spawnMeleeAttack(player, character, power);
		}
	}

	private void applyDefend(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		if (character == null) return;
		character.defend();
	}

	private void spawnProjectile(PlayerInfo player, GameCharacter character, double power) {
		if (battleField == null || character.getPosition() == null) return;
		ProjectileType projectileType = character.getProjectileType();
		double maxDistance = character.getProjectileRange();
		if (projectileType == null || maxDistance <= 0) return;
		int direction = getFacingDirection(player);
		double speed = character.getProjectileSpeed() * Math.max(1.0, power);
		double damage = character.getAttack();
		double startX = character.getPosition().getX() + (direction * 16);
		double startY = character.getPosition().getY() + 35;
		Projectile projectile = new Projectile(
				projectileType,
				player.getId(),
				startX,
				startY,
				direction * speed,
				0,
				power,
				damage,
				maxDistance
		);
		battleField.addEntity(projectile);
	}

	private void spawnMeleeAttack(PlayerInfo player, GameCharacter character, double power) {
		if (battleField == null || character.getPosition() == null) return;
		double width = character.getMeleeWidth() * Math.min(1.4, power);
		double height = character.getMeleeHeight() * Math.min(1.3, power);
		double offset = character.getMeleeOffset();
		int lifetime = character.getMeleeLifetimeTicks();
		double damage = character.getAttack();
		double baseX = character.getPosition().getX();
		double baseY = character.getPosition().getY();
		AttackHitbox front = new AttackHitbox(
				player.getId(),
				damage,
				baseX + offset,
				baseY,
				width,
				height,
				0,
				0,
				0,
				lifetime
		);
		AttackHitbox back = new AttackHitbox(
				player.getId(),
				damage,
				baseX - offset,
				baseY,
				width,
				height,
				0,
				0,
				0,
				lifetime
		);
		battleField.addEntity(front);
		battleField.addEntity(back);
	}

	private boolean applyJump(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		if (character == null || !character.canJump()) return false;
		character.setVerticalVelocity(character.getJumpVelocity());
		character.registerJump();
		return true;
	}

	private String formatPlayerEntry(PlayerInfo info) {
		GameCharacter character = info.getCharacter();
		CharacterType type = character != null ? character.getType() : CharacterType.defaultType();
		return info.getId() + " " + info.getName() + " " + info.isReady() + " " + type.getId();
	}

	private void setFacingDirection(PlayerInfo player, int direction) {
		if (direction == 0) return;
		facingDirections.put(player.getId(), direction > 0 ? 1 : -1);
	}

	private int getFacingDirection(PlayerInfo player) {
		return facingDirections.getOrDefault(player.getId(), 1);
	}

	private void startCharge(PlayerInfo player) {
		chargeStartTimes.put(player.getId(), System.currentTimeMillis());
	}

	private long stopCharge(PlayerInfo player) {
		Long start = chargeStartTimes.remove(player.getId());
		if (start == null) return 0;
		return Math.max(0, System.currentTimeMillis() - start);
	}

	private double resolveMoveStepX(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		return character != null ? character.getMoveStepX() : 0;
	}

	private double resolveMoveStepY(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		return character != null ? character.getMoveStepY() : 0;
	}

	private int getFieldWidth() {
		return battleField != null ? battleField.getWidth() : BattleField.DEFAULT_WIDTH;
	}

	private int getFieldHeight() {
		return battleField != null ? battleField.getHeight() : BattleField.DEFAULT_HEIGHT;
	}

}
