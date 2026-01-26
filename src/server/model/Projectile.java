package server.model;

import model.Entity;
import model.ProjectileType;
import model.Vector2D;

public final class Projectile extends Entity {
	private final ProjectileType type;
	private final int ownerId;
	private final double power;
	private final double damage;
	private final double maxDistance;
	private double traveledDistance;

	public Projectile(
			ProjectileType type,
			int ownerId,
			double x,
			double y,
			double velocityX,
			double velocityY,
			double power,
			double damage,
			double maxDistance
	) {
		this.type = type;
		this.ownerId = ownerId;
		this.power = power;
		this.damage = damage;
		this.maxDistance = maxDistance;
		position = new Vector2D(x, y);
		width = new Vector2D(16, 0);
		height = new Vector2D(0, 16);
		velocity = new Vector2D(velocityX, velocityY);
	}

	public ProjectileType getType() {
		return type;
	}

	public int getOwnerId() {
		return ownerId;
	}

	public double getPower() {
		return power;
	}

	public double getDamage() {
		return damage;
	}

	@Override
	public void update() {
		if (velocity != null) {
			traveledDistance += velocity.length();
		}
		super.update();
	}

	public boolean isExpired() {
		return maxDistance > 0 && traveledDistance >= maxDistance;
	}

	public boolean isOutOfBounds(int fieldWidth, int fieldHeight) {
		if (position == null) return true;
		return position.getX() < -20 || position.getX() > fieldWidth + 20
				|| position.getY() < -20 || position.getY() > fieldHeight + 20;
	}
}
