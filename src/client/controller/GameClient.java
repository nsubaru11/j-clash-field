package client.controller;

import model.LoggingConfig;

import java.util.logging.Logger;

public final class GameClient {
	private static final Logger logger = Logger.getLogger(GameClient.class.getName());
	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 10000;
	// private static final String DEFAULT_HOST = "133.42.227.142";
	private static NetworkController network;
	private static GuiController gui;

	public static void main(final String[] args) {
		LoggingConfig.initialize("client");
		int len = args.length;
		String host = len == 0 ? DEFAULT_HOST : args[0];
		int port = len <= 1 ? DEFAULT_PORT : Integer.parseInt(args[1]);
		gui = new GuiController();
		network = new NetworkController(host, port);
		network.start();
		Runtime.getRuntime().addShutdownHook(new Thread(network::close));
		gui.showLoad();
		if (!network.connect()) {
			logger.severe("接続に失敗しました。");
			System.exit(1);
		}
		gui.completeLoad();
	}
}
