package client.model;

import model.CharacterType;

import java.awt.Color;

public class ArcherSprite extends CharacterSprite {
	private static final String SPRITE_SHEET = "/resources/archer_sprite_sheet.png";
	public static final Color ACCENT_COLOR = CharacterType.ARCHER.getAccentColor();

	public ArcherSprite() {
		super(CharacterType.ARCHER, SPRITE_SHEET, ACCENT_COLOR);
	}

}
