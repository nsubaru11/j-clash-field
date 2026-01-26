package client.view;

import model.CharacterType;

import java.awt.Color;

public class ArcherView extends CharacterView {
	private static final String SPRITE_SHEET = "/resorces/archer_sprite_sheet.png";
	private static final Color ACCENT_COLOR = new Color(150, 50, 255);

	public ArcherView() {
		super(CharacterType.ARCHER, SPRITE_SHEET, ACCENT_COLOR);
	}
}
