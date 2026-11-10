package su.crazy74.sparky.ai;

import su.crazy74.sparky.core.AppHandler;
import su.crazy74.sparky.core.SessionNode;
import su.crazy74.sparky.core.Storage;
import su.crazy74.sparky.interfaces.i_RequestAI;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class DebugEmulator implements i_RequestAI {

	private String URL = "http://localhost:12717/index.php?class=GermanAI&method=QueryDebug";

	public static i_RequestAI Construct(aiModelHandler model){
		return new DebugEmulator();
	};

	@Override
	public SessionNode Request(List<String> data) {
		//AppHandler.toConsole(" --- DebugEmulator.Request");
		SessionNode result = new SessionNode(); // Создаем пустой Storage для ответа

		try {
			// 1. Готовим JSON из списка строк (data)[cite: 3]
			//String jsonPayload = new Gson().toJson(data);
			String jsonPayload = "";
			if (data != null && !data.isEmpty()) {
				jsonPayload = jsonPayload + "[\r\n";
				Boolean isFirst = true;
				for (String line : data) {
					Storage part = new Storage();
					part.set("content", line);
					if (isFirst) {
						isFirst = false;
					} else {
						jsonPayload = jsonPayload + ",\r\n";
					}
					jsonPayload = jsonPayload + part.toJson();
				}
				jsonPayload = jsonPayload + "\r\n]";
			}

			// 2. Настраиваем HTTP-клиент
			HttpClient client = HttpClient.newBuilder()
							.connectTimeout(Duration.ofSeconds(10))
							.build();

			// 3. Формируем запрос к твоему PHP-методу QueryDebug[cite: 3]
			//AppHandler.toConsole("jsonPayload: " + jsonPayload);
			HttpRequest request = HttpRequest.newBuilder()
							.uri(URI.create(URL))
							.header("Content-Type", "application/json") // Чтобы PHP понял, что это JSON
							.POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
							.build();

			// 4. Отправляем и получаем ответ
			// В методе Request после получения ответа
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			// Печатаем само тело ответа, а не объект HttpResponse
			//AppHandler.toConsole("Raw Response Body: " + response.body());

			if (response.body() == null || response.body().isEmpty()) {
				AppHandler.toConsole("Внимание: PHP вернул пустую строку!");
			}

			result = SessionNode.fromJson(response.body());
			//AppHandler.toConsole("result.toFmtJson(): " + result.toFmtJson());

		} catch (Exception e) {
			AppHandler.toConsole("Exception: " + e.getMessage());
			// Если всё упало, пишем ошибку в Storage, чтобы JS об этом узнал
			result.set("error/message", "Ошибка связи с эмулятором: " + e.getMessage());
		}

		return result;
	}
}
