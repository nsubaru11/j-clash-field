package server.model;

import model.ProjectileType;

public interface RangedAttacker {
	ProjectileType getProjectileType();

	double getProjectileRange();

	double getProjectileSpeed();

	void shoot(BattleField field); // 攻撃処理
}
