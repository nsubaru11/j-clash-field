package server;

import java.io.Closeable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

class GameRoom extends Thread implements Closeable {
	private static final Logger logger = Logger.getLogger(GameRoom.class.getName());
	private static final int MAX_PLAYERS = 4;
	private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);
	private final int roomId;
	private final Map<ClientHandler, Player> playerMap;
	private final Queue<Command> commandQueue = new ConcurrentLinkedQueue<>();
	private volatile Runnable disconnectListener;
	private boolean isStarted;
	private boolean isClosed;

	public GameRoom() {
		this.roomId = ID_GENERATOR.incrementAndGet();
		this.playerMap = new ConcurrentHashMap<>(MAX_PLAYERS);
		this.isStarted = false;
		this.isClosed = false;
	}

	@Override
	public void run() {
		while (!isClosed) {
			while (!commandQueue.isEmpty()) {
				Command cmd = commandQueue.poll();
				ClientHandler sender = cmd.getSender();
				String msg = cmd.getMessage();
				Player targetPlayer = playerMap.get(sender);
				if (targetPlayer != null) {
					handleCommand(targetPlayer, msg);
				}
			}
			broadcastState();
			try {
				sleep(16);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public boolean join(final ClientHandler handler) {
		if (playerMap.size() >= MAX_PLAYERS) return false;
		logger.info(() -> "ルーム " + roomId + " にプレイヤー " + handler.getConnectionId() + " を追加しました");
		handler.setMessageListener(msg -> this.commandQueue.add(new Command(handler, msg)));
		handler.setDisconnectListener(() -> handleDisconnect(handler));
		Player newPlayer = new Player("NoName");
		playerMap.put(handler, newPlayer);
		return true;
	}

	public void setDisconnectListener(Runnable listener) {
		this.disconnectListener = listener;
	}

	// ここでゲームロジックを処理
	private void handleCommand(Player player, String message) {
		// 例: "MOVE_RIGHT" というコマンドを解析して座標を更新
	}

	public void exitRoom(ClientHandler handler) {
		playerMap.remove(handler);
		handler.close();
		if (playerMap.isEmpty()) isClosed = true;
	}

	private void startGame() {
		for (Player player : playerMap.values()) {
			if (!player.isReady()) return;
		}
		isStarted = true;
		logger.info(() -> "ルーム " + roomId + " でゲーム開始");
	}

	public void handleResign(Player resigner) {
		logger.info(() -> "ルーム " + roomId + ": Player resigned");

		close();
	}

	public void handleDisconnect(ClientHandler handler) {
		logger.info(() -> "ルーム " + roomId + " でプレイヤー切断");
		exitRoom(handler);
	}

	private boolean isGameOver() {
		return false;
	}

	private void endGame() {
		notifyResult();
		close();
	}

	private void notifyResult() {

	}

	private void broadcastState() {
	}

	public int getRoomId() {
		return roomId;
	}

	public boolean isClosed() {
		return isClosed;
	}

	public void close() {
		isClosed = true;
		for (ClientHandler handler : playerMap.keySet()) {
			handler.close();
		}
		if (disconnectListener != null) {
			disconnectListener.run();
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ルーム ").append(roomId).append(":\n");
		if (isStarted) sb.append("ゲーム開始中\n");
		else sb.append("マッチング中\n");
		sb.append("プレイヤー:\n");
		for (Player player : playerMap.values()) sb.append("  ").append(player.getPlayerName()).append("\n");
		return sb.toString();
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof GameRoom)) return false;
		return ((GameRoom) obj).roomId == roomId;
	}

	public int hashCode() {
		return roomId;
	}
}
