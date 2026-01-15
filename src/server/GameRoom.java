package server;

import model.BattleField;
import model.GameCharacter;
import model.Protocol;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

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
	private final ConcurrentHashMap<ClientHandler, Player> playerMap;

	// -------------------- インスタンス変数 --------------------
	private volatile Runnable disconnectListener;
	private volatile boolean isStarted;
	private volatile boolean isClosed;
	private volatile boolean isGameOver;
	private volatile int alivePlayers;
	private volatile BattleField battleField;

	public GameRoom() {
		roomId = ID_GENERATOR.incrementAndGet();
		commandQueue = new ConcurrentLinkedQueue<>();
		playerMap = new ConcurrentHashMap<>(MAX_PLAYERS);
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
				battleField.update();
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
	 * デバッグ用
	 */
	public synchronized String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ルーム(ID: ").append(roomId).append("):\n");
		if (isStarted) sb.append("ゲーム開始中\n");
		else sb.append("マッチング中\n");
		sb.append("プレイヤー:\n");
		for (ClientHandler handler : playerMap.keySet()) sb.append("  ").append(handler.getConnectionId()).append("\n");
		return sb.toString();
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

	public synchronized boolean join(final ClientHandler handler) {
		if (isClosed || isStarted || playerMap.size() >= MAX_PLAYERS) return false;
		handler.setMessageListener(msg -> this.commandQueue.add(new ServerCommand(handler, msg)));
		handler.setDisconnectListener(() -> handleDisconnect(handler));
		Player newPlayer = new Player("NoName");
		playerMap.put(handler, newPlayer);
		// TODO: メッセージ処理
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
		switch (command.getCommandType()) {
			case READY:
				player.setReady(command.getBody());
				logger.fine(() -> "プレイヤー(ID: " + sender.getConnectionId() + ")が準備完了です。");
				startGame();
				break;
			case UNREADY:
				player.setUnReady();
				logger.fine(() -> "プレイヤー(ID: " + sender.getConnectionId() + ")が準備を解除しました。");
				// TODO: 全体通知
				break;
			case MOVE_LEFT:
				break;
			case MOVE_UP:
				break;
			case MOVE_RIGHT:
				break;
			case MOVE_DOWN:
				break;
			case RESIGN:
				break;
			default:
				break;
		}
	}

	private synchronized void startGame() {
		for (Player player : playerMap.values()) {
			if (!player.isReady()) return;
		}
		isStarted = true;
		isGameOver = false;
		alivePlayers = playerMap.size();
		logger.info("ルーム(ID: " + roomId + ")でゲーム開始");
		battleField = new BattleField();
		for (Player player : playerMap.values()) {
			battleField.addEntity(player.getCharacter());
		}
		// TODO: 開始通知
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

	private synchronized void broadcastState() {
		// TODO: 状態を全員に通知する
		if (!isStarted || battleField == null) return;

		playerMap.forEach((handler, player) -> {
			GameCharacter character = player.getCharacter();
			if (character != null) {
				String msg = Protocol.move(character.getId(), character.getPosition().getX(), character.getPosition().getY());
				playerMap.keySet().forEach(h -> h.sendMessage(msg));
			}
		});
	}

}
