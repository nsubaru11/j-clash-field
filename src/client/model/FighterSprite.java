package client.model;

import model.CharacterType;

import java.awt.Color;

public class FighterSprite extends CharacterSprite {
	private static final String SPRITE_SHEET = "/resorces/fighter_sprite_sheet.png";
	public static final Color ACCENT_COLOR = CharacterType.FIGHTER.getAccentColor();

	public FighterSprite() {
		super(CharacterType.FIGHTER, SPRITE_SHEET, ACCENT_COLOR);
	}
}
