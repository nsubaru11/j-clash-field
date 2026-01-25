package client.view;

import model.CharacterType;

public class ArcherView extends CharacterView {
	private static final String SPRITE_SHEET = "/resorces/archer_sprite_sheet.png";

	public ArcherView() {
		super(CharacterType.ARCHER, SPRITE_SHEET);
	}
}
