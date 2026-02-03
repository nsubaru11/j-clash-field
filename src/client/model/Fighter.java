package client.model;

import model.CharacterType;

import java.awt.*;

public class Fighter extends GameCharacterClient {
	public static final Color ACCENT_COLOR = CharacterType.FIGHTER.getAccentColor();
	private static final String SPRITE_SHEET = "/resources/fighter_sprite_sheet.png";

	public Fighter() {
		super(CharacterType.FIGHTER, SPRITE_SHEET, ACCENT_COLOR);
	}
}
