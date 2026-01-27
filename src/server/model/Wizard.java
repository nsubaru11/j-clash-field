package server.model;

import model.CharacterType;
import model.GameCharacter;
import model.ProjectileType;

public class Wizard extends GameCharacter {
	private static final double GRAVITY = -0.5;

	public Wizard() {
		super(CharacterType.WIZARD);
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
	public void specialAttack() {

	}

}
