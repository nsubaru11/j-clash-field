package client.controller;

import model.Protocol;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 通信を管理するクラスです。
 */
class NetworkController extends Thread implements Closeable {
	private static final Logger logger = Logger.getLogger(NetworkController.class.getName());
	private final String host;
	private final int port;
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;

	private volatile Consumer<String> messageListener;

	public NetworkController(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public boolean connect() {
		try {
			socket = new Socket(host, port);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public boolean joinRoom(int roomId) {
		out.println(Protocol.join(roomId));
		return true;
	}

	public boolean joinRoom() {
		out.println(Protocol.join(-1));
		return true;
	}

	public void moveLeft() {
		out.println(Protocol.moveLeft());
	}

	public void moveRight() {
		out.println(Protocol.moveRight());
	}

	public void moveUp() {
		out.println(Protocol.moveUp());
	}

	public void moveDown() {
		out.println(Protocol.moveDown());
	}

	public void resign() {
		out.println(Protocol.resign());
	}

	public void disconnect() {
		out.println(Protocol.disconnect());
	}

	public void run() {
		try {
			// メッセージ受信ループ
			while (true) {
				String line = in.readLine();
				if (line == null) break;
				if (messageListener != null) {
					logger.fine(() -> "サーバーから受信: " + line);
					messageListener.accept(line);
				}
			}
		} catch (final SocketTimeoutException e) {
			logger.log(Level.WARNING, "サーバーからのタイムアウトにより切断", e);
		} catch (final IOException e) {
			// 意図的に閉じた(isConnected==false)場合はエラーログを出さない
			logger.log(Level.WARNING, "サーバー接続エラー", e);
		} finally {
			logger.log(Level.WARNING, "切断リスナー実行中にエラー");
			close();
		}
	}

	@Override
	public void close() {
		try {
			socket.close();
			logger.fine(() -> "ソケットをクローズしました");
		} catch (IOException e) {
			logger.log(Level.WARNING, "プレイヤーソケットクローズに失敗", e);
		}
	}
}
