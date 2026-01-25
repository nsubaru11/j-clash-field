package model;

public enum CharacterType {
	NONE(0),
	ARCHER(1),
	WARRIOR(2),
	FIGHTER(3),
	WIZARD(4);

	private static final CharacterType[] BY_ID;

	static {
		int maxId = 0;
		for (CharacterType type : values()) {
			if (type.id > maxId) maxId = type.id;
		}
		BY_ID = new CharacterType[maxId + 1];
		for (CharacterType type : values()) {
			BY_ID[type.id] = type;
		}
	}

	private final int id;

	CharacterType(int networkId) {
		this.id = networkId;
	}

	public static CharacterType fromId(int id) {
		return BY_ID[id];
	}

	public static GameCharacter createCharacter(CharacterType type) {
		switch (type) {
			case ARCHER:
				return new Archer();
			case WARRIOR:
				return new Warrior();
			case FIGHTER:
				return new Fighter();
			case WIZARD:
				return new Wizard();
			default:
				return null;
		}
	}

	public int getId() {
		return id;
	}
}
