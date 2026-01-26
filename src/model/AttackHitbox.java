package model;

/**
 * 近接攻撃の当たり判定用
 */
public final class AttackHitbox extends Entity {
	private final int ownerId;
	private final double damage;
	private final double maxDistance;
	private int remainingTicks;
	private double traveledDistance;

	public AttackHitbox(
			int ownerId,
			double damage,
			double x,
			double y,
			double width,
			double height,
			double velocityX,
			double velocityY,
			double maxDistance,
			int lifetimeTicks
	) {
		this.ownerId = ownerId;
		this.damage = damage;
		this.maxDistance = maxDistance;
		this.remainingTicks = lifetimeTicks;
		position = new Vector2D(x, y);
		this.width = new Vector2D(Math.abs(width), 0);
		this.height = new Vector2D(0, Math.abs(height));
		velocity = new Vector2D(velocityX, velocityY);
	}

	@Override
	public void update() {
		if (velocity != null) {
			double step = velocity.length();
			traveledDistance += step;
		}
		super.update();
		remainingTicks--;
	}

	public int getOwnerId() {
		return ownerId;
	}

	public double getDamage() {
		return damage;
	}

	public boolean isExpired() {
		if (remainingTicks <= 0) return true;
		return maxDistance > 0 && traveledDistance >= maxDistance;
	}
}
