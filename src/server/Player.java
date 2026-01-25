package server;

import model.Archer;
import model.CharacterType;
import model.Fighter;
import model.GameCharacter;
import model.Warrior;
import model.Wizard;

import java.util.logging.Logger;

public class Player {
	private static final Logger logger = Logger.getLogger(Player.class.getName());

	private int id;
	private String name;
	private boolean isReady; // ゲームロジックとしての状態
	private GameCharacter character;

	public Player(int id, String name) {
		this.id = id;
		this.name = name;
		this.character = new Archer();
	}

	public boolean isReady() {
		return isReady;
	}

	public String getPlayerName() {
		return name;
	}

	public void reset() {
		character = new Archer();
		isReady = false;
	}

	public void setPlayerName(String name) {
		this.name = name;
	}

	public void selectCharacter(CharacterType characterType) {
		try {
			switch (characterType) {
				case ARCHER:
					character = new Archer();
					break;
				case WARRIOR:
					character = new Warrior();
					break;
				case FIGHTER:
					character = new Fighter();
					break;
				case WIZARD:
					character = new Wizard();
					break;
				case NONE:
				default:
					character = new Archer();
					break;
			}
		} catch (final Exception e) {
			logger.warning(() -> "プレイヤー" + name + "のキャラクター設定に失敗しました: " + characterType);
		}
	}

	public void unselectCharacter() {
		character = new Archer();
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
		return id + " " + name + " " + isReady + " " + character.getType().getId();
	}
}
