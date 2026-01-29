package model;

public abstract class GameCharacter extends Entity {
	protected static final double DEFAULT_JUMP_VELOCITY = 14.0;
	protected static final int MAX_JUMPS = 3;

	protected double speedX;
	protected double speedY;
	protected double hp;
	protected double attack;
	protected double defend;
	protected double attackMin;
	protected double attackMax;
	protected double attackChargeTimeMs;
	protected double defenseChargeTimeMs;
	protected double projectileSpeed;
	protected double projectileRange;
	protected double meleeWidth;
	protected double meleeHeight;
	protected double meleeOffset;
	protected int meleeLifetimeTicks;
	protected CharacterType type;
	protected boolean grounded;
	protected int ownerId = -1;
	protected int jumpCount;

	private boolean defending;
	private long defenseRemainingMs;
	private long lastDefenseTickMs = -1;

	protected GameCharacter() {
		this(CharacterType.defaultType());
	}

	protected GameCharacter(CharacterType type) {
		position = new Vector2D(0, 0);
		width = new Vector2D(96, 0);
		height = new Vector2D(0, 96);
		velocity = new Vector2D(0, 0);
		applyCharacterInfo(type);
	}

	protected final void applyCharacterInfo(CharacterType type) {
		this.type = type == null ? CharacterType.defaultType() : type;
		CharacterInfo info = CharacterInfo.forType(this.type);
		attackMin = Math.max(0.0, info.getAttackMin());
		attackMax = Math.max(attackMin, info.getAttackMax());
		attackChargeTimeMs = Math.max(0.0, info.getAttackChargeTimeMs());
		defend = Math.max(0.0, info.getDefense());
		defenseChargeTimeMs = Math.max(0.0, info.getDefenseChargeTimeMs());
		hp = Math.max(0.0, info.getHp());
		speedX = Math.max(0.0, info.getMoveStepX());
		speedY = Math.max(0.0, info.getMoveStepY());
		projectileSpeed = Math.max(0.0, info.getProjectileSpeed());
		projectileRange = Math.max(0.0, info.getProjectileRange());
		meleeWidth = Math.max(0.0, info.getMeleeWidth());
		meleeHeight = Math.max(0.0, info.getMeleeHeight());
		meleeOffset = Math.max(0.0, info.getMeleeOffset());
		meleeLifetimeTicks = Math.max(0, info.getMeleeLifetimeTicks());
		attack = resolveNormalAttackValue();
		defenseRemainingMs = (long) defenseChargeTimeMs;
		defending = false;
		lastDefenseTickMs = -1;
	}

	public CharacterType getType() {
		return type;
	}

	public abstract double getGravity();

	public void normalAttack() {
		attack = resolveNormalAttackValue();
	}

	public void chargeAttack(long chargeMs) {
		attack = resolveChargeAttackValue(chargeMs);
	}

	public abstract void specialAttack();

	public void defend() {
		startDefend(System.currentTimeMillis());
	}

	public void startDefend(long nowMs) {
		if (defend <= 0.0 || defenseChargeTimeMs <= 0.0) return;
		if (defenseRemainingMs <= 0) return;
		if (!defending) {
			defending = true;
			lastDefenseTickMs = nowMs;
		}
	}

	public void stopDefend() {
		defending = false;
		lastDefenseTickMs = -1;
	}

	public void recoverDefense() {
		defenseRemainingMs = (long) defenseChargeTimeMs;
	}

	public void updateDefense(long nowMs) {
		if (!defending) return;
		if (lastDefenseTickMs < 0) {
			lastDefenseTickMs = nowMs;
			return;
		}
		long elapsed = Math.max(0L, nowMs - lastDefenseTickMs);
		if (elapsed <= 0) return;
		defenseRemainingMs = Math.max(0L, defenseRemainingMs - elapsed);
		lastDefenseTickMs = nowMs;
		if (defenseRemainingMs <= 0) defending = false;
	}

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
		return projectileSpeed;
	}

	/**
	 * null のときは近接キャラ(飛び道具なし)として扱う。
	 */
	public ProjectileType getProjectileType() {
		return null;
	}

	public double getProjectileRange() {
		return projectileRange;
	}

	public double getMeleeWidth() {
		return meleeWidth;
	}

	public double getMeleeHeight() {
		return meleeHeight;
	}

	public double getMeleeOffset() {
		return meleeOffset;
	}

	public int getMeleeLifetimeTicks() {
		return meleeLifetimeTicks;
	}

	public boolean isRanged() {
		return getProjectileType() != null;
	}

	public void setPosition(double x, double y) {
		position.setX(x);
		position.setY(y);
	}

	public boolean isGrounded() {
		return grounded;
	}

	public void setGrounded(boolean grounded) {
		this.grounded = grounded;
	}

	public void setVerticalVelocity(double vy) {
		velocity.setY(vy);
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

	public double getAttackMin() {
		return attackMin;
	}

	public double getAttackMax() {
		return attackMax;
	}

	public double getAttackPowerRatio() {
		double normal = resolveNormalAttackValue();
		if (normal <= 0.0) return 1.0;
		return attack / normal;
	}

	public int getHp() {
		return (int) Math.max(0, Math.round(hp));
	}

	public void setHp(int hp) {
		this.hp = Math.max(0, hp);
	}

	public int applyDamage(double damage) {
		if (defending && defenseRemainingMs > 0) return getHp();
		double mitigated = Math.max(0.0, damage - resolveDefense());
		hp = Math.max(0.0, hp - mitigated);
		return getHp();
	}

	public void movLeft() {
		position.moveX(-getMoveStepX());
	}

	public void movRight() {
		position.moveX(getMoveStepX());
	}

	public void movUp() {
		position.moveY(getMoveStepY());
	}

	public void movDown() {
		position.moveY(-getMoveStepY());
	}

	protected double resolveNormalAttackValue() {
		return (attackMin + attackMax) / 2.0;
	}

	protected double resolveChargeAttackValue(long chargeMs) {
		if (attackMax <= attackMin) return attackMin;
		if (attackChargeTimeMs <= 0.0) return attackMax;
		double clamped = Math.max(0.0, Math.min(chargeMs, attackChargeTimeMs));
		double ratio = clamped / attackChargeTimeMs;
		return attackMin + (attackMax - attackMin) * ratio;
	}

	private double resolveDefense() {
		if (defend <= 0.0) return 0.0;
		if (defenseChargeTimeMs <= 0.0) return 0.0;
		if (!defending) return 0.0;
		if (defenseRemainingMs <= 0) return 0.0;
		double ratio = Math.min(1.0, defenseRemainingMs / defenseChargeTimeMs);
		return defend * ratio;
	}
}
