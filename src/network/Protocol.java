package network;

import model.CharacterType;
import model.ProjectileType;

/**
 * 通信用のクラスです。
 * 区切り文字をレベル別に[`:` > `,` > ` `]と定義するため、これらの文字を送信データ内に含んではいけません。
 */
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

	public static String move(int playerId, double i, double j) {
		return CommandType.MOVE.getId() + ":" + playerId + ":" + i + "," + j;
	}

	public static String moveUp(int playerId) {
		return CommandType.MOVE_UP.getId() + ":" + playerId;
	}

	public static String damage(int playerId, int hp) {
		return CommandType.DAMAGE.getId() + ":" + playerId + "," + hp;
	}

	public static String dead() {
		return CommandType.DEAD.getId() + "";
	}

	public static String opponentResigned() {
		return CommandType.OPPONENT_RESIGNED.getId() + "";
	}

	public static String opponentDisconnected(int playerId) {
		return CommandType.OPPONENT_DISCONNECTED.getId() + ":" + playerId;
	}

	public static String joinSuccess(int playerId, String roomState) {
		return CommandType.JOIN_SUCCESS.getId() + ":" + playerId + ":" + roomState;
	}

	public static String joinFailed() {
		return CommandType.JOIN_FAILED.getId() + "";
	}

	public static String joinOpponent(int opponentId, String opponentName) {
		return CommandType.JOIN_OPPONENT.getId() + ":" + opponentId + "," + opponentName;
	}

	public static String readySuccess(int playerId, int characterId) {
		return CommandType.READY_SUCCESS.getId() + ":" + playerId + "," + characterId;
	}

	public static String unreadySuccess(int playerId) {
		return CommandType.UNREADY_SUCCESS.getId() + ":" + playerId;
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

	public static String projectile(long projectileId, ProjectileType type, double x, double y, double power) {
		return CommandType.PROJECTILE.getId() + ":" + projectileId + "," + type.getId() + "," + x + "," + y + "," + power;
	}

	public static String projectileRemove(long projectileId) {
		return CommandType.PROJECTILE_REMOVE.getId() + ":" + projectileId;
	}

	// -------------------- クライアント -> サーバー --------------------

	public static String connect() {
		return CommandType.CONNECT.getId() + "";
	}

	public static String createRoom(String userName) {
		return CommandType.CREATE_ROOM.getId() + ":" + userName;
	}

	public static String join(String userName, int roomId) {
		return CommandType.JOIN.getId() + ":" + userName + ":" + roomId;
	}

	public static String ready(CharacterType characterType) {
		return CommandType.READY.getId() + ":" + characterType.getId();
	}

	public static String unready() {
		return CommandType.UNREADY.getId() + "";
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

	public static String normalAttack() {
		return CommandType.NORMAL_ATTACK.getId() + "";
	}

	public static String normalAttack(int playerId) {
		return CommandType.NORMAL_ATTACK.getId() + ":" + playerId;
	}

	public static String chargeAttack() {
		return CommandType.CHARGE_ATTACK.getId() + "";
	}

	public static String chargeStart() {
		return CommandType.CHARGE_START.getId() + "";
	}

	public static String chargeAttack(int playerId) {
		return CommandType.CHARGE_ATTACK.getId() + ":" + playerId;
	}

	public static String defend() {
		return CommandType.DEFEND.getId() + "";
	}

	public static String chargeStart(int playerId) {
		return CommandType.CHARGE_START.getId() + ":" + playerId;
	}

	public static String defend(int playerId) {
		return CommandType.DEFEND.getId() + ":" + playerId;
	}

	public static String resign() {
		return CommandType.RESIGN.getId() + "";
	}

	public static String disconnect() {
		return CommandType.DISCONNECT.getId() + "";
	}
}
