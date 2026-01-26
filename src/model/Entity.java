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
	/** 物体の速度を表すベクトル */
	protected Vector2D velocity = new Vector2D(0, 0);

	public long getId() {
		return id;
	}

	public Vector2D getPosition() {
		return position;
	}

	public Vector2D getVelocity() {
		return velocity;
	}

	public void update() {
		if (velocity != null && position != null) {
			position.addLocal(velocity);
		}
	}

	public void setVelocity(double x, double y) {
		this.velocity = new Vector2D(x, y);
	}

	public boolean collidesWith(Entity other) {
		if (other == null || position == null || other.position == null) return false;
		double widthValue = getWidthValue();
		double heightValue = getHeightValue();
		double otherWidth = other.getWidthValue();
		double otherHeight = other.getHeightValue();
		if (widthValue <= 0 || heightValue <= 0 || otherWidth <= 0 || otherHeight <= 0) return false;

		double thisMinX = position.getX() - widthValue / 2.0;
		double thisMaxX = position.getX() + widthValue / 2.0;
		double thisMinY = position.getY() - heightValue;
		double thisMaxY = position.getY();

		double otherMinX = other.position.getX() - otherWidth / 2.0;
		double otherMaxX = other.position.getX() + otherWidth / 2.0;
		double otherMinY = other.position.getY() - otherHeight;
		double otherMaxY = other.position.getY();

		return thisMinX < otherMaxX && thisMaxX > otherMinX
				&& thisMinY < otherMaxY && thisMaxY > otherMinY;
	}

	public double getWidthValue() {
		if (width == null) return 0;
		return Math.abs(width.getX());
	}

	public double getHeightValue() {
		if (height == null) return 0;
		return Math.abs(height.getY());
	}

}
