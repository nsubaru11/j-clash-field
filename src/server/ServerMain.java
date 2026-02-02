package server;

import model.LoggingConfig;
import server.controller.GameServer;

import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ServerMain {
	private static final Logger logger = Logger.getLogger(ServerMain.class.getName());
	private static final int DEFAULT_PORT = 10000;

	private ServerMain() {
	}

	public static void main(final String[] args) {
		LoggingConfig.initialize("server");

		int port = DEFAULT_PORT;
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (final NumberFormatException e) {
				logger.log(Level.WARNING, "ポート番号が不正です。デフォルト(" + DEFAULT_PORT + ")を使用します。", e);
			}
		}

		GameServer server = new GameServer(port);
		new Thread(server).start();
		Runtime.getRuntime().addShutdownHook(new Thread(server::close));

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
}
