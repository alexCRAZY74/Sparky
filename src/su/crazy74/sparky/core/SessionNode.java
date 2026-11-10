package su.crazy74.sparky.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import su.crazy74.sparky.core.Storage;
import java.util.HashMap;
import java.util.Map;

public class SessionNode extends Storage {

	public SessionNode() {
		this.data = new HashMap<>();
	}

	public SessionNode(Map<String, Object> data) {
		this.data = data != null ? data : new HashMap<>();
	}

	public static SessionNode unsafeParseJson(String json) {
		int start = json.indexOf("{");
		if (start == -1) {
			throw new IllegalArgumentException("JSON не найден");
		}

		int bracketCount = 0;
		boolean inString = false;
		int end = -1;

		for (int i = start; i < json.length(); i++) {
			char c = json.charAt(i);

			// Проверяем, не находимся ли мы внутри строки (чтобы игнорировать скобки в коде)
			if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
				inString = !inString;
			}

			if (!inString) {
				if (c == '{') {
					bracketCount++;
				}
				if (c == '}') {
					bracketCount--;
					if (bracketCount == 0) {
						end = i;
						break;
					}
				}
			}
		}

		if (end != -1) {
			json = json.substring(start, end + 1);
		}

		// Никаких жестких замен слэшей! Отдаем чистый валидный кусок.
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
		}.getType());
		return new SessionNode(map);
	}

	public static SessionNode fromJson(String json) {
		if (json == null || json.trim().isEmpty()) {
			return new SessionNode();
		}
		try {
			return unsafeParseJson(json);
		} catch (Exception e) {
			// Хоть в консоль пускай плюнет, чтобы не гадать в темноте
			AppHandler.toConsole("SessionNode.fromJson КРИТИЧЕСКАЯ ОШИБКА: " + e.getMessage());
			return new SessionNode();
		}
	}
}
