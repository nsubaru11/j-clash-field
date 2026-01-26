package client.view;

import model.CharacterType;

import java.awt.Color;

public class WizardView extends CharacterView {
	private static final String SPRITE_SHEET = "/resorces/wizard_sprite_sheet.png";
	private static final Color ACCENT_COLOR = new Color(90, 0, 140);

	public WizardView() {
		super(CharacterType.WIZARD, SPRITE_SHEET, ACCENT_COLOR);
	}
}
