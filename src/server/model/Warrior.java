package server.model;

import model.CharacterType;
import model.GameCharacter;

public class Warrior extends GameCharacter {
	private static final double GRAVITY = -1.5;

	public Warrior() {
		super(CharacterType.WARRIOR);
	}

	@Override
	public double getGravity() {
		return GRAVITY;
	}

	@Override
	public void specialAttack() {

	}

}
