package model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class BattleField {
	private final List<Entity> entities = new ArrayList<>();
	public int width, height;
	private final double groundY;
	private final double gravity;

	public BattleField(int width, int height, double groundY, double gravity) {
		this.width = width;
		this.height = height;
		this.groundY = groundY;
		this.gravity = gravity;
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
				if (projectile.isOutOfBounds(width, height)) {
					removedProjectiles.add(projectile);
					iterator.remove();
				}
			}
		}
		updateCollision(removedProjectiles, damageEvents);
		return new UpdateResult(removedProjectiles, damageEvents);
	}

	private void updateCollision(List<Projectile> removedProjectiles, List<DamageEvent> damageEvents) {
		List<Entity> toRemove = new ArrayList<>();
		for (Entity entity : entities) {
			if (!(entity instanceof Projectile)) continue;
			Projectile projectile = (Projectile) entity;
			for (Entity other : entities) {
				if (entity == other || !(other instanceof GameCharacter)) continue;
				GameCharacter character = (GameCharacter) other;
				if (character.getOwnerId() == projectile.getOwnerId()) continue;
				if (!projectile.collidesWith(character)) continue;
				int newHp = character.applyDamage(projectile.getDamage() * projectile.getPower());
				damageEvents.add(new DamageEvent(character.getOwnerId(), newHp));
				toRemove.add(projectile);
				removedProjectiles.add(projectile);
				break;
			}
		}
		if (!toRemove.isEmpty()) {
			entities.removeAll(toRemove);
		}
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
		if (character.velocity == null) return;
		character.velocity.setY(character.velocity.getY() + gravity);
	}

	private void clampToGround(GameCharacter character) {
		if (character.getPosition() == null || character.velocity == null) return;
		if (character.getPosition().getY() <= groundY) {
			character.getPosition().setY(groundY);
			if (character.velocity.getY() < 0) {
				character.velocity.setY(0);
			}
			character.setGrounded(true);
			character.resetJumpCount();
		} else {
			character.setGrounded(false);
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

		public DamageEvent(int targetId, int hp) {
			this.targetId = targetId;
			this.hp = hp;
		}

		public int getTargetId() {
			return targetId;
		}

		public int getHp() {
			return hp;
		}
	}
}
