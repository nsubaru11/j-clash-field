package client.model;

import model.CharacterType;

import java.awt.*;

public class Archer extends GameCharacterClient {
	public static final Color ACCENT_COLOR = CharacterType.ARCHER.getAccentColor();
	private static final String SPRITE_SHEET = "/resources/archer_sprite_sheet.png";

	public Archer() {
		super(CharacterType.ARCHER, SPRITE_SHEET, ACCENT_COLOR);
	}

}
