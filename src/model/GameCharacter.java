package model;

public  abstract class GameCharacter extends Entity {
	protected int speedX, speedY;

	public abstract void normalAttack();

	public abstract void chargeAttack();

	public abstract void specialAttack();

	public abstract void defend();

	public void movLeft() {
		position.moveX(-speedX);
	}

	public void movRight() {
		position.moveX(speedX);
	}

	public void movUp() {
		position.moveY(speedY);
	}

	public void movDown() {
		position.moveY(-speedY);
	}
}
