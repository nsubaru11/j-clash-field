package model;

public class Archer extends GameCharacter {
	private static final double GRAVITY = -0.7;
	private double speedX = 5, speedY = 5;
	private CharacterType type = CharacterType.ARCHER;

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
		return ProjectileType.ARROW;
	}

	@Override
	public double getProjectileRange() {
		return DEFAULT_ARROW_RANGE;
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
