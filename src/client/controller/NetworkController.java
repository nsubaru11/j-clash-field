package client.controller;

import model.CharacterType;
import network.MessageListener;
import network.Protocol;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 通信を管理するクラスです。
 */
public class NetworkController implements Closeable {
	private static final Logger logger = Logger.getLogger(NetworkController.class.getName());
	private final String host;
	private final int port;
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private LinkedBlockingQueue<String> sendQueue;
	private Thread senderThread;
	private Thread receiverThread;
	private volatile MessageListener messageListener;
	private volatile boolean isConnected;

	public NetworkController(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public boolean isConnected() {
		return isConnected;
	}

	public void close() {
		isConnected = false;
		if (senderThread != null) senderThread.interrupt();
		if (receiverThread != null) receiverThread.interrupt();
		try {
			socket.close();
			logger.fine(() -> "ソケットをクローズしました");
		} catch (IOException e) {
			logger.log(Level.WARNING, "プレイヤーソケットクローズに失敗", e);
		}
	}

	public void connect(Runnable onSuccess, Runnable onFailure) {
		new Thread(() -> {
			int attempt = 0;
			while (attempt < 3) {
				attempt++;
				try {
					socket = new Socket(host, port);
					out = new PrintWriter(socket.getOutputStream(), true);
					in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					isConnected = true;
					sendQueue = new LinkedBlockingQueue<>();
					startThreads();
					logger.info("接続に成功しました。");
					if (onSuccess != null) onSuccess.run();
					break;
				} catch (IOException e) {
					if (attempt < 3) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e2) {
							logger.warning("");
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
		sendQueue.offer(Protocol.join(userName, roomId));
	}

	public void createRoom(String userName) {
		sendQueue.offer(Protocol.createRoom(userName));
	}

	public void ready(CharacterType characterType) {
		sendQueue.offer(Protocol.ready(characterType));
	}

	public void unready() {
		sendQueue.offer(Protocol.unready());
	}

	public void moveLeft() {
		sendQueue.offer(Protocol.moveLeft());
	}

	public void moveRight() {
		sendQueue.offer(Protocol.moveRight());
	}

	public void moveUp() {
		sendQueue.offer(Protocol.moveUp());
	}

	public void moveDown() {
		sendQueue.offer(Protocol.moveDown());
	}

	public void normalAttack() {
		sendQueue.offer(Protocol.normalAttack());
	}

	public void chargeAttack() {
		sendQueue.offer(Protocol.chargeAttack());
	}

	public void chargeStart() {
		sendQueue.offer(Protocol.chargeStart());
	}

	public void defend() {
		sendQueue.offer(Protocol.defend());
	}

	public void resign() {
		sendQueue.offer(Protocol.resign());
	}

	public void disconnect() {
		if (!isConnected) return;
		sendQueue.offer(Protocol.disconnect());
		close();
	}

	public void setMessageListener(MessageListener messageListener) {
		this.messageListener = messageListener;
	}

	private void startThreads() {
		senderThread = new Thread(new MessageSender(), "Sender");
		receiverThread = new Thread(new MessageReceiver(), "Receiver");

		senderThread.start();
		receiverThread.start();
	}

	private final class MessageSender implements Runnable {
		public void run() {
			try {
				while (isConnected && !Thread.currentThread().isInterrupted()) {
					String message = sendQueue.take();
					out.println(message);
					logger.fine(() -> "サーバーに送信: " + message);

					if (out.checkError()) {
						logger.warning("メッセージ送信エラー");
						close();
						break;
					}
				}
			} catch (InterruptedException e) {
				logger.fine("送信スレッド停止");
			}
		}
	}

	private final class MessageReceiver implements Runnable {
		public void run() {
			try {
				while (isConnected) {
					String line = in.readLine();
					if (line == null) break; // 切断検知

					if (messageListener != null) {
						logger.fine(() -> "サーバーから受信: " + line);
						messageListener.onMessageReceived(line);
					}
				}
			} catch (SocketException e) {
				if (isConnected) logger.log(Level.WARNING, "予期せぬ切断", e);
			} catch (IOException e) {
				if (isConnected) logger.log(Level.WARNING, "受信エラー", e);
			} finally {
				close();
			}
		}
	}
}
