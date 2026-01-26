package model;

public  abstract class GameCharacter extends Entity {
	protected static final double DEFAULT_MOVE_STEP = 6.0;
	protected static final double DEFAULT_JUMP_VELOCITY = 14.0;
	protected static final double DEFAULT_PROJECTILE_SPEED = 9.0;
	protected static final double DEFAULT_ARROW_RANGE = 100.0;
	protected static final double DEFAULT_MAGIC_RANGE = 100.0;
	protected static final double DEFAULT_MELEE_WIDTH = 30.0;
	protected static final double DEFAULT_MELEE_HEIGHT = 30.0;
	protected static final double DEFAULT_MELEE_OFFSET = 30.0;
	protected static final int DEFAULT_MELEE_LIFETIME_TICKS = 2;
	protected static final long DEFAULT_MAX_CHARGE_MS = 1200;
	protected static final double DEFAULT_MAX_CHARGE_MULTIPLIER = 2.5;
	protected double speedX, speedY;
	protected double hp, attack, defend;
	protected CharacterType type;
	protected static final int MAX_JUMPS = 3;
	protected boolean grounded;
	protected int ownerId = -1;
	protected int jumpCount;

	protected GameCharacter() {
		position = new Vector2D(0, 0);
		width = new Vector2D(32, 0);
		height = new Vector2D(0, 32);
		velocity = new Vector2D(0, 0);
		speedX = DEFAULT_MOVE_STEP;
		speedY = DEFAULT_MOVE_STEP;
		hp = 100;
		attack = 10;
		defend = 0;
	}

	public abstract CharacterType getType();

	public abstract double getGravity();

	public abstract void normalAttack();

	public abstract void chargeAttack();

	public abstract void specialAttack();

	public abstract void defend();

	public double getMoveStepX() {
		return speedX;
	}

	public double getMoveStepY() {
		return speedY;
	}

	public double getJumpVelocity() {
		return DEFAULT_JUMP_VELOCITY;
	}

	public double getProjectileSpeed() {
		return DEFAULT_PROJECTILE_SPEED;
	}

	public ProjectileType getProjectileType() {
		return null;
	}

	public double getProjectileRange() {
		return 0;
	}

	public double getMeleeWidth() {
		return DEFAULT_MELEE_WIDTH;
	}

	public double getMeleeHeight() {
		return DEFAULT_MELEE_HEIGHT;
	}

	public double getMeleeOffset() {
		return DEFAULT_MELEE_OFFSET;
	}

	public int getMeleeLifetimeTicks() {
		return DEFAULT_MELEE_LIFETIME_TICKS;
	}

	public long getMaxChargeMs() {
		return DEFAULT_MAX_CHARGE_MS;
	}

	public double getMaxChargeMultiplier() {
		return DEFAULT_MAX_CHARGE_MULTIPLIER;
	}

	public boolean isRanged() {
		return getProjectileType() != null;
	}

	public void setPosition(double x, double y) {
		if (position == null) {
			position = new Vector2D(x, y);
		} else {
			position.setX(x);
			position.setY(y);
		}
	}

	public boolean isGrounded() {
		return grounded;
	}

	public void setGrounded(boolean grounded) {
		this.grounded = grounded;
	}

	public void setVerticalVelocity(double vy) {
		if (velocity == null) {
			velocity = new Vector2D(0, vy);
		} else {
			velocity.setY(vy);
		}
	}

	public boolean canJump() {
		return jumpCount < MAX_JUMPS;
	}

	public void registerJump() {
		jumpCount++;
		grounded = false;
	}

	public void resetJumpCount() {
		jumpCount = 0;
	}

	public int getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(int ownerId) {
		this.ownerId = ownerId;
	}

	public double getAttack() {
		return attack;
	}

	public int getHp() {
		return (int) Math.max(0, Math.round(hp));
	}

	public void setHp(int hp) {
		this.hp = Math.max(0, hp);
	}

	public int applyDamage(double damage) {
		double mitigated = Math.max(0, damage - defend);
		hp = Math.max(0, hp - mitigated);
		return getHp();
	}

	public void movLeft() {
		position.moveX(-speedX);
	}

	public void movRight() {
		position.moveX(speedX);
	}

	public void movUp() {
		position.moveY(speedY);
	}

	public void movDown() {
		position.moveY(-speedY);
	}
}
