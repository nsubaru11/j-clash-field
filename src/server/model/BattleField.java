package server.model;

import model.Entity;
import model.GameCharacter;
import model.Vector2D;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class BattleField {
public static final int DEFAULT_WIDTH = 1280;
public static final int DEFAULT_HEIGHT = 720;
public static final double DEFAULT_GROUND_Y = DEFAULT_HEIGHT * 0.255;
	private final List<Entity> entities = new ArrayList<>();
	private final int width;
	private final int height;
	private final double groundY;

	public BattleField() {
		this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_GROUND_Y);
	}

	public BattleField(int width, int height, double groundY) {
		this.width = width;
		this.height = height;
		this.groundY = groundY;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public double getGroundY() {
		return groundY;
	}

	public void addEntity(Entity entity) {
		entities.add(entity);
	}

	public UpdateResult update() {
		List<Projectile> removedProjectiles = new ArrayList<>();
		List<DamageEvent> damageEvents = new ArrayList<>();
		for (Entity entity : entities) {
			if (entity instanceof GameCharacter) {
				GameCharacter character = (GameCharacter) entity;
				applyGravity(character);
				character.update();
				clampToGround(character);
			} else {
				entity.update();
			}
		}
		Iterator<Entity> iterator = entities.iterator();
		while (iterator.hasNext()) {
			Entity entity = iterator.next();
			if (entity instanceof Projectile) {
				Projectile projectile = (Projectile) entity;
				if (projectile.isOutOfBounds(width, height) || projectile.isExpired()) {
					removedProjectiles.add(projectile);
					iterator.remove();
				}
			}
		}
		updateCollision(removedProjectiles, damageEvents);
		removeExpiredHitboxes();
		return new UpdateResult(removedProjectiles, damageEvents);
	}

	private void updateCollision(List<Projectile> removedProjectiles, List<DamageEvent> damageEvents) {
		List<Entity> toRemove = new ArrayList<>();
		for (Entity entity : entities) {
			if (entity instanceof Projectile) {
				Projectile projectile = (Projectile) entity;
				for (Entity other : entities) {
					if (entity == other) continue;
					if (other instanceof GameCharacter) {
						GameCharacter character = (GameCharacter) other;
						if (character.getOwnerId() == projectile.getOwnerId()) continue;
					}
					if (!projectile.collidesWith(other)) continue;
					if (other instanceof Projectile) {
						toRemove.add(projectile);
						toRemove.add(other);
						if (!removedProjectiles.contains(projectile)) removedProjectiles.add(projectile);
						if (!removedProjectiles.contains(other)) removedProjectiles.add((Projectile) other);
						break;
					} else {
						if (other instanceof GameCharacter) {
							GameCharacter character = (GameCharacter) other;
						int oldHp = character.getHp();
						int newHp = character.applyDamage(projectile.getDamage());
							double dealt = Math.max(0, oldHp - newHp);
							damageEvents.add(new DamageEvent(character.getOwnerId(), newHp, projectile.getOwnerId(), dealt));
						}
						toRemove.add(projectile);
						if (!removedProjectiles.contains(projectile)) removedProjectiles.add(projectile);
						break;
					}
				}
			} else if (entity instanceof AttackHitbox) {
				AttackHitbox hitbox = (AttackHitbox) entity;
				boolean hitAny = false;
				for (Entity other : entities) {
					if (entity == other || !(other instanceof GameCharacter)) continue;
					GameCharacter character = (GameCharacter) other;
					if (character.getOwnerId() == hitbox.getOwnerId()) continue;
					if (!hitbox.collidesWith(character)) continue;
					int oldHp = character.getHp();
					int newHp = character.applyDamage(hitbox.getDamage());
					double dealt = Math.max(0, oldHp - newHp);
					damageEvents.add(new DamageEvent(character.getOwnerId(), newHp, hitbox.getOwnerId(), dealt));
					hitAny = true;
				}
				if (hitAny) toRemove.add(hitbox);
			}
		}
		if (!toRemove.isEmpty()) entities.removeAll(toRemove);
	}

	public List<Projectile> getProjectiles() {
		List<Projectile> list = new ArrayList<>();
		for (Entity entity : entities) {
			if (entity instanceof Projectile) {
				list.add((Projectile) entity);
			}
		}
		return list;
	}

	private void applyGravity(GameCharacter character) {
		Vector2D velocity = character.getVelocity();
		velocity.setY(velocity.getY() + character.getGravity());
	}

	private void clampToGround(GameCharacter character) {
		Vector2D velocity = character.getVelocity();
		if (character.getPosition().getY() <= groundY) {
			character.getPosition().setY(groundY);
			if (velocity.getY() < 0) {
				character.setVerticalVelocity(0);
			}
			character.setGrounded(true);
			character.resetJumpCount();
		} else {
			character.setGrounded(false);
		}
	}

	private void removeExpiredHitboxes() {
		Iterator<Entity> iterator = entities.iterator();
		while (iterator.hasNext()) {
			Entity entity = iterator.next();
			if (entity instanceof AttackHitbox) {
				AttackHitbox hitbox = (AttackHitbox) entity;
				if (hitbox.isExpired()) {
					iterator.remove();
				}
			}
		}
	}

	public static final class UpdateResult {
		private final List<Projectile> removedProjectiles;
		private final List<DamageEvent> damageEvents;

		public UpdateResult(List<Projectile> removedProjectiles, List<DamageEvent> damageEvents) {
			this.removedProjectiles = removedProjectiles;
			this.damageEvents = damageEvents;
		}

		public List<Projectile> getRemovedProjectiles() {
			return removedProjectiles;
		}

		public List<DamageEvent> getDamageEvents() {
			return damageEvents;
		}
	}

	public static final class DamageEvent {
		private final int targetId;
		private final int hp;
		private final int sourceId;
		private final double damage;

		public DamageEvent(int targetId, int hp, int sourceId, double damage) {
			this.targetId = targetId;
			this.hp = hp;
			this.sourceId = sourceId;
			this.damage = damage;
		}

		public int getTargetId() {
			return targetId;
		}

		public int getHp() {
			return hp;
		}

		public int getSourceId() {
			return sourceId;
		}

		public double getDamage() {
			return damage;
		}
	}
}
