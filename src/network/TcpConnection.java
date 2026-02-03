package network;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TcpConnection implements Closeable {
	private static final Logger logger = Logger.getLogger(TcpConnection.class.getName());

	private final Socket socket;
	private final PrintWriter out;
	private final BufferedReader in;
	private final MessageSender sender;
	private final AtomicBoolean disconnectNotified = new AtomicBoolean(false);

	private Thread senderThread;
	private Thread receiverThread;

	private volatile MessageListener messageListener;
	private volatile DisconnectListener disconnectListener;

	private volatile boolean isConnected;

	public TcpConnection(Socket socket) throws IOException {
		this.socket = socket;
		this.out = new PrintWriter(socket.getOutputStream(), true);
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.isConnected = true;
		this.sender = new MessageSender();
	}

	public void start() {
		if (senderThread != null || receiverThread != null) return;
		senderThread = new Thread(sender, "Sender");
		receiverThread = new Thread(new MessageReceiver(), "Receiver");
		senderThread.start();
		receiverThread.start();
	}

	public boolean isConnected() {
		return isConnected;
	}

	public void sendMessage(String message) {
		sender.send(message);
	}

	public void close() {
		if (!isConnected) return;
		isConnected = false;
		try {
			socket.close();
		} catch (IOException e) {
			logger.log(Level.FINE, "ソケットクローズ中に例外が発生しました。", e);
		} finally {
			if (senderThread != null) senderThread.interrupt();
			if (receiverThread != null) receiverThread.interrupt();
			notifyDisconnect();
		}
	}

	// -------------------- リスナーのセッター --------------------
	public void setMessageListener(MessageListener listener) {
		this.messageListener = listener;
	}

	public void setDisconnectListener(DisconnectListener listener) {
		this.disconnectListener = listener;
	}

	private void notifyDisconnect() {
		if (disconnectListener != null && disconnectNotified.compareAndSet(false, true)) {
			try {
				logger.fine("接続が切断されました。");
				disconnectListener.onDisconnect();
			} catch (Exception e) {
				logger.log(Level.WARNING, "切断リスナー実行中に例外が発生しました。", e);
			}
		}
	}

	// -------------------- 内部クラス --------------------
	private class MessageReceiver implements Runnable {
		public void run() {
			try {
				while (isConnected) {
					String line = in.readLine();
					if (line == null) break;

					if (messageListener != null) {
						logger.fine(() -> "受信: " + line);
						messageListener.onMessageReceived(line);
					}
				}
			} catch (IOException e) {
				logger.log(Level.FINE, "受信処理中に例外が発生しました。", e);
			} finally {
				close();
			}
		}
	}

	private class MessageSender implements Runnable {
		private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

		void send(String msg) {
			queue.offer(msg);
		}

		public void run() {
			try {
				while (isConnected && !Thread.currentThread().isInterrupted()) {
					String msg = queue.take();
					out.println(msg);
					logger.fine(() -> "送信: " + msg);
					if (out.checkError()) {
						throw new IOException("Write error");
					}
				}
			} catch (Exception e) {
				logger.log(Level.FINE, "送信処理中に例外が発生しました。", e);
				close();
			}
		}
	}
}