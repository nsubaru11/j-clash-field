package model;

import java.util.ArrayList;
import java.util.List;

public final class ResultData {
	private static final String ENTRY_DELIMITER = ";";
	private static final String FIELD_DELIMITER = "|";
	private static final String FIELD_SPLIT_REGEX = "\\|";

	private final int id;
	private ResultType result;
	private int kills;
	private int deaths;
	private double damageGiven;
	private double damageTaken;

	public ResultData(int id) {
		this.id = id;
		reset();
	}

	public int getId() {
		return id;
	}

	public ResultType getResult() {
		return result;
	}

	public int getKills() {
		return kills;
	}

	public int getDeaths() {
		return deaths;
	}

	public double getDamageGiven() {
		return damageGiven;
	}

	public double getDamageTaken() {
		return damageTaken;
	}

	public void reset() {
		result = ResultType.LOSE;
		kills = 0;
		deaths = 0;
		damageGiven = 0;
		damageTaken = 0;
	}

	public void incrementKills() {
		kills++;
	}

	public void incrementDeaths() {
		deaths++;
	}

	public void addDamageGiven(double damage) {
		if (damage > 0) damageGiven += damage;
	}

	public void addDamageTaken(double damage) {
		if (damage > 0) damageTaken += damage;
	}

	public void setResult(ResultType result) {
		this.result = result == null ? ResultType.LOSE : result;
	}

	public String toProtocolString() {
		return id + FIELD_DELIMITER
				+ result.getId() + FIELD_DELIMITER
				+ kills + FIELD_DELIMITER
				+ deaths + FIELD_DELIMITER
				+ damageGiven + FIELD_DELIMITER
				+ damageTaken;
	}

	public static ResultData fromProtocolString(String token) {
		if (token == null || token.isEmpty()) return null;
		String[] fields = token.split(FIELD_SPLIT_REGEX);
		if (fields.length < 6) return null;
		int id = Integer.parseInt(fields[0]);
		ResultType type = ResultType.fromId(Integer.parseInt(fields[1]));
		int kills = Integer.parseInt(fields[2]);
		int deaths = Integer.parseInt(fields[3]);
		double damageGiven = Double.parseDouble(fields[4]);
		double damageTaken = Double.parseDouble(fields[5]);
		ResultData data = new ResultData(id);
		data.result = type;
		data.kills = kills;
		data.deaths = deaths;
		data.damageGiven = damageGiven;
		data.damageTaken = damageTaken;
		return data;
	}

	public static String serializeList(Iterable<ResultData> results) {
		if (results == null) return "";
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (ResultData data : results) {
			if (data == null) continue;
			if (!first) sb.append(ENTRY_DELIMITER);
			sb.append(data.toProtocolString());
			first = false;
		}
		return sb.toString();
	}

	public static List<ResultData> parseList(String payload) {
		List<ResultData> list = new ArrayList<>();
		if (payload == null || payload.isEmpty()) return list;
		String[] tokens = payload.split(ENTRY_DELIMITER);
		for (String token : tokens) {
			ResultData data = fromProtocolString(token);
			if (data != null) list.add(data);
		}
		return list;
	}

	@Override
	public String toString() {
		return toProtocolString();
	}

	public enum ResultType {
		LOSE(0, "Lose"),
		WIN(1, "Win"),
		DRAW(2, "Draw");

		private final int id;
		private final String label;

		ResultType(int id, String label) {
			this.id = id;
			this.label = label;
		}

		public int getId() {
			return id;
		}

		public String getLabel() {
			return label;
		}

		public static ResultType fromId(int id) {
			for (ResultType type : values()) {
				if (type.id == id) return type;
			}
			return LOSE;
		}
	}
}
