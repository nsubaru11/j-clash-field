package server.model;

import model.CharacterType;
import model.GameCharacter;

public class Fighter extends GameCharacter {
	private static final double GRAVITY = -1.3;

	public Fighter() {
		super(CharacterType.FIGHTER);
	}

	@Override
	public double getGravity() {
		return GRAVITY;
	}

	@Override
	public void specialAttack() {

	}

}
