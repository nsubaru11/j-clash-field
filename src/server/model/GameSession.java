package server.model;

import model.GameCharacter;
import model.PlayerInfo;
import model.ResultData;
import model.Vector2D;
import network.CommandType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GameSession {
	private final int maxPlayers;
	private final Map<Integer, PlayerInfo> playersById = new HashMap<>();
	private final Map<Integer, ResultData> resultMap = new HashMap<>();
	private final Set<Integer> aliveIds = new HashSet<>();
	private final Map<Integer, Long> chargeStartTimes = new HashMap<>();
	private static final long DEFEND_HOLD_TIMEOUT_MS = 250L;
	private final Map<Integer, Long> defendInputTimes = new HashMap<>();

	private BattleField battleField;
	private boolean started;
	private boolean gameOver;
	private boolean resultReady;
	private List<ResultData> finalResults = new ArrayList<>();

	public GameSession(int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}

	public boolean isStarted() {
		return started;
	}

	public boolean isGameOver() {
		return gameOver;
	}

	public BattleField getBattleField() {
		return battleField;
	}

	public void start(Collection<PlayerInfo> players) {
		playersById.clear();
		resultMap.clear();
		aliveIds.clear();
		chargeStartTimes.clear();
		defendInputTimes.clear();
		finalResults = new ArrayList<>();
		resultReady = false;
		gameOver = false;
		started = true;

		battleField = new BattleField();
		double fieldWidth = battleField.getWidth();
		double groundY = battleField.getGroundY();
		List<PlayerInfo> playersList = new ArrayList<>(players);
		int index = 0;
		for (PlayerInfo player : playersList) {
			int playerId = player.getId();
			playersById.put(playerId, player);
			aliveIds.add(playerId);
			resultMap.put(playerId, new ResultData(playerId));
			GameCharacter character = player.getCharacter();
			double slotCenter = (index + 0.5) / (double) maxPlayers;
			double x = fieldWidth * slotCenter;
			character.setPosition(x, groundY);
			character.setGrounded(true);
			character.stopDefend();
			character.recoverDefense();
			character.setOwnerId(playerId);
			battleField.addEntity(character);
			index++;
		}
	}

	public CommandType handleAction(CommandType actionType, PlayerInfo player) {
		if (!canAct(player)) return null;
		long now = System.currentTimeMillis();
		GameCharacter character = player.getCharacter();
		switch (actionType) {
			case MOVE_LEFT:
				character.stopDefend();
				character.recoverDefense();
				defendInputTimes.remove(player.getId());
				setFacingDirection(player, -1, 0);
				applyMove(player, -resolveMoveStepX(player), 0);
				return null;
			case MOVE_RIGHT:
				character.stopDefend();
				character.recoverDefense();
				defendInputTimes.remove(player.getId());
				setFacingDirection(player, 1, 0);
				applyMove(player, resolveMoveStepX(player), 0);
				return null;
			case MOVE_UP:
				character.stopDefend();
				character.recoverDefense();
				defendInputTimes.remove(player.getId());
				setFacingDirection(player, 0, 1);
				return applyJump(player) ? CommandType.MOVE_UP : null;
			case MOVE_DOWN:
				character.stopDefend();
				character.recoverDefense();
				defendInputTimes.remove(player.getId());
				setFacingDirection(player, 0, -1);
				applyMove(player, 0, -resolveMoveStepY(player));
				return null;
			case CHARGE_START:
				character.stopDefend();
				character.recoverDefense();
				defendInputTimes.remove(player.getId());
				startCharge(player);
				return CommandType.CHARGE_START;
			case NORMAL_ATTACK:
				character.stopDefend();
				character.recoverDefense();
				defendInputTimes.remove(player.getId());
				applyNormalAttack(player);
				return CommandType.NORMAL_ATTACK;
			case CHARGE_ATTACK:
				character.stopDefend();
				character.recoverDefense();
				defendInputTimes.remove(player.getId());
				applyChargeAttack(player);
				return CommandType.CHARGE_ATTACK;
			case DEFEND:
				defendInputTimes.put(player.getId(), now);
				if (character.startDefend(now)) {
					return CommandType.DEFEND;
				}
				return null;
			default:
				return null;
		}
	}

	public BattleField.UpdateResult update() {
		if (!started || gameOver) return null;
		BattleField.UpdateResult result = battleField.update();
		updateDefenseStates();
		processDamage(result.getDamageEvents());
		return result;
	}

	public boolean eliminatePlayer(int playerId, boolean countDeath) {
		if (!aliveIds.remove(playerId)) return false;
		ResultData data = resultMap.get(playerId);
		if (countDeath && data != null) data.incrementDeaths();
		if (aliveIds.size() <= 1) {
			finalizeResultsNonCombat();
		}
		return true;
	}

	public List<ResultData> consumeResults() {
		if (!resultReady) return null;
		resultReady = false;
		return new ArrayList<>(finalResults);
	}

	public void clearGameOver() {
		gameOver = false;
		started = false;
		battleField = null;
		resultReady = false;
		finalResults = new ArrayList<>();
	}

	private boolean canAct(PlayerInfo player) {
		return started && !gameOver && aliveIds.contains(player.getId());
	}

	private void processDamage(List<BattleField.DamageEvent> events) {
		if (events.isEmpty() || gameOver) return;
		Set<Integer> deathsThisFrame = new HashSet<>();
		Set<Integer> aliveBefore = new HashSet<>(aliveIds);
		for (BattleField.DamageEvent damage : events) {
			int targetId = damage.getTargetId();
			int sourceId = damage.getSourceId();
			double dealt = damage.getDamage();
			ResultData targetData = resultMap.get(targetId);
			if (targetData != null) targetData.addDamageTaken(dealt);
			ResultData sourceData = resultMap.get(sourceId);
			if (sourceData != null) sourceData.addDamageGiven(dealt);

			if (damage.getHp() <= 0 && aliveIds.contains(targetId) && deathsThisFrame.add(targetId)) {
				if (targetData != null) targetData.incrementDeaths();
				if (sourceId != targetId && sourceData != null) {
					sourceData.incrementKills();
				}
			}
		}
		if (!deathsThisFrame.isEmpty()) {
			aliveIds.removeAll(deathsThisFrame);
			if (aliveIds.size() <= 1) {
				finalizeResults(aliveBefore, deathsThisFrame);
			}
		}
	}

	private void finalizeResults(Set<Integer> aliveBefore, Set<Integer> deathsThisFrame) {
		if (gameOver) return;
		gameOver = true;
		started = false;
		Set<Integer> winners = new HashSet<>();
		boolean drawAll = false;

		if (aliveIds.size() == 1) {
			winners.addAll(aliveIds);
		} else if (aliveIds.isEmpty()) {
			Set<Integer> allPlayers = playersById.keySet();
			if (!allPlayers.isEmpty() && deathsThisFrame.containsAll(allPlayers)) {
				drawAll = true;
			} else if (!aliveBefore.isEmpty() && deathsThisFrame.containsAll(aliveBefore)) {
				winners.addAll(aliveBefore);
			} else {
				drawAll = true;
			}
		}

		buildFinalResults(winners, drawAll);
	}

	private void finalizeResultsNonCombat() {
		if (gameOver) return;
		gameOver = true;
		started = false;
		Set<Integer> winners = new HashSet<>(aliveIds);
		boolean drawAll = winners.isEmpty();
		buildFinalResults(winners, drawAll);
	}

	private void buildFinalResults(Set<Integer> winners, boolean drawAll) {
		List<ResultData> list = new ArrayList<>(resultMap.values());
		for (ResultData data : list) {
			if (drawAll) {
				data.setResult(ResultData.ResultType.DRAW);
			} else if (winners.contains(data.getId())) {
				data.setResult(ResultData.ResultType.WIN);
			} else {
				data.setResult(ResultData.ResultType.LOSE);
			}
		}
		finalResults = list;
		resultReady = true;
	}

	private void applyMove(PlayerInfo player, double dx, double dy) {
		GameCharacter character = player.getCharacter();
		double fieldWidth = battleField.getWidth();
		double fieldHeight = battleField.getHeight();
		double nextX = character.getPosition().getX() + dx;
		double nextY = character.getPosition().getY() + dy;
		if (nextX < 0) nextX = 0;
		if (nextX > fieldWidth) nextX = fieldWidth;
		if (nextY < 0) nextY = 0;
		if (nextY > fieldHeight) nextY = fieldHeight;
		character.setPosition(nextX, nextY);
	}

	private void applyNormalAttack(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		character.normalAttack();
		if (character instanceof RangedAttacker) {
			((RangedAttacker) character).shoot(battleField);
		} else if (character instanceof MeleeAttacker) {
			((MeleeAttacker) character).strike(battleField);
		}
	}

	private void applyChargeAttack(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		long chargeMs = stopCharge(player);
		character.chargeAttack(chargeMs);
		if (character instanceof RangedAttacker) {
			((RangedAttacker) character).shoot(battleField);
		} else if (character instanceof MeleeAttacker) {
			((MeleeAttacker) character).strike(battleField);
		}
	}

	private boolean applyJump(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		if (!character.canJump()) return false;
		character.setVerticalVelocity(character.getJumpVelocity());
		character.registerJump();
		return true;
	}

	private void setFacingDirection(PlayerInfo player, double x, double y) {
		GameCharacter character = player.getCharacter();
		character.setFacingDirection(x, y);
	}

	private void updateDefenseStates() {
		long now = System.currentTimeMillis();
		for (PlayerInfo player : playersById.values()) {
			GameCharacter character = player.getCharacter();
			character.updateDefense(now);
			Long lastInput = defendInputTimes.get(player.getId());
			if (lastInput != null && now - lastInput > DEFEND_HOLD_TIMEOUT_MS) {
				character.stopDefend();
				defendInputTimes.remove(player.getId());
			}
		}
	}

	private Vector2D getFacingDirection(GameCharacter character) {
		return character.getFacingDirection();
	}


	private void startCharge(PlayerInfo player) {
		chargeStartTimes.put(player.getId(), System.currentTimeMillis());
	}

	private long stopCharge(PlayerInfo player) {
		Long start = chargeStartTimes.remove(player.getId());
		return Math.max(0, System.currentTimeMillis() - start);
	}

	private double resolveMoveStepX(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		return character.getMoveStepX();
	}

	private double resolveMoveStepY(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		return character.getMoveStepY();
	}
}
