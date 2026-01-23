package client.controller;

import client.view.GameGUI;
import client.view.GamePanel;
import client.view.GameRoomPanel;
import client.view.HomePanel;
import client.view.LoadPanel;
import client.view.MatchingPanel;
import client.view.ResultPanel;
import client.view.TitlePanel;
import model.Command;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public final class GuiController {
	// -------------------- クラス定数 --------------------
	private static final Logger logger = Logger.getLogger(GuiController.class.getName());
	private static final String CARD_TITLE = "title";
	private static final String CARD_LOAD = "load";
	private static final String CARD_HOME = "home";
	private static final String CARD_GAME_ROOM = "game_room";
	private static final String CARD_GAME = "game";
	private static final String CARD_RESULT = "result";

	// -------------------- インスタンス定数 --------------------
	/** ルートパネル */
	private final JLayeredPane rootPane;
	/** 画面切り替えレイアウトマネージャ */
	private final CardLayout cardLayout;
	/** 各画面パネルを保持する親パネル */
	private final JPanel cardPanel;

	private final TitlePanel titlePanel;
	private final LoadPanel loadPanel;
	private final HomePanel homePanel;
	private final MatchingPanel matchingPanel;
	private final GameRoomPanel gameRoomPanel;
	private final GamePanel gamePanel;
	private final ResultPanel resultPanel;
	private volatile MatchingPanel.MatchingMode lastMatchingMode = MatchingPanel.MatchingMode.RANDOM;

	/** ネットワークコントローラー */
	private final NetworkController network;
	/** コマンドキュー */
	private final ConcurrentLinkedQueue<Command> commandQueue;

	public GuiController(NetworkController network) {
		this.network = network;
		cardLayout = new CardLayout();
		cardPanel = new JPanel(cardLayout);

		titlePanel = new TitlePanel(this::startConnection);
		loadPanel = new LoadPanel();
		homePanel = new HomePanel(this::showMatching, e -> {
			int result = JOptionPane.showConfirmDialog(cardPanel, "終了します。よろしいですか？", "終了確認", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				network.disconnect();
				System.exit(0);
			}
		});
		matchingPanel = new MatchingPanel();
		matchingPanel.setVisible(false);
		matchingPanel.setStartGameListener(e -> joinRoom());
		matchingPanel.setCancelListener(e -> showHome());
		gameRoomPanel = new GameRoomPanel();
		gamePanel = new GamePanel();
		resultPanel = new ResultPanel();

		rootPane = new JLayeredPane();
		rootPane.setLayout(new OverlayLayout(rootPane));
		rootPane.add(cardPanel, JLayeredPane.DEFAULT_LAYER);
		rootPane.add(matchingPanel, JLayeredPane.PALETTE_LAYER);
		rootPane.add(loadPanel, JLayeredPane.MODAL_LAYER);

		cardPanel.add(titlePanel, CARD_TITLE);
		cardPanel.add(homePanel, CARD_HOME);
		cardPanel.add(gameRoomPanel, CARD_GAME_ROOM);
		cardPanel.add(gamePanel, CARD_GAME);
		cardPanel.add(resultPanel, CARD_RESULT);

		GameGUI gui = new GameGUI(rootPane);
		gui.setVisible(true);
		commandQueue = new ConcurrentLinkedQueue<>();
	}

	private void startConnection() {
		network.setMessageListener(msg -> this.commandQueue.add(new Command(msg)));
		Runtime.getRuntime().addShutdownHook(new Thread(network::close));
		showLoad();
		network.connect(() -> {
			new GameLoopThread().start();
			loadPanel.setNextScreen(this::showHome);
			completeLoad();
		}, () -> SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(cardPanel, "サーバーに接続できませんでした。", "接続エラー", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}));
	}

	private synchronized void joinRoom() {
		String userName = matchingPanel.getUserName();
		MatchingPanel.MatchingMode mode = matchingPanel.getCurrentMode();
		matchingPanel.setVisible(false);
		showLoad();
		loadPanel.setNextScreen(this::showGameRoom);
		switch (mode) {
			case CREATE:
				network.createRoom(userName);
				break;
			case JOIN:
				network.joinRoom(userName, matchingPanel.getRoomId());
				break;
			case RANDOM:
			default:
				network.joinRoom(userName, -1);
				break;
		}
	}

	private void showHome() {
		SwingUtilities.invokeLater(() -> {
			matchingPanel.reset();
			matchingPanel.setVisible(false);
			cardLayout.show(cardPanel, CARD_HOME);
		});
	}

	private void showLoad() {
		loadPanel.startLoading();
	}

	private void showMatching(MatchingPanel.MatchingMode mode) {
		lastMatchingMode = mode;
		SwingUtilities.invokeLater(() -> {
			matchingPanel.setupForMode(mode);
			matchingPanel.reset();
			matchingPanel.setVisible(true);
		});
	}

	private void showGameRoom() {
		SwingUtilities.invokeLater(() -> cardLayout.show(cardPanel, CARD_GAME_ROOM));
	}

	private void showGame() {
		SwingUtilities.invokeLater(() -> cardLayout.show(cardPanel, CARD_GAME));
	}

	private void showResult() {
		SwingUtilities.invokeLater(() -> cardLayout.show(cardPanel, CARD_RESULT));
	}

	private void completeLoad() {
		loadPanel.completeLoading();
	}

	private void handleCommand(Command command) {
		// TODO: コマンド処理
		switch (command.getCommandType()) {
			case GAME_START:
				break;
			case GAME_OVER:
				break;
			case MOVE:
				break;
			case DAMAGE:
				break;
			case DEAD:
				break;
			case OPPONENT_RESIGNED:
				break;
			case OPPONENT_DISCONNECTED:
				break;
			case JOIN_SUCCESS:
				String[] roomState = command.getBody().split(":");
				String[] roomInfo = roomState[0].split(",");
				int roomId = Integer.parseInt(roomInfo[0]);
				boolean isPublic = roomInfo.length > 2 && Boolean.parseBoolean(roomInfo[1]);
				int playerCount = Integer.parseInt(roomInfo[roomInfo.length > 2 ? 2 : 1]);
				HashMap<Integer, String[]> playerMap = new HashMap<>();
				for (String playerInfo : roomState[1].split(",")) {
					String[] playerInfoArray = playerInfo.split(",");
					playerMap.put(Integer.parseInt(playerInfoArray[0]), playerInfoArray);
				}
				loadPanel.setNextScreen(this::showGameRoom);
				completeLoad();
				break;
			case JOIN_FAILED:
				loadPanel.setNextScreen(() -> showMatching(lastMatchingMode));
				completeLoad();
				break;
			case RESULT:
				break;
			case GAME_ROOM_CLOSED:
				break;
			case SERVER_CLOSED:
				break;
			default:
				break;
		}
	}

	private final class GameLoopThread extends Thread {
		private static final int FPS = 60;
		private static final long FRAME_TIME = 1000_000_000L / FPS;

		@Override
		public void run() {
			long targetTime = System.nanoTime();
			while (true) {
				targetTime += FRAME_TIME;
				while (!commandQueue.isEmpty()) {
					Command cmd = commandQueue.poll();
					handleCommand(cmd);
				}
				long waitNs = targetTime - System.nanoTime();
				if (waitNs > 0) {
					long waitMs = waitNs / 1_000_000;
					int waitNsRest = (int) (waitNs % 1_000_000);
					try {
						// noinspection BusyWait
						Thread.sleep(waitMs, waitNsRest);
					} catch (InterruptedException e) {
						logger.warning("");
						break;
					}
				} else {
					logger.fine("処理落ち発生: " + waitNs + "ns");
					targetTime -= waitNs;
				}
			}
		}
	}

}
