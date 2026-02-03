package client.model;

import model.CharacterType;

import java.awt.*;

public class Warrior extends GameCharacterClient {
	public static final Color ACCENT_COLOR = CharacterType.WARRIOR.getAccentColor();
	private static final String SPRITE_SHEET = "/resources/warrior_sprite_sheet.png";

	public Warrior() {
		super(CharacterType.WARRIOR, SPRITE_SHEET, ACCENT_COLOR);
	}
}
