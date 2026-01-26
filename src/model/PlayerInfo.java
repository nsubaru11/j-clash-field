package model;

public class PlayerInfo {
	private final int id;
	private String name;
	private boolean ready;
	private GameCharacter character;

	public PlayerInfo(int id, String name, boolean ready, GameCharacter character) {
		this.id = id;
		this.name = name == null ? "" : name;
		this.ready = ready;
		this.character = character;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name == null ? "" : name;
	}

	public boolean isReady() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	public GameCharacter getCharacter() {
		return character;
	}

	public void setCharacter(GameCharacter character) {
		this.character = character;
	}
}
