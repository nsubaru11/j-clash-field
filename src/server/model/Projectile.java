package server.model;

import model.Entity;
import model.ProjectileType;
import model.Vector2D;

public final class Projectile extends Entity {
	private final ProjectileType type;
	private final int ownerId;
	private final double power; // チャージ倍率
	private final double damage; // 基本ダメージ
	private final double baseSpeed; // 初速
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
		width = new Vector2D(10, 0);
		height = new Vector2D(0, 10);
		velocity = new Vector2D(velocityX, velocityY);
		this.baseSpeed = velocity.length();
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
		double currentSpeed = velocity.length();
		double ratio = currentSpeed / baseSpeed;
		return Math.max(0, damage * ratio * power);
	}

	@Override
	public boolean collidesWith(Entity other) {
		double widthValue = getWidthValue();
		double heightValue = getHeightValue();
		double otherWidth = other.getWidthValue();
		double otherHeight = other.getHeightValue();
		if (widthValue <= 0 || heightValue <= 0 || otherWidth <= 0 || otherHeight <= 0) return false;

		double thisMinX = position.getX() - widthValue / 2.0;
		double thisMaxX = position.getX() + widthValue / 2.0;
		double thisMinY = position.getY() - heightValue / 2.0;
		double thisMaxY = position.getY() + heightValue / 2.0;

		double otherMinX = other.getPosition().getX() - otherWidth / 2.0;
		double otherMaxX = other.getPosition().getX() + otherWidth / 2.0;
		double otherMinY;
		double otherMaxY;
		if (other instanceof Projectile) {
			otherMinY = other.getPosition().getY() - otherHeight / 2.0;
			otherMaxY = other.getPosition().getY() + otherHeight / 2.0;
		} else {
			otherMinY = other.getPosition().getY();
			otherMaxY = other.getPosition().getY() + otherHeight;
		}

		return thisMinX < otherMaxX && thisMaxX > otherMinX && thisMinY < otherMaxY && thisMaxY > otherMinY;
	}

	@Override
	public void update() {
		traveledDistance += velocity.length();
		super.update();
	}

	public boolean isExpired() {
		return maxDistance > 0 && traveledDistance >= maxDistance;
	}

	public boolean isOutOfBounds(int fieldWidth, int fieldHeight) {
		return position.getX() < -20 || position.getX() > fieldWidth + 20
				|| position.getY() < -20 || position.getY() > fieldHeight + 20;
	}
}
