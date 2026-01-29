package network;

import java.util.EnumSet;

import static java.util.Arrays.fill;
import static java.util.Arrays.stream;

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
	PROJECTILE(13),
	PROJECTILE_REMOVE(14),

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
	CREATE_ROOM(60),
	READY_SUCCESS(61),
	UNREADY_SUCCESS(62),
	NORMAL_ATTACK(64),
	CHARGE_ATTACK(65),
	DEFEND(66),
	CHARGE_START(67),

	// -------------------- その他 --------------------
	ERROR(254),
	UNKNOWN(255);

	public static final EnumSet<CommandType> GAME_INPUT_ACTIONS = EnumSet.of(
			MOVE_LEFT,
			MOVE_UP,
			MOVE_RIGHT,
			MOVE_DOWN,
			NORMAL_ATTACK,
			CHARGE_START,
			CHARGE_ATTACK,
			DEFEND
	);

	public static final EnumSet<CommandType> BROADCAST_ACTIONS = EnumSet.of(
			MOVE_UP,
			NORMAL_ATTACK,
			CHARGE_START,
			CHARGE_ATTACK,
			DEFEND
	);

	private final int id;

	private static final CommandType[] CACHE = new CommandType[256];

	static {
		fill(CACHE, UNKNOWN);
		stream(values()).forEach(type -> CACHE[type.id] = type);
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
