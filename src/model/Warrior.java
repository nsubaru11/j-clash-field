package model;

public class Warrior extends GameCharacter {
	private Vector2D GRAVITY = new Vector2D(0, -4.9);
	private double speedX = 5, speedY = 5;
	private CharacterType type = CharacterType.WARRIOR;

	@Override
	public CharacterType getType() {
		return type;
	}

	@Override
	public void normalAttack() {

	}

	@Override
	public void chargeAttack() {

	}

	@Override
	public void specialAttack() {

	}

	@Override
	public void defend() {

	}

}
