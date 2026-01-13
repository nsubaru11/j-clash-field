package server;

import model.GameCharacter;

import java.util.logging.Logger;

public class Player {
	private static final Logger logger = Logger.getLogger(Player.class.getName());

	private String id;
	private String name;
	private boolean isReady; // ゲームロジックとしての状態
	private GameCharacter character;


	public Player(String name) {
		this.name = name;
	}

	public boolean isReady() {
		return isReady;
	}

	public String getPlayerName() {
		return name;
	}

	public void setPlayerName(String name) {
		this.name = name;
	}

	public void setReady(final String characterName) {
		try {
			isReady = true;
			character = Class.forName(characterName).asSubclass(GameCharacter.class).newInstance();
		} catch (final Exception e) {
			logger.warning(() -> "プレイヤー" + name + "のキャラクター設定に失敗しました: " + characterName);
		}
	}

	public void setUnReady() {
		isReady = false;
		character = null;
	}

	public GameCharacter getCharacter() {
		return character;
	}
}
