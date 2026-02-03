package client.controller;

import model.CharacterType;
import network.DisconnectListener;
import network.MessageListener;
import network.Protocol;
import network.TcpConnection;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 通信を管理するクラスです。
 */
public class NetworkController implements Closeable {
	private static final Logger logger = Logger.getLogger(NetworkController.class.getName());
	private final String host;
	private final int port;
	private TcpConnection connection;

	public NetworkController(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public boolean isConnected() {
		return connection != null && connection.isConnected();
	}

	public void close() {
		if (connection == null) return;
		connection.close();
		logger.fine(() -> "ソケットをクローズしました");
	}

	public void connect(MessageListener messageListener, DisconnectListener disconnectListener, Runnable onSuccess, Runnable onFailure) {
		new Thread(() -> {
			int attempt = 0;
			int maxAttempt = 5;
			while (attempt < maxAttempt) {
				attempt++;
				logger.fine("接続試行 " + attempt + "/" + maxAttempt);
				try {
					connection = new TcpConnection(new Socket(host, port));
					connection.setMessageListener(messageListener);
					connection.setDisconnectListener(disconnectListener);
					connection.start();
					logger.info("接続に成功しました。");
					if (onSuccess != null) onSuccess.run();
					break;
				} catch (IOException e) {
					if (attempt < maxAttempt) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e2) {
							logger.warning("接続リトライが中断されました。");
							break;
						}
						continue;
					}
					logger.log(Level.SEVERE, "接続に失敗しました。", e);
					if (onFailure != null) onFailure.run();
				}
			}
		}).start();
	}

	public void joinRoom(String userName, int roomId) {
		connection.sendMessage(Protocol.join(userName, roomId));
	}

	public void createRoom(String userName) {
		connection.sendMessage(Protocol.createRoom(userName));
	}

	public void ready(CharacterType characterType) {
		connection.sendMessage(Protocol.ready(characterType));
	}

	public void unready() {
		connection.sendMessage(Protocol.unready());
	}

	public void moveLeft() {
		connection.sendMessage(Protocol.moveLeft());
	}

	public void moveRight() {
		connection.sendMessage(Protocol.moveRight());
	}

	public void moveUp() {
		connection.sendMessage(Protocol.moveUp());
	}

	public void moveDown() {
		connection.sendMessage(Protocol.moveDown());
	}

	public void normalAttack() {
		connection.sendMessage(Protocol.normalAttack());
	}

	public void chargeAttack() {
		connection.sendMessage(Protocol.chargeAttack());
	}

	public void chargeStart() {
		connection.sendMessage(Protocol.chargeStart());
	}

	public void defend() {
		connection.sendMessage(Protocol.defend());
	}

	public void resign() {
		connection.sendMessage(Protocol.resign());
	}

	public void disconnect() {
		if (connection == null || !connection.isConnected()) return;
		connection.sendMessage(Protocol.disconnect());
		close();
	}

	public void setMessageListener(MessageListener messageListener) {
		connection.setMessageListener(messageListener);
	}

	public void setDisconnectListener(DisconnectListener disconnectListener) {
		if (connection == null) {
			logger.fine("接続前のためDisconnectListenerを設定できません。");
			return;
		}
		connection.setDisconnectListener(disconnectListener);
	}
}
