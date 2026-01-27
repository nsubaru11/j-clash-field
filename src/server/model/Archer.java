package server.model;

import model.CharacterType;
import model.GameCharacter;
import model.ProjectileType;

public class Archer extends GameCharacter {
	private static final double GRAVITY = -0.7;

	public Archer() {
		super(CharacterType.ARCHER);
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
	public void specialAttack() {

	}

}
