package client.controller;

import client.view.GameGUI;
import client.view.GamePanel;
import client.view.GameRoomPanel;
import client.view.HomePanel;
import client.view.LoadPanel;
import client.view.MatchingPanel;
import client.view.ResultPanel;
import model.Command;
import model.LoggingConfig;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

public final class GuiController {
	private static final Logger logger = Logger.getLogger(GuiController.class.getName());
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

	/** 画面切り替えレイアウトマネージャ */
	private final CardLayout cardLayout;
	/** 各画面パネルを保持する親パネル */
	private final JPanel cardPanel;
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
	/** ネットワークコントローラー */
	private final NetworkController network;

	/** 現在の画面の Id */
	private String currentCardId;

	public GuiController(NetworkController network) {
		cardLayout = new CardLayout();
		cardPanel = new JPanel(cardLayout);

		loadPanel = new LoadPanel();
		homePanel = new HomePanel(e -> showMatching(), e -> System.exit(0));
		matchingPanel = new MatchingPanel(e -> showGameRoom(), e -> showHome());
		gameRoomPanel = new GameRoomPanel();
		gamePanel = new GamePanel();
		resultPanel = new ResultPanel();
		this.network = network;

		cardPanel.add(loadPanel, CARD_LOAD);
		cardPanel.add(homePanel, CARD_HOME);
		cardPanel.add(matchingPanel, CARD_MATCHING);
		cardPanel.add(gameRoomPanel, CARD_GAME_ROOM);
		cardPanel.add(gamePanel, CARD_GAME);
		cardPanel.add(resultPanel, CARD_RESULT);

		GameGUI gui = new GameGUI(cardPanel);
		gui.setVisible(true);
	}

	public void start() {
		network.start();
		network.setMessageListener(s -> handleMessage(new Command(s)));
		Runtime.getRuntime().addShutdownHook(new Thread(network::close));
		showLoad();
		if (!network.connect()) {
			logger.severe("接続に失敗しました。");
			System.exit(1);
		}
		completeLoad();
	}

	public void showHome() {
		cardLayout.show(cardPanel, CARD_HOME);
		currentCardId = CARD_HOME;
	}

	public void showLoad() {
		Runnable onFinish;
		switch (currentCardId) {
			case CARD_RESULT:
			case CARD_MATCHING:
				onFinish = this::showGameRoom;
				break;
			case CARD_GAME_ROOM:
				onFinish = this::showGame;
				break;
			case CARD_GAME:
				onFinish = this::showResult;
				break;
			default:
				onFinish = this::showHome;
				break;
		}
		loadPanel.setLoaded(onFinish);
		cardLayout.show(cardPanel, CARD_LOAD);
		loadPanel.startLoading();
		currentCardId = CARD_LOAD;
	}

	public void showMatching() {
		cardLayout.show(cardPanel, CARD_MATCHING);
		currentCardId = CARD_MATCHING;
	}

	public void showGameRoom() {
		cardLayout.show(cardPanel, CARD_GAME_ROOM);
		currentCardId = CARD_GAME_ROOM;
	}

	public void showGame() {
		cardLayout.show(cardPanel, CARD_GAME);
		currentCardId = CARD_GAME;
	}

	public void showResult() {
		cardLayout.show(cardPanel, CARD_RESULT);
		currentCardId = CARD_RESULT;
	}

	public void completeLoad() {
		loadPanel.completeLoading();
	}

	public void handleMessage(Command message) {
		logger.info("受信したメッセージ: " + message);
	}

}
