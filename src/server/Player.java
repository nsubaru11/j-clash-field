package server;

import model.GameCharacter;

import java.util.logging.Logger;

public class Player {
	private static final Logger logger = Logger.getLogger(Player.class.getName());

	private int id;
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

	public void reset() {
		character = null;
		isReady = false;
	}

	public void setPlayerName(String name) {
		this.name = name;
	}

	public void selectCharacter(String characterName) {
		try {
			character = Class.forName(characterName).asSubclass(GameCharacter.class).newInstance();
		} catch (final Exception e) {
			logger.warning(() -> "プレイヤー" + name + "のキャラクター設定に失敗しました: " + characterName);
		}
	}

	public void unselectCharacter() {
		character = null;
	}

	public void setReady() {
		if (character == null) return;
		isReady = true;
	}

	public void setUnReady() {
		isReady = false;
	}

	public GameCharacter getCharacter() {
		return character;
	}

	public int getId() {
		return id;
	}

	public String toString() {
		return id + " " + name + " " + isReady + " " + (character != null ? character.getClass().getName() : "0");
	}
}
