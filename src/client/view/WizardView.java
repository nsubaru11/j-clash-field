package client.view;

import model.CharacterType;

public class WizardView extends CharacterView {
	private static final String SPRITE_SHEET = "/resorces/wizard_sprite_sheet.png";

	public WizardView() {
		super(CharacterType.WIZARD, SPRITE_SHEET);
	}
}
