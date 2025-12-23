package model;

import java.util.Objects;

@SuppressWarnings("unused")
public final class Vector2D implements Cloneable {
	private double x, y;

	public Vector2D(final double x, final double y) {
		this.x = x;
		this.y = y;
	}

	public Vector2D clone() {
		return new Vector2D(x, y);
	}

	public boolean equals(final Object obj) {
		if (!(obj instanceof Vector2D)) return false;
		Vector2D v = (Vector2D) obj;
		return x == v.x && y == v.y;
	}

	public int hashCode() {
		return Objects.hash(x, y);
	}

	public String toString() {
		return String.format("(%.2f, %.2f)", x, y);
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public void moveX(double dx) {
		x += dx;
	}

	public void moveY(double dy) {
		y += dy;
	}

	public double length() {
		return Math.sqrt(x * x + y * y);
	}

	public Vector2D add(final Vector2D v) {
		return new Vector2D(x + v.x, y + v.y);
	}

	public void addLocal(final Vector2D v) {
		x += v.x;
		y += v.y;
	}

	public Vector2D sub(final Vector2D v) {
		return new Vector2D(x - v.x, y - v.y);
	}

	public void subLocal(final Vector2D v) {
		x -= v.x;
		y -= v.y;
	}

	public Vector2D mul(final double s) {
		return new Vector2D(x * s, y * s);
	}

	public void mulLocal(final double s) {
		x *= s;
		y *= s;
	}

	public Vector2D div(final double s) {
		return new Vector2D(x / s, y / s);
	}

	public void divLocal(final double s) {
		x /= s;
		y /= s;
	}

	public Vector2D negate() {
		return new Vector2D(-x, -y);
	}

	public void negateLocal() {
		x = -x;
		y = -y;
	}

	public Vector2D normalize() {
		double len = length();
		return new Vector2D(x / len, y / len);
	}

	public void normalizeLocal() {
		double len = length();
		x /= len;
		y /= len;
	}

	public double dot(final Vector2D v) {
		return x * v.x + y * v.y;
	}

	public double crossZ(final Vector2D v) {
		return x * v.y - y * v.x;
	}
}
