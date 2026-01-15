package model;

public final class Protocol {
	private Protocol() {
	}

	// -------------------- サーバー -> クライアント --------------------
	public static String gameStart() {
		return CommandType.GAME_START.getId() + "";
	}

	public static String gameOver() {
		return CommandType.GAME_OVER.getId() + "";
	}

	public static String move(long id, double i, double j) {
		return CommandType.MOVE.getId() + ":" + id + ":" + i + " " + j;
	}

	public static String damage(int hp) {
		return CommandType.DAMAGE.getId() + ":" + hp;
	}

	public static String dead() {
		return CommandType.DEAD.getId() + "";
	}

	public static String opponentResigned() {
		return CommandType.OPPONENT_RESIGNED.getId() + "";
	}

	public static String opponentDisconnected() {
		return CommandType.OPPONENT_DISCONNECTED.getId() + "";
	}

	public static String joinSuccess(int roomId) {
		return CommandType.JOIN_SUCCESS.getId() + ":" + roomId;
	}

	public static String joinFailed() {
		return CommandType.JOIN_FAILED.getId() + "";
	}

	public static String joinOpponent(String opponentName) {
		return CommandType.JOIN_OPPONENT.getId() + ":" + opponentName;
	}

	public static String result(String result) {
		return CommandType.RESULT.getId() + ":" + result;
	}

	public static String gameRoomClosed() {
		return CommandType.GAME_ROOM_CLOSED.getId() + "";
	}

	public static String serverClosed() {
		return CommandType.SERVER_CLOSED.getId() + "";
	}

	// -------------------- クライアント -> サーバー --------------------

	public static String connect() {
		return CommandType.CONNECT.getId() + "";
	}

	public static String join(String userName, int roomId) {
		return CommandType.JOIN.getId() + ":" + userName + ":" + roomId;
	}

	public static String moveLeft() {
		return CommandType.MOVE_LEFT.getId() + "";
	}

	public static String moveRight() {
		return CommandType.MOVE_RIGHT.getId() + "";
	}

	public static String moveUp() {
		return CommandType.MOVE_UP.getId() + "";
	}

	public static String moveDown() {
		return CommandType.MOVE_DOWN.getId() + "";
	}

	public static String ready(GameCharacter character) {
		return CommandType.READY.getId() + ":" + character.getClass().getName();
	}

	public static String unready() {
		return CommandType.UNREADY.getId() + "";
	}

	public static String resign() {
		return CommandType.RESIGN.getId() + "";
	}

	public static String disconnect() {
		return CommandType.DISCONNECT.getId() + "";
	}
}
