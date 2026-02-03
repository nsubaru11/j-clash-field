package client.model;

import model.CharacterType;

import java.awt.*;

public class Wizard extends GameCharacterClient {
	public static final Color ACCENT_COLOR = CharacterType.WIZARD.getAccentColor();
	private static final String SPRITE_SHEET = "/resources/wizard_sprite_sheet.png";

	public Wizard() {
		super(CharacterType.WIZARD, SPRITE_SHEET, ACCENT_COLOR);
	}
}
