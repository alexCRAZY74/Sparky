package su.crazy74.sparky.ai;

import su.crazy74.sparky.core.Storage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeminiProvider extends BaseAIProvider {

	public GeminiProvider(aiModelHandler modelHandler) {
		super(modelHandler, "GeminiProvider");
	}

	public static BaseAIProvider Construct(aiModelHandler model) {
		return new GeminiProvider(model);
	}

	@Override
	protected String getMessagesKey() {
		// У Гугла корневой ключ для истории называется "contents", а не "messages"
		return "contents";
	}
	
	@Override
	protected void preparePayload(Storage payload, List<String> data) throws Exception {
		// Собираем правильный generationConfig через чистый Map
		Map<String, Object> generationConfig = new HashMap<>();
		generationConfig.put("temperature", 0.0);
		generationConfig.put("responseMimeType", "application/json");
		
		// Запихиваем конфигурацию в корень payload
		payload.set("generationConfig", generationConfig);

		// Вытаскиваем системный промпт (индекс 0) и пихаем его в root-поле systemInstruction
		if (data != null && !data.isEmpty() && data.get(0) != null) {
			String sysPrompt = data.get(0).trim();
			if (!sysPrompt.isEmpty()) {
				Map<String, Object> part = new HashMap<>();
				part.put("text", sysPrompt);

				List<Map<String, Object>> parts = new ArrayList<>();
				parts.add(part);

				Map<String, Object> systemInstruction = new HashMap<>();
				systemInstruction.put("parts", parts);

				payload.set("systemInstruction", systemInstruction);
			}
		}
	}

	@Override
	protected Object buildMessages(List<String> data) throws Exception {
		List<Map<String, Object>> contents = new ArrayList<>();
		
		if (data != null && !data.isEmpty()) {
			int size = data.size();
			
			// Начинаем с i = 1, так как i = 0 (system) уже ушел в systemInstruction
			for (int i = 1; i < size; i++) {
				String content = data.get(i);
				if (content == null || content.trim().isEmpty()) {
					continue;
				}

				Map<String, Object> part = new HashMap<>();
				part.put("text", content.trim());
				
				List<Map<String, Object>> parts = new ArrayList<>();
				parts.add(part);

				Map<String, Object> contentNode = new HashMap<>();
				contentNode.put("parts", parts);
				
				// Распределяем роли по спецификации Gemini: строго "user" или "model"
				if (i == size - 1) {
					contentNode.put("role", "user");
				} else {
					// Индекс 1 — первый запрос пользователя в истории диалога, далее чередуем
					contentNode.put("role", (i % 2 == 1) ? "user" : "model");
				}

				contents.add(contentNode);
			}
		}
		return contents;
	}

	@Override
	protected String extractAiText(Storage responseData) throws Exception {
		// Родной путь в JSON-ответе Google Gemini API
		return responseData.getStr("candidates/0/content/parts/0/text");
	}
}