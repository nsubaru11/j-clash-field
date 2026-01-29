package server.model;

import model.GameCharacter;
import model.PlayerInfo;
import model.ProjectileType;
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
			character.setOwnerId(playerId);
			battleField.addEntity(character);
			index++;
		}
	}

	public CommandType handleAction(CommandType actionType, PlayerInfo player) {
		if (!canAct(player)) return null;
		switch (actionType) {
			case MOVE_LEFT:
				setFacingDirection(player, -1, 0);
				applyMove(player, -resolveMoveStepX(player), 0);
				return null;
			case MOVE_RIGHT:
				setFacingDirection(player, 1, 0);
				applyMove(player, resolveMoveStepX(player), 0);
				return null;
			case MOVE_UP:
				setFacingDirection(player, 0, 1);
				return applyJump(player) ? CommandType.MOVE_UP : null;
			case MOVE_DOWN:
				setFacingDirection(player, 0, -1);
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
		if (!started || gameOver) return null;
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
		if (character.isRanged()) {
			spawnProjectile(player, character, 1.0);
		} else {
			spawnMeleeAttack(player, character, 1.0);
		}
	}

	private void applyChargeAttack(PlayerInfo player) {
		GameCharacter character = player.getCharacter();
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
		character.defend();
	}

	private void spawnProjectile(PlayerInfo player, GameCharacter character, double power) {
		ProjectileType projectileType = character.getProjectileType();
		double maxDistance = character.getProjectileRange();
		if (maxDistance <= 0) return;
		Vector2D facing = getFacingDirection(character);
		double speed = character.getProjectileSpeed() * Math.max(1.0, power);
		double damage = character.getAttack();
		double startX = character.getPosition().getX() + (facing.getX() * 16);
		double startY = character.getPosition().getY() + 35 + (facing.getY() * 16);
		Projectile projectile = new Projectile(
				projectileType,
				player.getId(),
				startX,
				startY,
				facing.getX() * speed,
				facing.getY() * speed,
				power,
				damage,
				maxDistance
		);
		battleField.addEntity(projectile);
	}

	private void spawnMeleeAttack(PlayerInfo player, GameCharacter character, double power) {
		double width = character.getMeleeWidth() * Math.min(1.4, power);
		double height = character.getMeleeHeight() * Math.min(1.3, power);
		double offset = character.getMeleeOffset();
		int lifetime = character.getMeleeLifetimeTicks();
		double damage = character.getAttack();
		double baseX = character.getPosition().getX();
		double baseY = character.getPosition().getY();
		Vector2D facing = getFacingDirection(character);
		double offsetX = facing.getX() * offset;
		double offsetY = facing.getY() * offset;
		AttackHitbox front = new AttackHitbox(
				player.getId(),
				damage,
				baseX + offsetX,
				baseY + offsetY,
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
				baseX - offsetX,
				baseY - offsetY,
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
		if (!character.canJump()) return false;
		character.setVerticalVelocity(character.getJumpVelocity());
		character.registerJump();
		return true;
	}

	private void setFacingDirection(PlayerInfo player, double x, double y) {
		GameCharacter character = player.getCharacter();
		character.setFacingDirection(x, y);
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
