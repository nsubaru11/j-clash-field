package client.view;

import model.CharacterType;

public class FighterView extends CharacterView {
	private static final String SPRITE_SHEET = "/resorces/fighter_sprite_sheet.png";

	public FighterView() {
		super(CharacterType.FIGHTER, SPRITE_SHEET);
	}
}
