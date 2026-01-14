package model;

import java.util.ArrayList;
import java.util.List;

public final class BattleField {
	private final List<Entity> entities = new ArrayList<>();
	public int width, height;

	public void addEntity(Entity entity) {
		entities.add(entity);
	}

	public void update() {
		for (Entity entity : entities) {
			 entity.update();
			 entity.position.addLocal(entity.velocity);
		}
		updateCollision();
	}

	private void updateCollision() {
		for (Entity entity : entities) {
			for (Entity other : entities) {
				if (entity == other || !entity.collidesWith(other)) continue;
				// TODO: 衝突処理
			}
		}
	}
}
