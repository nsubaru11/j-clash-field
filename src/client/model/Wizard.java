package client.model;

import model.CharacterType;

import java.awt.Color;

public class Wizard extends GameCharacterClient {
	private static final String SPRITE_SHEET = "/resources/wizard_sprite_sheet.png";
	public static final Color ACCENT_COLOR = CharacterType.WIZARD.getAccentColor();

	public Wizard() {
		super(CharacterType.WIZARD, SPRITE_SHEET, ACCENT_COLOR);
	}
}
