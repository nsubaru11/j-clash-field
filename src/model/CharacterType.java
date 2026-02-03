package model;

import java.awt.*;

public enum CharacterType {
	ARCHER(0),
	WARRIOR(1),
	FIGHTER(2),
	WIZARD(3);

	public static final int DEFAULT_ID = 0;
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

	public static CharacterType defaultType() {
		return fromId(DEFAULT_ID);
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
