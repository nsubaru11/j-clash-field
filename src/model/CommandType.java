package model;

import java.util.Arrays;

public enum CommandType {
	// -------------------- サーバー -> クライアント --------------------
	GAME_START(0),
	GAME_OVER(1),
	MOVE(2),
	DAMAGE(3),
	DEAD(4),
	OPPONENT_RESIGNED(5),
	OPPONENT_DISCONNECTED(6),
	JOIN_SUCCESS(7),
	JOIN_FAILED(8),
	JOIN_OPPONENT(9),
	RESULT(10),
	GAME_ROOM_CLOSED(11),
	SERVER_CLOSED(12),

	// -------------------- クライアント -> サーバー --------------------
	CONNECT(50),
	JOIN(51),
	READY(52),
	UNREADY(53),
	MOVE_LEFT(54),
	MOVE_UP(55),
	MOVE_RIGHT(56),
	MOVE_DOWN(57),
	RESIGN(58),
	DISCONNECT(59),

	// -------------------- その他 --------------------
	ERROR(254),
	UNKNOWN(255);

	private final int id;

	private static final CommandType[] CACHE = new CommandType[256];

	static {
		Arrays.fill(CACHE, UNKNOWN);
		for (CommandType type : values()) {
			if (type.id >= 0 && type.id < CACHE.length) {
				CACHE[type.id] = type;
			}
		}
	}

	CommandType(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static CommandType fromId(int id) {
		if (id < 0 || id >= CACHE.length) return UNKNOWN;
		return CACHE[id];
	}
}
