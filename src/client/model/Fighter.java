package client.model;

import model.CharacterType;

import java.awt.Color;

public class Fighter extends GameCharacterClient {
	private static final String SPRITE_SHEET = "/resources/fighter_sprite_sheet.png";
	public static final Color ACCENT_COLOR = CharacterType.FIGHTER.getAccentColor();

	public Fighter() {
		super(CharacterType.FIGHTER, SPRITE_SHEET, ACCENT_COLOR);
	}
}
