package model;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CharacterInfo {
	private static final String RESOURCE_PATH = "/resorces/character_info.json";
	private static final double DEFAULT_MOVE_STEP = 6.0;
	private static final double DEFAULT_PROJECTILE_SPEED = 9.0;
	private static final double DEFAULT_MELEE_WIDTH = 180.0;
	private static final double DEFAULT_MELEE_HEIGHT = 70.0;
	private static final double DEFAULT_MELEE_OFFSET = 60.0;
	private static final int DEFAULT_MELEE_LIFETIME_TICKS = 2;
	private static final CharacterInfo FALLBACK = new CharacterInfo(
			" ",
			Color.BLACK,
			" ",
			0.0,
			0.0,
			0.0,
			0.0,
			0.0,
			0.0,
			DEFAULT_MOVE_STEP,
			DEFAULT_MOVE_STEP,
			DEFAULT_PROJECTILE_SPEED,
			0.0,
			DEFAULT_MELEE_WIDTH,
			DEFAULT_MELEE_HEIGHT,
			DEFAULT_MELEE_OFFSET,
			DEFAULT_MELEE_LIFETIME_TICKS
	);
	private static final EnumMap<CharacterType, CharacterInfo> CACHE = new EnumMap<>(CharacterType.class);

	static {
		load();
	}

	private final String name; // キャラクター名
	private final Color themeColor; // キャラクターのテーマ色
	private final String description; // キャラクターの説明文
	private final double attackMin; // 最低攻撃力
	private final double attackMax; // 最高攻撃力
	private final double attackChargeTimeMs; // 攻撃力の充電時間
	private final double defense; // 防御力
	private final double defenseChargeTimeMs; // 防御力の持続時間
	private final double hp; // 最大HP
	private final double moveStepX; // 横方向移動速度
	private final double moveStepY; // 縦方向移動速度
	private final double projectileSpeed; // 射撃速度
	private final double projectileRange; // 射撃範囲
	private final double meleeWidth; // 近接攻撃の幅
	private final double meleeHeight; // 近接攻撃の高さ
	private final double meleeOffset; // 近接攻撃のオフセット
	private final int meleeLifetimeTicks; // 近接攻撃の持続時間

	private CharacterInfo(
			String name,
			Color themeColor,
			String description,
			double attackMin,
			double attackMax,
			double attackChargeTimeMs,
			double defense,
			double defenseChargeTimeMs,
			double hp,
			double moveStepX,
			double moveStepY,
			double projectileSpeed,
			double projectileRange,
			double meleeWidth,
			double meleeHeight,
			double meleeOffset,
			int meleeLifetimeTicks
	) {
		this.name = name == null ? "" : name;
		this.themeColor = themeColor == null ? Color.BLACK : themeColor;
		this.description = description == null ? "" : description;
		this.attackMin = attackMin;
		this.attackMax = attackMax;
		this.attackChargeTimeMs = attackChargeTimeMs;
		this.defense = defense;
		this.defenseChargeTimeMs = defenseChargeTimeMs;
		this.hp = hp;
		this.moveStepX = moveStepX;
		this.moveStepY = moveStepY;
		this.projectileSpeed = projectileSpeed;
		this.projectileRange = projectileRange;
		this.meleeWidth = meleeWidth;
		this.meleeHeight = meleeHeight;
		this.meleeOffset = meleeOffset;
		this.meleeLifetimeTicks = meleeLifetimeTicks;
	}

	public static CharacterInfo forType(CharacterType type) {
		if (type == null) return FALLBACK;
		CharacterInfo info = CACHE.get(type);
		return info != null ? info : FALLBACK;
	}

	private static void load() {
		EnumMap<CharacterType, CharacterInfo> map = new EnumMap<>(CharacterType.class);
		for (CharacterType type : CharacterType.values()) {
			map.put(type, FALLBACK);
		}
		try (InputStream stream = CharacterInfo.class.getResourceAsStream(RESOURCE_PATH)) {
			if (stream == null) {
				CACHE.clear();
				CACHE.putAll(map);
				return;
			}
			String json = readUtf8(stream);
			Object rootValue = new JsonParser(json).parseValue();
			Map<String, Object> root = asObject(rootValue);
			Map<String, Object> characters = root != null ? asObject(root.get("characters")) : null;
			if (characters != null) {
				for (CharacterType type : CharacterType.values()) {
					Object entry = characters.get(type.name());
					Map<String, Object> data = asObject(entry);
					if (data == null) continue;
					map.put(type, buildInfo(type, data));
				}
			}
		} catch (IOException | RuntimeException e) {
			map.clear();
			for (CharacterType type : CharacterType.values()) {
				map.put(type, FALLBACK);
			}
		}
		CACHE.clear();
		CACHE.putAll(map);
	}

	private static CharacterInfo buildInfo(CharacterType type, Map<String, Object> data) {
		String fallbackName = type == null ? "" : type.name();
		String name = asString(data.get("name"), fallbackName);
		Color themeColor = parseColor(data.get("themeColor"), FALLBACK.themeColor);
		String description = asString(data.get("description"), "");
		double attackMin = Math.max(0.0, asDouble(data.get("attackMin"), 0.0));
		double attackMax = Math.max(attackMin, asDouble(data.get("attackMax"), attackMin));
		double attackChargeTimeMs = Math.max(0.0, asDouble(data.get("attackChargeTimeMs"),
				asDouble(data.get("attackChargeTime"), 0.0)));
		double defense = Math.max(0.0, asDouble(data.get("defense"), 0.0));
		double defenseChargeTimeMs = Math.max(0.0, asDouble(data.get("defenseChargeTimeMs"),
				asDouble(data.get("defenseChargeTime"), asDouble(data.get("defenceChargeTime"), 0.0))));
		double hp = Math.max(0.0, asDouble(data.get("hp"), 0.0));
		double moveStepX = asDouble(data.get("speedX"), asDouble(data.get("moveStepX"), DEFAULT_MOVE_STEP));
		double moveStepY = asDouble(data.get("speedY"), asDouble(data.get("moveStepY"), DEFAULT_MOVE_STEP));
		double projectileSpeed = asDouble(data.get("projectileSpeed"), DEFAULT_PROJECTILE_SPEED);
		double projectileRange = Math.max(0.0, asDouble(data.get("projectileRange"), 0.0));
		double meleeWidth = asDouble(data.get("meleeWidth"), DEFAULT_MELEE_WIDTH);
		double meleeHeight = asDouble(data.get("meleeHeight"), DEFAULT_MELEE_HEIGHT);
		double meleeOffset = asDouble(data.get("meleeOffset"), DEFAULT_MELEE_OFFSET);
		int meleeLifetimeTicks = asInt(data.get("meleeLifetimeTicks"), DEFAULT_MELEE_LIFETIME_TICKS);
		return new CharacterInfo(
				name,
				themeColor,
				description,
				attackMin,
				attackMax,
				attackChargeTimeMs,
				defense,
				defenseChargeTimeMs,
				hp,
				moveStepX,
				moveStepY,
				projectileSpeed,
				projectileRange,
				meleeWidth,
				meleeHeight,
				meleeOffset,
				meleeLifetimeTicks
		);
	}

	private static String readUtf8(InputStream stream) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] data = new byte[4096];
		int count;
		while ((count = stream.read(data)) != -1) {
			buffer.write(data, 0, count);
		}
		return buffer.toString(StandardCharsets.UTF_8.name());
	}

	private static String asString(Object value, String fallback) {
		if (value instanceof String) return ((String) value).trim();
		return fallback;
	}

	private static double asDouble(Object value, double fallback) {
		if (value instanceof Number) return ((Number) value).doubleValue();
		if (value instanceof String) {
			try {
				return Double.parseDouble(((String) value).trim());
			} catch (NumberFormatException ignored) {
				return fallback;
			}
		}
		return fallback;
	}

	private static int asInt(Object value, int fallback) {
		if (value instanceof Number) return ((Number) value).intValue();
		if (value instanceof String) {
			try {
				return Integer.parseInt(((String) value).trim());
			} catch (NumberFormatException ignored) {
				return fallback;
			}
		}
		return fallback;
	}

	private static Color parseColor(Object value, Color fallback) {
		if (!(value instanceof String)) return fallback;
		String raw = ((String) value).trim();
		if (raw.isEmpty()) return fallback;
		String hex = raw;
		if (hex.startsWith("#")) {
			hex = hex.substring(1);
		} else if (hex.startsWith("0x") || hex.startsWith("0X")) {
			hex = hex.substring(2);
		}
		try {
			int colorValue = (int) Long.parseLong(hex, 16);
			if (hex.length() == 8) {
				int a = (colorValue >> 24) & 0xFF;
				int r = (colorValue >> 16) & 0xFF;
				int g = (colorValue >> 8) & 0xFF;
				int b = colorValue & 0xFF;
				return new Color(r, g, b, a);
			}
			if (hex.length() == 6) {
				int r = (colorValue >> 16) & 0xFF;
				int g = (colorValue >> 8) & 0xFF;
				int b = colorValue & 0xFF;
				return new Color(r, g, b);
			}
			if (hex.length() == 3) {
				int r = (colorValue >> 8) & 0xF;
				int g = (colorValue >> 4) & 0xF;
				int b = colorValue & 0xF;
				return new Color(r << 4 | r, g << 4 | g, b << 4 | b);
			}
		} catch (NumberFormatException ignored) {
			return fallback;
		}
		return fallback;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asObject(Object value) {
		if (value instanceof Map) return (Map<String, Object>) value;
		return null;
	}

	public String getName() {
		return name;
	}

	public Color getThemeColor() {
		return themeColor;
	}

	public String getDescription() {
		return description;
	}

	public double getAttackMin() {
		return attackMin;
	}

	public double getAttackMax() {
		return attackMax;
	}

	public double getAttackChargeTimeMs() {
		return attackChargeTimeMs;
	}

	public double getDefense() {
		return defense;
	}

	public double getDefenseChargeTimeMs() {
		return defenseChargeTimeMs;
	}

	public double getHp() {
		return hp;
	}

	public double getMoveStepX() {
		return moveStepX;
	}

	public double getMoveStepY() {
		return moveStepY;
	}

	public double getProjectileSpeed() {
		return projectileSpeed;
	}

	public double getProjectileRange() {
		return projectileRange;
	}

	public double getMeleeWidth() {
		return meleeWidth;
	}

	public double getMeleeHeight() {
		return meleeHeight;
	}

	public double getMeleeOffset() {
		return meleeOffset;
	}

	public int getMeleeLifetimeTicks() {
		return meleeLifetimeTicks;
	}

	private static final class JsonParser {
		private final String text;
		private int index;

		private JsonParser(String text) {
			this.text = text == null ? "" : text;
		}

		private Object parseValue() {
			skipWhitespace();
			char c = peek();
			if (c == '{') return parseObject();
			if (c == '[') return parseArray();
			if (c == '"') return parseString();
			if (c == 't' || c == 'f') return parseBoolean();
			if (c == 'n') return parseNull();
			return parseNumber();
		}

		private Map<String, Object> parseObject() {
			expect('{');
			Map<String, Object> map = new LinkedHashMap<>();
			skipWhitespace();
			if (peek() == '}') {
				index++;
				return map;
			}
			while (true) {
				skipWhitespace();
				String key = parseString();
				skipWhitespace();
				expect(':');
				Object value = parseValue();
				map.put(key, value);
				skipWhitespace();
				char c = peek();
				if (c == ',') {
					index++;
					continue;
				}
				if (c == '}') {
					index++;
					break;
				}
				throw new IllegalStateException("Unexpected character in object: " + c);
			}
			return map;
		}

		private List<Object> parseArray() {
			expect('[');
			List<Object> list = new ArrayList<>();
			skipWhitespace();
			if (peek() == ']') {
				index++;
				return list;
			}
			while (true) {
				Object value = parseValue();
				list.add(value);
				skipWhitespace();
				char c = peek();
				if (c == ',') {
					index++;
					continue;
				}
				if (c == ']') {
					index++;
					break;
				}
				throw new IllegalStateException("Unexpected character in array: " + c);
			}
			return list;
		}

		private String parseString() {
			expect('"');
			StringBuilder sb = new StringBuilder();
			while (index < text.length()) {
				char c = text.charAt(index++);
				if (c == '"') break;
				if (c == '\\') {
					if (index >= text.length()) break;
					char esc = text.charAt(index++);
					switch (esc) {
						case '"':
							sb.append('"');
							break;
						case '\\':
							sb.append('\\');
							break;
						case '/':
							sb.append('/');
							break;
						case 'b':
							sb.append('\b');
							break;
						case 'f':
							sb.append('\f');
							break;
						case 'n':
							sb.append('\n');
							break;
						case 'r':
							sb.append('\r');
							break;
						case 't':
							sb.append('\t');
							break;
						case 'u':
							sb.append(parseUnicode());
							break;
						default:
							sb.append(esc);
							break;
					}
				} else {
					sb.append(c);
				}
			}
			return sb.toString();
		}

		private char parseUnicode() {
			if (index + 4 > text.length()) return '?';
			String hex = text.substring(index, index + 4);
			index += 4;
			try {
				return (char) Integer.parseInt(hex, 16);
			} catch (NumberFormatException e) {
				return '?';
			}
		}

		private Object parseNumber() {
			int start = index;
			while (index < text.length()) {
				char c = text.charAt(index);
				if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
					index++;
				} else {
					break;
				}
			}
			String token = text.substring(start, index).trim();
			if (token.isEmpty()) return 0.0;
			try {
				return Double.parseDouble(token);
			} catch (NumberFormatException e) {
				throw new IllegalStateException("Invalid number: " + token, e);
			}
		}

		private Boolean parseBoolean() {
			if (text.startsWith("true", index)) {
				index += 4;
				return Boolean.TRUE;
			}
			if (text.startsWith("false", index)) {
				index += 5;
				return Boolean.FALSE;
			}
			throw new IllegalStateException("Invalid boolean at index " + index);
		}

		private Object parseNull() {
			if (text.startsWith("null", index)) {
				index += 4;
				return null;
			}
			throw new IllegalStateException("Invalid null at index " + index);
		}

		private void expect(char expected) {
			skipWhitespace();
			if (peek() != expected) {
				throw new IllegalStateException("Expected '" + expected + "' at index " + index);
			}
			index++;
		}

		private char peek() {
			if (index >= text.length()) return '\0';
			return text.charAt(index);
		}

		private void skipWhitespace() {
			while (index < text.length()) {
				char c = text.charAt(index);
				if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
					index++;
				} else {
					break;
				}
			}
		}
	}
}
