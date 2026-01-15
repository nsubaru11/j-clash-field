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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public final class GuiController extends Thread {
	private static final Logger logger = Logger.getLogger(GuiController.class.getName());
	/** タイトル画面の識別子 */
	private static final String CARD_TITLE = "title";
	/** ロード画面の識別子 */
	private static final String CARD_LOAD = "load";
	/** ホーム画面の識別子 */
	private static final String CARD_HOME = "home";
	/** マッチング画面の識別子 */
	private static final String CARD_MATCHING = "matching";
	/** ゲーム部屋の識別子 */
	private static final String CARD_GAME_ROOM = "game_room";
	/** ゲーム画面の識別子 */
	private static final String CARD_GAME = "game";
	/** 結果画面の識別子 */
	private static final String CARD_RESULT = "result";
	private static final int FPS = 60;
	private static final long FRAME_TIME = 1000_000_000L / FPS;

	/** 画面切り替えレイアウトマネージャ */
	private final CardLayout cardLayout;
	/** 各画面パネルを保持する親パネル */
	private final JPanel cardPanel;
	/** タイトル画面パネル */
	private final TitlePanel titlePanel;
	/** ロード画面パネル */
	private final LoadPanel loadPanel;
	/** ホーム画面パネル */
	private final HomePanel homePanel;
	/** マッチング設定パネル */
	private final MatchingPanel matchingPanel;
	/** ゲーム部屋画面パネル */
	private final GameRoomPanel gameRoomPanel;
	/** ゲーム画面パネル */
	private final GamePanel gamePanel;
	/** 結果画面パネル */
	private final ResultPanel resultPanel;
	private final JLayeredPane rootPane;
	/** ネットワークコントローラー */
	private final NetworkController network;
	private final ConcurrentLinkedQueue<Command> commandQueue;

	/** 現在の画面の Id */
	private String currentCardId = CARD_TITLE;

	public GuiController(NetworkController network) {
		this.network = network;
		cardLayout = new CardLayout();
		cardPanel = new JPanel(cardLayout);

		titlePanel = new TitlePanel(this::startConnection);
		loadPanel = new LoadPanel();
		homePanel = new HomePanel(e -> showMatching(), e -> System.exit(0));
		matchingPanel = new MatchingPanel();
		matchingPanel.setStartGameListener(e -> joinRoom());
		gameRoomPanel = new GameRoomPanel();
		gamePanel = new GamePanel();
		resultPanel = new ResultPanel();

		rootPane = new JLayeredPane();
		rootPane.setLayout(new OverlayLayout(rootPane));
		rootPane.add(loadPanel, JLayeredPane.PALETTE_LAYER);
		rootPane.add(cardPanel, JLayeredPane.DEFAULT_LAYER);

		cardPanel.add(titlePanel, CARD_TITLE);
		cardPanel.add(homePanel, CARD_HOME);
		cardPanel.add(matchingPanel, CARD_MATCHING);
		cardPanel.add(gameRoomPanel, CARD_GAME_ROOM);
		cardPanel.add(gamePanel, CARD_GAME);
		cardPanel.add(resultPanel, CARD_RESULT);

		GameGUI gui = new GameGUI(rootPane);
		gui.setVisible(true);
		commandQueue = new ConcurrentLinkedQueue<>();
	}

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

	private synchronized void startConnection() {
		network.setMessageListener(msg -> this.commandQueue.add(new Command(msg)));
		Runtime.getRuntime().addShutdownHook(new Thread(network::close));
		showLoad();
		logger.info("接続を開始します...");
		if (!network.connect()) {
			logger.severe("接続に失敗しました。");
			System.exit(1);
		}
		start();
		logger.info("接続に成功しました。");
		loadPanel.setNextScreen(this::showHome);
		completeLoad();
	}

	private synchronized void joinRoom() {
		String userName = matchingPanel.getUserName();
		int roomId = matchingPanel.getRoomId();
		network.joinRoom(userName, roomId);
	}

	private synchronized void showHome() {
		cardLayout.show(cardPanel, CARD_HOME);
		currentCardId = CARD_HOME;
	}

	private synchronized void showLoad() {
		loadPanel.startLoading();
		currentCardId = CARD_LOAD;
	}

	private synchronized void showMatching() {
		cardLayout.show(cardPanel, CARD_MATCHING);
		currentCardId = CARD_MATCHING;
	}

	private synchronized void showGameRoom() {
		cardLayout.show(cardPanel, CARD_GAME_ROOM);
		currentCardId = CARD_GAME_ROOM;
	}

	private synchronized void showGame() {
		cardLayout.show(cardPanel, CARD_GAME);
		currentCardId = CARD_GAME;
	}

	private synchronized void showResult() {
		cardLayout.show(cardPanel, CARD_RESULT);
		currentCardId = CARD_RESULT;
	}

	private synchronized void completeLoad() {
		loadPanel.completeLoading();
	}

	private synchronized void handleCommand(Command command) {
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
				loadPanel.setNextScreen(this::showGameRoom);
				completeLoad();
				break;
			case JOIN_FAILED:
				loadPanel.setNextScreen(this::showMatching);
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

}
