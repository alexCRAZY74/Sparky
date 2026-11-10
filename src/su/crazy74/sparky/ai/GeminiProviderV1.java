package su.crazy74.sparky.ai;

import su.crazy74.sparky.core.Storage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeminiProviderV1 extends BaseAIProvider {

	public GeminiProviderV1(aiModelHandler modelHandler) {
		super(modelHandler, "GeminiProviderV1");
	}

	public static BaseAIProvider Construct(aiModelHandler model) {
		return new GeminiProviderV1(model);
	}

	@Override
	protected String getMessagesKey() {
		return "contents";
	}

	@Override
	protected void preparePayload(Storage payload, List<String> data) throws Exception {
		// Выкидываем нахер systemInstruction и generationConfig, которые ломают v1 шлюз.
		// Оставляем только базовую температуру, если нужно, но лучше вообще не злить валидатор.
		Map<String, Object> generationConfig = new HashMap<>();
		generationConfig.put("temperature", 0.0);

		payload.set("generationConfig", generationConfig);
	}

	@Override
	protected Object buildMessages(List<String> data) throws Exception {
		List<Map<String, Object>> contents = new ArrayList<>();

		if (data == null || data.isEmpty()) {
			return contents;
		}

		// Внедряем системный промпт (индекс 0) как самое первое сообщение диалога
		if (data.get(0) != null && !data.get(0).trim().isEmpty()) {
			contents.add(createContentNode("user", data.get(0).trim()));
			// Добавим симулированный ответ модели, чтобы зафиксировать инструкцию
			contents.add(createContentNode("model", "Инструкция принята. Я готов работать строго в формате JSON сессии."));
		}

		StringBuilder userAccumulator = new StringBuilder();
		int size = data.size();

		for (int i = 1; i < size; i++) {
			String content = data.get(i);
			if (content == null || content.trim().isEmpty()) {
				continue;
			}

			boolean isModelResponse = content.contains("\"guid\":") && content.contains("\"stages\":");

			if (isModelResponse) {
				if (userAccumulator.length() > 0) {
					contents.add(createContentNode("user", userAccumulator.toString().trim()));
					userAccumulator.setLength(0);
				}
				contents.add(createContentNode("model", content.trim()));
			} else {
				if (userAccumulator.length() > 0) {
					userAccumulator.append("\n\n");
				}
				userAccumulator.append(content.trim());
			}
		}

		if (userAccumulator.length() > 0) {
			contents.add(createContentNode("user", userAccumulator.toString().trim()));
		}

		if (contents.isEmpty()) {
			contents.add(createContentNode("user", "Начать сессию"));
		}

		return contents;
	}

	private Map<String, Object> createContentNode(String role, String text) {
		Map<String, Object> part = new HashMap<>();
		part.put("text", text);

		List<Map<String, Object>> parts = new ArrayList<>();
		parts.add(part);

		Map<String, Object> node = new HashMap<>();
		node.put("role", role);
		node.put("parts", parts);
		return node;
	}

	@Override
	protected String extractAiText(Storage responseData) throws Exception {
		return responseData.getStr("candidates/0/content/parts/0/text");
	}
}
