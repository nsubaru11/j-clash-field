package client.model;

import model.CharacterType;

import java.awt.Color;

public class WarriorSprite extends CharacterSprite {
	private static final String SPRITE_SHEET = "/resorces/warrior_sprite_sheet.png";
	public static final Color ACCENT_COLOR = CharacterType.WARRIOR.getAccentColor();

	public WarriorSprite() {
		super(CharacterType.WARRIOR, SPRITE_SHEET, ACCENT_COLOR);
	}
}
