package model;

import java.util.ArrayList;
import java.util.List;

public final class BattleField {
	private final List<Entity> entities = new ArrayList<>();
	public int width, height;

	private void updateCollision() {
		for (Entity entity : entities) {
			for (Entity other : entities) {
				if (entity == other || !entity.collidesWith(other)) continue;
				// TODO: 衝突処理
			}
		}
	}
}
