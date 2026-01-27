package model;

import java.awt.Color;

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

	public int getId() {
		return id;
	}

	public CharacterInfo getInfo() {
		return CharacterInfo.forType(this);
	}

	public String getName() {
		return getInfo().getName();
	}

	public Color getAccentColor() {
		return getInfo().getThemeColor();
	}
}
