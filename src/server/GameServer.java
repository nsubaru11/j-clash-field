package server;

import model.CommandType;
import model.LoggingConfig;
import model.Protocol;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * サーバーを起動するためのクラスです。
 */
public final class GameServer implements Runnable, Closeable {
	// -------------------- クラス定数 --------------------
	private static final Logger logger = Logger.getLogger(GameServer.class.getName());
	private static final int DEFAULT_PORT = 10000;

	// -------------------- インスタンス定数 --------------------
	private final ServerSocket serverSocket;
	private final LinkedHashSet<GameRoom> gameRooms;
	private final LinkedHashSet<ClientHandler> waitingPlayers;

	// -------------------- インスタンス変数 --------------------
	private volatile boolean isRunning;

	// -------------------- コンストラクタ --------------------
	public GameServer(final int port) {
		// 初期化
		try {
			// サーバーソケットを開く
			serverSocket = new ServerSocket(port);
			logger.info(() -> "サーバーがポート " + port + " で起動しました。");
		} catch (final IOException e) {
			logger.log(Level.SEVERE, "サーバーがポート " + port + " で起動出来ませんでした。", e);
			throw new RuntimeException(e);
		} catch (final Exception e) {
			logger.log(Level.SEVERE, "予期せぬ重大なエラーが発生しました。", e);
			throw new RuntimeException(e);
		}
		gameRooms = new LinkedHashSet<>();
		waitingPlayers = new LinkedHashSet<>();
		isRunning = true;
	}

	// -------------------- エントリーポイント --------------------
	public static void main(final String[] args) {
		LoggingConfig.initialize("server");

		// ポート番号の設定
		int port = DEFAULT_PORT;
		if (args.length > 0) {
			try {
				// 引数があれば、それをポート番号として使う
				port = Integer.parseInt(args[0]);
			} catch (final NumberFormatException e) {
				logger.log(Level.WARNING, "ポート番号が不正です。デフォルト(" + DEFAULT_PORT + ")を使用します。", e);
			}
		}

		// サーバーを起動する
		GameServer server = new GameServer(port);
		new Thread(server).start();
		Runtime.getRuntime().addShutdownHook(new Thread(server::close));

		// 標準入力を監視してサーバーを終了する（exitを入力すると終了する）
		Scanner sc = new Scanner(System.in);
		while (true) {
			String input = sc.nextLine();
			if (input.equals("exit")) {
				server.close();
				sc.close();
				break;
			}
		}
	}

	// -------------------- publicメソッド --------------------
	public void run() {
		while (isRunning) {
			try {
				// クライアントの接続を待つ
				Socket clientSocket = serverSocket.accept();
				ClientHandler handler = new ClientHandler(clientSocket);
				handler.start();
				handler.setMessageListener(msg -> join(handler, msg));
				logger.info(() -> "新しいクライアント(ID: " + handler.getConnectionId() + ")が接続しました。");
			} catch (final IOException e) {
				if (isRunning) {
					logger.log(Level.SEVERE, "クライアント接続処理で例外が発生しました。", e);
					isRunning = false;
				} else {
					logger.info(() -> "サーバー停止に伴い接続待機を終了します。");
				}
			} catch (final Exception e) {
				logger.log(Level.SEVERE, "予期せぬ重大なエラーが発生しました。", e);
				throw new RuntimeException(e);
			}
		}
	}

	public synchronized void close() {
		isRunning = false;
		waitingPlayers.forEach(ClientHandler::close);
		gameRooms.forEach(GameRoom::close);
		try {
			serverSocket.close();
		} catch (final IOException e) {
			logger.log(Level.WARNING, "サーバーソケットクローズに失敗しました。", e);
			throw new RuntimeException(e);
		}
	}

	// -------------------- privateメソッド --------------------
	/** プレイヤーがルームに参加するコマンドを受け取ったときの処理 */
	private synchronized void join(ClientHandler handler, String msg) {
		Command cmd = new Command(handler, msg);
		if (cmd.getCommandType() != CommandType.JOIN) {
			logger.warning(() -> "不正なコマンドが送信されました。");
			return;
		}
		String roomId = cmd.getBody();
		if (roomId.isEmpty()) {
			addWaitingHandler(handler);
		} else {
			int id = Integer.parseInt(roomId);
			for (GameRoom room : gameRooms) {
				if (room.getRoomId() == id) {
					if (room.join(handler)) {
						handler.sendMessage(Protocol.joinSuccess(room.getRoomId()));
						logger.info(() -> "プレイヤー(ID: " + handler.getConnectionId() + ")がルーム(ID: " + room.getRoomId() + ")に追加されました。");
					} else {
						logger.warning(() -> "ルーム(ID: " + room.getRoomId() + ")は既に満員です。");
					}
					logger.config(room::toString);
					return;
				}
			}
			logger.warning(() -> "ルーム(ID: " + id + ")は存在しません。");
			handler.sendMessage(Protocol.joinFailed());
		}
	}

	private synchronized void addWaitingHandler(final ClientHandler handler) {
		if (!isRunning) return;
		waitingPlayers.add(handler);
		handler.setDisconnectListener(() -> disconnectHandler(handler));
		logger.info(() -> "プレイヤー(ID: " + handler.getConnectionId() + ")が待ち行列に追加されました。");
		matchPlayers();
	}

	private synchronized void matchPlayers() {
		Iterator<ClientHandler> iterator = waitingPlayers.iterator();
		while (iterator.hasNext()) {
			ClientHandler handler = iterator.next();
			boolean assigned = false;
			for (GameRoom room : gameRooms) {
				if (room.join(handler)) {
					assigned = true;
					logger.info(() -> "プレイヤー(ID: " + handler.getConnectionId() + ")がルーム(ID: " + room.getRoomId() + ")に追加されました。");
					logger.config(room::toString);
					break;
				}
			}
			if (!assigned) {
				GameRoom room = new GameRoom();
				room.join(handler);
				room.start();
				room.setDisconnectListener(() -> removeGameRoom(room));
				gameRooms.add(room);
				logger.info(() -> "プレイヤー(ID: " + handler.getConnectionId() + ")がルーム(ID: " + room.getRoomId() + ")に追加されました。");
				logger.config(room::toString);
			}
			iterator.remove();
		}
	}

	private synchronized void removeGameRoom(final GameRoom room) {
		if (!isRunning || room == null) return;
		gameRooms.remove(room);
	}

	private synchronized void disconnectHandler(final ClientHandler handler) {
		if (!isRunning || handler == null) return;
		logger.info(() -> "プレイヤー(ID: " + handler.getConnectionId() + ")が切断されました。");
		waitingPlayers.remove(handler);
	}
}
