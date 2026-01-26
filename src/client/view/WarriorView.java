package client.view;

import model.CharacterType;

import java.awt.Color;

public class WarriorView extends CharacterView {
	private static final String SPRITE_SHEET = "/resorces/warrior_sprite_sheet.png";
	private static final Color ACCENT_COLOR = new Color(100, 200, 255);

	public WarriorView() {
		super(CharacterType.WARRIOR, SPRITE_SHEET, ACCENT_COLOR);
	}
}
