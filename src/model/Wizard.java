package model;

public class Wizard extends GameCharacter {
	private static final double GRAVITY = -0.5;
	private double speedX = 5, speedY = 5;
	private CharacterType type = CharacterType.WIZARD;

	@Override
	public CharacterType getType() {
		return type;
	}

	@Override
	public double getGravity() {
		return GRAVITY;
	}

	@Override
	public ProjectileType getProjectileType() {
		return ProjectileType.MAGIC;
	}

	@Override
	public double getProjectileRange() {
		return DEFAULT_MAGIC_RANGE;
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
