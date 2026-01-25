package server;

import model.BattleField;
import model.CharacterType;
import model.GameCharacter;
import model.Projectile;
import model.ProjectileType;
import network.Protocol;

import java.io.Closeable;
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
	private static final int FIELD_WIDTH = 1000;
	private static final int FIELD_HEIGHT = 520;
	private static final double MOVE_STEP = 6.0;
	private static final double JUMP_VELOCITY = 14.0;
	private static final double GROUND_Y = FIELD_HEIGHT * 0.36;
	private static final double GRAVITY = -0.9;
	private static final double PROJECTILE_SPEED = 9.0;
	private static final long MAX_CHARGE_MS = 1200;
	private static final double MAX_CHARGE_MULTIPLIER = 2.5;

	// -------------------- インスタンス定数 --------------------
	private final int roomId;
	private final ConcurrentLinkedQueue<ServerCommand> commandQueue;
	private final ConcurrentHashMap<ClientHandler, Player> playerMap;

	// -------------------- インスタンス変数 --------------------
	private volatile Runnable disconnectListener;
	private final boolean isPublic;
	private volatile boolean isStarted;
	private volatile boolean isClosed;
	private volatile boolean isGameOver;
	private volatile int alivePlayers;
	private volatile BattleField battleField;

	public GameRoom() {
		this(true);
	}

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
			sj.add(playerMap.values().stream().map(Player::toString).collect(Collectors.joining(",")));
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
		Player newPlayer = new Player(handler.getConnectionId(), playerName);
		playerMap.put(handler, newPlayer);
		handler.sendMessage(Protocol.joinSuccess(newPlayer.getId(), toString()));
		String joinMessage = Protocol.joinOpponent(newPlayer.getId(), newPlayer.getPlayerName());
		playerMap.keySet().forEach(other -> {
			if (other != handler) other.sendMessage(joinMessage);
		});
		logger.info("ルーム(ID: " + roomId + ")にプレイヤー(ID: " + handler.getConnectionId() + ")を追加しました");
		return true;
	}

	public synchronized void setDisconnectListener(Runnable listener) {
		this.disconnectListener = listener;
	}

	private synchronized void handleCommand(ServerCommand command) {
		// TODO: コマンド処理
		ClientHandler sender = command.getSender();
		Player player = playerMap.get(sender);
		if (player == null) return;
		String body = command.getBody();
		switch (command.getCommandType()) {
			case READY:
				CharacterType characterType = CharacterType.fromId(Integer.parseInt(body));
				player.selectCharacter(characterType);
				player.setReady();
				logger.fine(() -> "プレイヤー(ID: " + sender.getConnectionId() + ")が準備完了です。");
				String readyMessage = Protocol.readySuccess(player.getId(), characterType);
				playerMap.keySet().forEach(handler -> handler.sendMessage(readyMessage));
				startGame();
				break;
			case UNREADY:
				player.setUnReady();
				logger.fine(() -> "プレイヤー(ID: " + sender.getConnectionId() + ")が準備を解除しました。");
				String unreadyMessage = Protocol.unreadySuccess(player.getId());
				playerMap.keySet().forEach(handler -> handler.sendMessage(unreadyMessage));
				break;
			case MOVE_LEFT:
				player.setFacingDirection(-1);
				applyMove(player, -MOVE_STEP, 0);
				break;
			case MOVE_UP:
				applyMove(player, 0, MOVE_STEP);
				break;
			case MOVE_RIGHT:
				player.setFacingDirection(1);
				applyMove(player, MOVE_STEP, 0);
				break;
			case MOVE_DOWN:
				applyMove(player, 0, -MOVE_STEP);
				break;
			case JUMP:
				if (applyJump(player)) {
					broadcastAction(Protocol.jump(player.getId()));
				}
				break;
			case CHARGE_START:
				player.startCharge();
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
		for (Player player : playerMap.values()) {
			if (!player.isReady()) return;
		}
		isStarted = true;
		isGameOver = false;
		alivePlayers = playerMap.size();
		logger.info("ルーム(ID: " + roomId + ")でゲーム開始");
		battleField = new BattleField(FIELD_WIDTH, FIELD_HEIGHT, GROUND_Y, GRAVITY);
		int index = 0;
		for (Player player : playerMap.values()) {
			GameCharacter character = player.getCharacter();
			if (character != null) {
				double x = FIELD_WIDTH * (0.2 + 0.2 * index);
				character.setPosition(x, GROUND_Y);
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
			playerMap.remove(handler);
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

	private void applyMove(Player player, double dx, double dy) {
		GameCharacter character = player.getCharacter();
		if (character == null || character.getPosition() == null) return;
		double nextX = character.getPosition().getX() + dx;
		double nextY = character.getPosition().getY() + dy;
		if (nextX < 0) nextX = 0;
		if (nextX > FIELD_WIDTH) nextX = FIELD_WIDTH;
		if (nextY < 0) nextY = 0;
		if (nextY > FIELD_HEIGHT) nextY = FIELD_HEIGHT;
		character.setPosition(nextX, nextY);
	}

	private void applyNormalAttack(Player player) {
		GameCharacter character = player.getCharacter();
		if (character == null) return;
		character.normalAttack();
		spawnProjectile(player, character, 1.0);
	}

	private void applyChargeAttack(Player player) {
		GameCharacter character = player.getCharacter();
		if (character == null) return;
		character.chargeAttack();
		double power = resolveChargePower(player.stopCharge());
		spawnProjectile(player, character, power);
	}

	private void applyDefend(Player player) {
		GameCharacter character = player.getCharacter();
		if (character == null) return;
		character.defend();
	}

	private void spawnProjectile(Player player, GameCharacter character, double power) {
		if (battleField == null || character.getPosition() == null) return;
		ProjectileType projectileType;
		switch (character.getType()) {
			case ARCHER:
				projectileType = ProjectileType.ARROW;
				break;
			case WIZARD:
				projectileType = ProjectileType.MAGIC;
				break;
			default:
				return;
		}
		int direction = player.getFacingDirection();
		double speed = PROJECTILE_SPEED * Math.max(1.0, power);
		double damage = character.getAttack();
		double startX = character.getPosition().getX() + (direction * 16);
		double startY = character.getPosition().getY() + 16;
		Projectile projectile = new Projectile(projectileType, player.getId(), startX, startY, direction * speed, 0, power, damage);
		battleField.addEntity(projectile);
	}

	private boolean applyJump(Player player) {
		GameCharacter character = player.getCharacter();
		if (character == null || !character.canJump()) return false;
		character.setVerticalVelocity(JUMP_VELOCITY);
		character.registerJump();
		return true;
	}

	private double resolveChargePower(long chargeMs) {
		if (chargeMs <= 0) return 1.0;
		long clamped = Math.min(chargeMs, MAX_CHARGE_MS);
		double ratio = clamped / (double) MAX_CHARGE_MS;
		return 1.0 + (MAX_CHARGE_MULTIPLIER - 1.0) * ratio;
	}
}
