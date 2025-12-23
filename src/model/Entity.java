package model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 物体の抽象クラスです。
 */
public abstract class Entity {
	private static final AtomicLong ID_GENERATOR = new AtomicLong(0);
	private final long id = ID_GENERATOR.getAndIncrement();
	/** 物体の位置を表すベクトル */
	protected Vector2D position;
	/** 物体の幅方向を表すベクトル（positionを始点とするベクトル） */
	protected Vector2D width;
	/** 物体の高さ方向を表すベクトル（positionを始点とするベクトル） */
	protected Vector2D height;

	public long getId() {
		return id;
	}

	public boolean collidesWith(Entity other) {
		// TODO: 衝突判定
		return false;
	}

}
