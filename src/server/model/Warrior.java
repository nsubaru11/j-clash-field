package server.model;

import model.CharacterType;
import model.GameCharacter;
import model.Vector2D;

public class Warrior extends GameCharacter implements MeleeAttacker {
	private static final double GRAVITY = -1.5;

	public Warrior() {
		super(CharacterType.WARRIOR);
	}

	@Override
	public double getGravity() {
		return GRAVITY;
	}

	@Override
	public void strike(BattleField field) {
		if (field == null) return;
		double power = getAttackPowerRatio();
		double width = getMeleeWidth() * Math.min(1.4, power);
		double height = getMeleeHeight() * Math.min(1.3, power);
		double offset = getMeleeOffset();
		int lifetime = getMeleeLifetimeTicks();
		double damage = getAttack();
		double baseX = getPosition().getX();
		double baseY = getPosition().getY();
		Vector2D facing = getFacingDirection();
		double offsetX = facing.getX() * offset;
		double offsetY = facing.getY() * offset;
		AttackHitbox front = new AttackHitbox(
				getOwnerId(),
				damage,
				baseX + offsetX,
				baseY + offsetY,
				width,
				height,
				0,
				0,
				0,
				lifetime
		);
		AttackHitbox back = new AttackHitbox(
				getOwnerId(),
				damage,
				baseX - offsetX,
				baseY - offsetY,
				width,
				height,
				0,
				0,
				0,
				lifetime
		);
		field.addEntity(front);
		field.addEntity(back);
	}

	@Override
	public void specialAttack() {
	}

}
