package client.model;

import model.CharacterType;

import java.awt.Color;

public class WizardSprite extends CharacterSprite {
	private static final String SPRITE_SHEET = "/resorces/wizard_sprite_sheet.png";
	public static final Color ACCENT_COLOR = CharacterType.WIZARD.getAccentColor();

	public WizardSprite() {
		super(CharacterType.WIZARD, SPRITE_SHEET, ACCENT_COLOR);
	}
}
