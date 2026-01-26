package server.controller;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

class ClientHandler implements Closeable {
	// -------------------- 定数・フィールド --------------------
	private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
	private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

	private final int connectionId;
	private final Socket socket;
	private final PrintWriter out;
	private final BufferedReader in;
	private final LinkedBlockingQueue<String> sendQueue;

	private Thread senderThread;
	private Thread receiverThread;

	private volatile Consumer<String> messageListener;
	private volatile Runnable disconnectListener;
	private volatile boolean isConnected;

	// -------------------- コンストラクタ --------------------
	public ClientHandler(final Socket socket) throws IOException {
		this.connectionId = ID_GENERATOR.incrementAndGet();
		this.socket = socket;
		socket.setSoTimeout(0);
		this.out = new PrintWriter(socket.getOutputStream(), true);
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.sendQueue = new LinkedBlockingQueue<>();
		this.isConnected = true;
	}

	public void start() {
		senderThread = new Thread(new MessageSender(), "Sender-" + connectionId);
		receiverThread = new Thread(new MessageReceiver(), "Receiver-" + connectionId);

		senderThread.start();
		receiverThread.start();
	}

	public void close() {
		if (!isConnected) return;
		isConnected = false;

		try {
			socket.close();
			logger.fine(() -> "プレイヤー(ID: " + connectionId + ")ソケットをクローズしました");
		} catch (IOException e) {
			logger.log(Level.WARNING, "プレイヤーソケットクローズに失敗", e);
		}

		if (senderThread != null && senderThread.isAlive()) {
			senderThread.interrupt();
		}
	}

	public void sendMessage(final String message) {
		sendQueue.offer(message);
	}

	public int getConnectionId() {
		return connectionId;
	}

	public void setMessageListener(final Consumer<String> messageListener) {
		this.messageListener = messageListener;
	}

	public void setDisconnectListener(final Runnable disconnectListener) {
		this.disconnectListener = disconnectListener;
	}

	/**
	 * 切断時のリスナー呼び出し処理を切り出し
	 */
	private void notifyDisconnect() {
		if (isConnected && disconnectListener != null) {
			try {
				disconnectListener.run();
			} catch (Exception e) {
				logger.log(Level.WARNING, "切断リスナー実行中にエラー", e);
			}
		}
	}

	private final class MessageSender implements Runnable {
		public void run() {
			try {
				while (isConnected && !Thread.currentThread().isInterrupted()) {
					String message = sendQueue.take();
					out.println(message);
					logger.fine(() -> "プレイヤー(ID: " + connectionId + ")に送信: " + message);

					if (out.checkError()) {
						logger.warning("メッセージ送信エラー: " + connectionId);
						close();
						break;
					}
				}
			} catch (InterruptedException e) {
				logger.fine("送信スレッド停止: " + connectionId);
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
						logger.fine(() -> "プレイヤー(ID: " + connectionId + ")から受信: " + line);
						messageListener.accept(line);
					}
				}
			} catch (SocketException e) {
				if (isConnected) logger.log(Level.WARNING, "予期せぬ切断: " + connectionId, e);
			} catch (IOException e) {
				if (isConnected) logger.log(Level.WARNING, "受信エラー: " + connectionId, e);
			} finally {
				notifyDisconnect();
				close();
			}
		}
	}
}
