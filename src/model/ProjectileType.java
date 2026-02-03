package model;

public enum ProjectileType {
	ARROW(1),
	MAGIC(2);

	private final int id;

	ProjectileType(int id) {
		this.id = id;
	}

	public static ProjectileType fromId(int id) {
		for (ProjectileType type : values()) {
			if (type.id == id) return type;
		}
		return ARROW;
	}

	public int getId() {
		return id;
	}
}
