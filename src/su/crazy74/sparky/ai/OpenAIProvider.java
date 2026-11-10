package su.crazy74.sparky.ai;

import su.crazy74.sparky.core.Storage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAIProvider extends BaseAIProvider {

	public OpenAIProvider(aiModelHandler modelHandler) {
		super(modelHandler, "OpenAIProvider");
	}

	public static BaseAIProvider Construct(aiModelHandler model) {
		return new OpenAIProvider(model);
	}

	@Override
	protected void preparePayload(Storage payload, List<String> data) throws Exception {
		payload.set("temperature", 0.0);

		// Для OpenAI/SiliconAI формат ответа JSON задается объектом response_format
		Map<String, Object> resFormat = new HashMap<>();
		resFormat.put("type", "json_object");
		
		payload.set("response_format", resFormat);
	}

	@Override
	protected Object buildMessages(List<String> data) throws Exception {
		List<Map<String, Object>> messages = new ArrayList<>();
		
		if (data != null && !data.isEmpty()) {
			int size = data.size();
			for (int i = 0; i < size; i++) {
				String content = data.get(i);
				if (content == null || content.trim().isEmpty()) {
					continue; 
				}

				Map<String, Object> msg = new HashMap<>();
				if (i == 0) {
					msg.put("role", "system");
				} else if (i == size - 1) {
					msg.put("role", "user");
				} else {
					msg.put("role", (i % 2 == 1) ? "user" : "assistant");
				}

				msg.put("content", content.trim());
				messages.add(msg);
			}
		}
		return messages;
	}

	@Override
	protected String extractAiText(Storage responseData) throws Exception {
		return responseData.getStr("choices/0/message/content");
	}
}