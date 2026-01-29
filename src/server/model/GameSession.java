package server.model;

import model.GameCharacter;
import model.PlayerInfo;
import model.ProjectileType;
import model.ResultData;
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
	private final Map<Integer, Integer> facingDirections = new HashMap<>();
	private final Map<Integer, Long> chargeStartTimes = new HashMap<>();

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
		facingDirections.clear();
		chargeStartTimes.clear();
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
			if (player == null) continue;
			int playerId = player.getId();
			playersById.put(playerId, player);
			aliveIds.add(playerId);
			facingDirections.put(playerId, 1);
			resultMap.put(playerId, new ResultData(playerId));
			GameCharacter character = player.getCharacter();
			if (character != null) {
				double slotCenter = (index + 0.5) / (double) maxPlayers;
				double x = fieldWidth * slotCenter;
				character.setPosition(x, groundY);
				character.setGrounded(true);
				character.setOwnerId(playerId);
				battleField.addEntity(character);
			}
			index++;
		}
	}

	public CommandType handleAction(CommandType actionType, PlayerInfo player) {
		if (actionType == null || !canAct(player)) return null;
		switch (actionType) {
			case MOVE_LEFT:
				setFacingDirection(player, -1);
				applyMove(player, -resolveMoveStepX(player), 0);
				return null;
			case MOVE_RIGHT:
				setFacingDirection(player, 1);
				applyMove(player, resolveMoveStepX(player), 0);
				return null;
			case MOVE_UP:
				return applyJump(player) ? CommandType.MOVE_UP : null;
			case MOVE_DOWN:
				applyMove(player, 0, -resolveMoveStepY(player));
				return null;
			case CHARGE_START:
				startCharge(player);
				return CommandType.CHARGE_START;
			case NORMAL_ATTACK:
				applyNormalAttack(player);
				return CommandType.NORMAL_ATTACK;
			case CHARGE_ATTACK:
				applyChargeAttack(player);
				return CommandType.CHARGE_ATTACK;
			case DEFEND:
				applyDefend(player);
				return CommandType.DEFEND;
			default:
				return null;
		}
	}

	public BattleField.UpdateResult update() {
		if (!started || battleField == null || gameOver) return null;
		BattleField.UpdateResult result = battleField.update();
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

	private boolean canAct(PlayerInfo player) {
		return player != null && started && !gameOver && aliveIds.contains(player.getId());
	}

	private void processDamage(List<BattleField.DamageEvent> events) {
		if (events == null || events.isEmpty() || gameOver) return;
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
		if (character == null || character.getPosition() == null) return;
		double fieldWidth = getFieldWidth();
		double fieldHeight = getFieldHeight();
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
		if (character == null) return;
		character.normalAttack();
		if (character.isRanged()) {
			spawnProjectile(player, character, 1.0);
		} else {
			spawnMeleeAttack(player, character, 1.0);
		}
	}

	private void applyChargeAttack(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		if (character == null) return;
		long chargeMs = stopCharge(player);
		character.chargeAttack(chargeMs);
		double power = character.getAttackPowerRatio();
		if (character.isRanged()) {
			spawnProjectile(player, character, power);
		} else {
			spawnMeleeAttack(player, character, power);
		}
	}

	private void applyDefend(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		if (character == null) return;
		character.defend();
	}

	private void spawnProjectile(PlayerInfo player, GameCharacter character, double power) {
		if (battleField == null || character.getPosition() == null) return;
		ProjectileType projectileType = character.getProjectileType();
		double maxDistance = character.getProjectileRange();
		if (projectileType == null || maxDistance <= 0) return;
		int direction = getFacingDirection(player);
		double speed = character.getProjectileSpeed() * Math.max(1.0, power);
		double damage = character.getAttack();
		double startX = character.getPosition().getX() + (direction * 16);
		double startY = character.getPosition().getY() + 35;
		Projectile projectile = new Projectile(
				projectileType,
				player.getId(),
				startX,
				startY,
				direction * speed,
				0,
				power,
				damage,
				maxDistance
		);
		battleField.addEntity(projectile);
	}

	private void spawnMeleeAttack(PlayerInfo player, GameCharacter character, double power) {
		if (battleField == null || character.getPosition() == null) return;
		double width = character.getMeleeWidth() * Math.min(1.4, power);
		double height = character.getMeleeHeight() * Math.min(1.3, power);
		double offset = character.getMeleeOffset();
		int lifetime = character.getMeleeLifetimeTicks();
		double damage = character.getAttack();
		double baseX = character.getPosition().getX();
		double baseY = character.getPosition().getY();
		AttackHitbox front = new AttackHitbox(
				player.getId(),
				damage,
				baseX + offset,
				baseY,
				width,
				height,
				0,
				0,
				0,
				lifetime
		);
		AttackHitbox back = new AttackHitbox(
				player.getId(),
				damage,
				baseX - offset,
				baseY,
				width,
				height,
				0,
				0,
				0,
				lifetime
		);
		battleField.addEntity(front);
		battleField.addEntity(back);
	}

	private boolean applyJump(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		if (character == null || !character.canJump()) return false;
		character.setVerticalVelocity(character.getJumpVelocity());
		character.registerJump();
		return true;
	}

	private void setFacingDirection(PlayerInfo player, int direction) {
		if (direction == 0 || player == null) return;
		facingDirections.put(player.getId(), direction > 0 ? 1 : -1);
	}

	private int getFacingDirection(PlayerInfo player) {
		if (player == null) return 1;
		return facingDirections.getOrDefault(player.getId(), 1);
	}

	private void startCharge(PlayerInfo player) {
		if (player == null) return;
		chargeStartTimes.put(player.getId(), System.currentTimeMillis());
	}

	private long stopCharge(PlayerInfo player) {
		if (player == null) return 0;
		Long start = chargeStartTimes.remove(player.getId());
		if (start == null) return 0;
		return Math.max(0, System.currentTimeMillis() - start);
	}

	private double resolveMoveStepX(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		return character != null ? character.getMoveStepX() : 0;
	}

	private double resolveMoveStepY(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
		return character != null ? character.getMoveStepY() : 0;
	}

	private int getFieldWidth() {
		return battleField != null ? battleField.getWidth() : BattleField.DEFAULT_WIDTH;
	}

	private int getFieldHeight() {
		return battleField != null ? battleField.getHeight() : BattleField.DEFAULT_HEIGHT;
	}
}
