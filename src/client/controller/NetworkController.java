package client.controller;

import model.Protocol;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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

	public NetworkController(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public boolean connect(String playerName) {
		try {
			socket = new Socket(host, port);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out.println(Protocol.connect(playerName));
			return true;
		} catch (IOException e) {
			return false;
		}
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
		while (true) {
			try {
				String line = in.readLine();
				if (line == null) break;
			} catch (IOException e) {
				break;
			}
		}
	}

	@Override
	public void close() {
		disconnect();
	}
}
