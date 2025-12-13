package server;

import model.GameCharacter;

public class Player {
	private String id;
	private String name;
	private boolean isReady; // ゲームロジックとしての状態
	GameCharacter character;

	public Player(String name) {
		this.name = name;
	}

	public boolean isReady() {
		return isReady;
	}

	public String getPlayerName() {
		return name;
	}


	public void setReady(boolean ready) {
		this.isReady = ready;
	}
}
