package client.model;

import model.CharacterType;

import java.awt.Color;

public class Warrior extends GameCharacterClient {
	private static final String SPRITE_SHEET = "/resources/warrior_sprite_sheet.png";
	public static final Color ACCENT_COLOR = CharacterType.WARRIOR.getAccentColor();

	public Warrior() {
		super(CharacterType.WARRIOR, SPRITE_SHEET, ACCENT_COLOR);
	}
}
