package server.model;

import model.CharacterType;
import model.GameCharacter;
import model.ProjectileType;
import model.Vector2D;

public class Wizard extends GameCharacter implements RangedAttacker {
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
	public void shoot(BattleField field) {
		if (field == null) return;
		double maxDistance = getProjectileRange();
		if (maxDistance <= 0) return;
		Vector2D facing = getFacingDirection();
		double power = getAttackPowerRatio();
		double speed = getProjectileSpeed() * Math.max(1.0, power);
		double damage = getAttack();
		double startX = getPosition().getX() + (facing.getX() * 16);
		double startY = getPosition().getY() + 35 + (facing.getY() * 16);
		Projectile projectile = new Projectile(
				getProjectileType(),
				getOwnerId(),
				startX,
				startY,
				facing.getX() * speed,
				facing.getY() * speed,
				power,
				damage,
				maxDistance
		);
		field.addEntity(projectile);
	}

	@Override
	public void specialAttack() {
	}

}
