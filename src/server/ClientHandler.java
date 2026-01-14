package server;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

class ClientHandler extends Thread implements Closeable {
	private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
	private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

	private final int connectionId;
	private final Socket socket;
	private final PrintWriter out;
	private final BufferedReader in;
	private final LinkedBlockingQueue<String> sendQueue;

	private volatile Consumer<String> messageListener;
	private volatile Runnable disconnectListener;
	private volatile boolean isConnected;
	private volatile boolean isClosed;

	public ClientHandler(final Socket socket) {
		this.connectionId = ID_GENERATOR.incrementAndGet();
		this.socket = socket;
		this.sendQueue = new LinkedBlockingQueue<>();
		this.isConnected = true;
		this.isClosed = false;
		try {
			socket.setSoTimeout(10000);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setMessageListener(final Consumer<String> messageListener) {
		this.messageListener = messageListener;
	}

	public void setDisconnectListener(final Runnable disconnectListener) {
		this.disconnectListener = disconnectListener;
	}

	public void sendMessage(final String message) {
		sendQueue.add(message);
	}

	public void run() {
		new Thread(() -> {
			while (isConnected) {
				try {
					String message = sendQueue.take();
					out.println(message);
					logger.fine(() -> "プレイヤー(ID: " + connectionId + ")に送信: " + message);
					if (out.checkError()) {
						logger.warning("メッセージ送信エラー: " + connectionId);
						close();
					}
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}).start();
		try {
			// メッセージ受信ループ
			while (isConnected) {
				String line = in.readLine();
				if (line == null) break;
				if (messageListener != null) {
					logger.fine(() -> "プレイヤー(ID: " + connectionId + ")から受信: " + line);
					messageListener.accept(line);
				}
			}
		} catch (final SocketTimeoutException e) {
			logger.log(Level.WARNING, "プレイヤー(ID: " + connectionId + ")タイムアウトにより切断", e);
		} catch (final IOException e) {
			// 意図的に閉じた(isConnected==false)場合はエラーログを出さない
			if (isConnected) logger.log(Level.WARNING, "プレイヤー(ID: " + connectionId + ")接続エラー", e);
		} finally {
			if (isConnected && disconnectListener != null) {
				try {
					disconnectListener.run();
				} catch (Exception e) {
					logger.log(Level.WARNING, "切断リスナー実行中にエラー", e);
				}
			}
			close();
		}
	}

	public void close() {
		if (!isConnected || isClosed) return;
		isClosed = true;
		isConnected = false;
		try {
			socket.close();
			logger.fine(() -> "プレイヤー(ID: " + connectionId + ")ソケットをクローズしました");
		} catch (IOException e) {
			logger.log(Level.WARNING, "プレイヤーソケットクローズに失敗", e);
		}
	}

	public int getConnectionId() {
		return connectionId;
	}
}
