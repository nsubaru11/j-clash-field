package server.model;

public interface MeleeAttacker {
	double getMeleeWidth();

	double getMeleeHeight();

	void strike(BattleField field); // 攻撃処理
}
