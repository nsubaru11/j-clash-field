package client.view;

import model.CharacterType;

public class WarriorView extends CharacterView {
	private static final String SPRITE_SHEET = "/resorces/warrior_sprite_sheet.png";

	public WarriorView() {
		super(CharacterType.WARRIOR, SPRITE_SHEET);
	}
}
