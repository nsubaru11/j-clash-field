package client;

import client.controller.GuiController;
import client.controller.NetworkController;
import model.LoggingConfig;

import javax.swing.*;
import java.util.logging.Logger;

public final class ClientMain {
	private static final Logger logger = Logger.getLogger(ClientMain.class.getName());
	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 10000;
	// private static final String DEFAULT_HOST = "";

	private ClientMain() {
	}

	public static void main(final String[] args) {
		LoggingConfig.initialize("client");

		int len = args.length;
		String host = len == 0 ? DEFAULT_HOST : args[0];
		int port = len <= 1 ? DEFAULT_PORT : Integer.parseInt(args[1]);

		NetworkController network = new NetworkController(host, port);
		SwingUtilities.invokeLater(() -> {
			new GuiController(network);
			logger.info("ゲームが正常に開始されました。");
		});
	}
}
