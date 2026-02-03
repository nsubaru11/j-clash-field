package server.controller;

import network.DisconnectListener;
import network.MessageListener;
import network.TcpConnection;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

class ClientHandler implements Closeable {
	// -------------------- 定数・フィールド --------------------
	private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
	private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

	private final int connectionId;
	private final TcpConnection connection;

	// -------------------- コンストラクタ --------------------
	public ClientHandler(final Socket socket) throws IOException {
		this.connectionId = ID_GENERATOR.incrementAndGet();
		connection = new TcpConnection(socket);
	}

	public void start() {
		connection.start();
	}

	public void close() {
		if (!connection.isConnected()) return;
		connection.close();
		logger.fine(() -> "プレイヤー(ID: " + connectionId + ")ソケットをクローズしました");
	}

	public void sendMessage(final String message) {
		connection.sendMessage(message);
	}

	public int getConnectionId() {
		return connectionId;
	}

	public void setMessageListener(final MessageListener messageListener) {
		connection.setMessageListener(messageListener);
	}

	public void setDisconnectListener(final DisconnectListener disconnectListener) {
		connection.setDisconnectListener(disconnectListener);
	}
}
