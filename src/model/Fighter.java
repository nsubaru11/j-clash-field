package model;

public class Fighter extends GameCharacter {
	private static final double GRAVITY = -1.3;
	private double speedX = 5, speedY = 5;
	private CharacterType type = CharacterType.FIGHTER;

	@Override
	public CharacterType getType() {
		return type;
	}

	@Override
	public double getGravity() {
		return GRAVITY;
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
