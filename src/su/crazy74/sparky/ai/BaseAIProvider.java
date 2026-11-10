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


public abstract class BaseAIProvider implements i_RequestAI {

	protected enum LogMode { ALWAYS, ON_ERROR }
	protected static final LogMode DEBUG_LOG_MODE = LogMode.ON_ERROR;

	protected aiModelHandler modelHandler;
	protected String providerName;

	public BaseAIProvider(aiModelHandler modelHandler, String providerName) {		this.modelHandler = modelHandler;
		this.providerName = providerName;
	}

	// Наследники сами собирают сообщения/контент в зависимости от спецификации API
	protected abstract Object buildMessages(List<String> data) throws Exception;

	// Переопределяется в наследниках, если структура payload требует специфичных настроек
	protected abstract void preparePayload(Storage payload, List<String> data) throws Exception;

	// Как конкретный провайдер выковыривает текст из полученного ответа
	protected abstract String extractAiText(Storage responseData) throws Exception;

	@Override
	public SessionNode Request(List<String> data) {
		SessionNode result = new SessionNode();
		String payloadJson = "";
		String finalUrl = "";

		try {
			// Используем готовые методы хэндлера — он сам разберётся с шаблонами и массивами ключей
			finalUrl = modelHandler.getURL();
			String apiKey = modelHandler.getApiKey();
			String authType = String.valueOf(modelHandler.config("provider/authorization"));
			String modelName = String.valueOf(modelHandler.config("model"));

			// Корневой объект запроса
			Storage payload = new Storage();
			payload.set("model", modelName);

			// Даём наследнику докинуть свои специфичные поля (response_format, systemInstruction и т.д.)
			preparePayload(payload, data);

			// Накатываем сформированную историю сообщений
			payload.set(getMessagesKey(), buildMessages(data));

			payloadJson = payload.toJson();
			//AppHandler.toConsole(providerName + " payload: " + payload.toFmtJson());

			// Настраиваем HTTP-транспорт
			HttpClient client = HttpClient.newBuilder()
							.connectTimeout(Duration.ofSeconds(30))
							.build();

			// Без таймаута на сам запрос, чтобы долгие модели (SiliconFlow) могли думать минутами
			HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
							.uri(URI.create(finalUrl))
							.header("Content-Type", "application/json")
							.POST(HttpRequest.BodyPublishers.ofString(payloadJson));

			// Если авторизация требует Bearer-токен в заголовках
			if ("bearer".equalsIgnoreCase(authType) && !apiKey.isEmpty()) {
				requestBuilder.header("Authorization", "Bearer " + apiKey);
			}

			AppHandler.toConsole(providerName + " send ");
			// Отправляем запрос и получаем ответ в виде массива байт
			HttpResponse<byte[]> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
			String body = "";

			// Проверяем, пожал ли сервер ответ в gzip
			String encoding = response.headers().firstValue("Content-Encoding").orElse("");
			AppHandler.toConsole(providerName + " encoding: " + encoding);

			if ("gzip".equalsIgnoreCase(encoding)) {
				// Распаковываем стандартным встроенным GZIPInputStream из java.util.zip
				try (java.util.zip.GZIPInputStream gis = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(response.body()))) {
					body = new String(gis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
				}

			} else {
				// Если прилетел обычный несжатый текст (как от SiliconFlow)
				body = new String(response.body(), java.nio.charset.StandardCharsets.UTF_8);
			}

			int statusCode = response.statusCode();
			boolean isSuccess = statusCode == 200;

			if (!isSuccess) {
				result.set("waitAnswer", false);
				result.set("success", false);
				result.set("error", "HTTP " + statusCode);

				boolean isRetryable = (statusCode == 503 || statusCode == 429 || statusCode == 500 || statusCode == 502 || statusCode == 504);
				boolean isBusyOrQuota = false;

				try {
					Storage errorJson = Storage.fromJson(body);
					result.set("error/details", errorJson);

					String message = "";
					if (errorJson.Exists("error/message")) {
						message = errorJson.getStr("error/message");
					} else if (errorJson.Exists("message")) {
						message = errorJson.getStr("message");
					}

					if (!message.isEmpty()) {
						result.set("error/message", "Ошибка " + providerName + ": " + message);
						String lowerMsg = message.toLowerCase();
						if (lowerMsg.contains("high demand") || lowerMsg.contains("overloaded") || lowerMsg.contains("rate limit") || lowerMsg.contains("quota exceeded") || lowerMsg.contains("resource exhausted")) {
							isRetryable = true;
							isBusyOrQuota = true;
						}
					} else {
						result.set("error/message", "Ошибка " + providerName + " (код " + statusCode + ")");
					}
				} catch (Exception jsonEx) {
					String trimmedBody = body.trim();
					result.set("error/message", "Ошибка " + providerName + " [HTTP " + statusCode + "]: " + trimmedBody);
					String lowerBody = trimmedBody.toLowerCase();
					if (lowerBody.contains("high demand") || lowerBody.contains("overloaded") || lowerBody.contains("rate limit") || lowerBody.contains("quota exceeded") || lowerBody.contains("resource exhausted")) {
						isRetryable = true;
						isBusyOrQuota = true;
					}
				}

				result.set("error/retryable", isRetryable);

				if (DEBUG_LOG_MODE == LogMode.ALWAYS || !isBusyOrQuota) {
					writeDebugLog("HTTP_ERROR_" + statusCode, finalUrl, payloadJson, body);
				}

				return result;
			}

			if (DEBUG_LOG_MODE == LogMode.ALWAYS) {
				writeDebugLog("SUCCESS", finalUrl, payloadJson, body);
			}

			Storage responseData = Storage.fromJson(body);
			AppHandler.toConsole(providerName + " responseData ");

			String aiText = extractAiText(responseData);
			if (aiText == null) {
				throw new Exception("Не удалось извлечь текст ответа из структуры API.");
			}

			// ИЗОЛИРУЕМ ПАРСИНГ: оборачиваем в отдельный try-catch, чтобы защитить поток от залипания
			try {
				AppHandler.toConsole(providerName + " попытка парсинга SessionNode...");
				result = SessionNode.unsafeParseJson(aiText.trim());
				AppHandler.toConsole(providerName + " SessionNode успешно распарсен.");
			} catch (Exception parseEx) {
				AppHandler.toConsole(providerName + " КРИТИЧЕСКАЯ ОШИБКА ПАРСИНГА SessionNode: " + parseEx.getMessage());
				// Если метод изнутри вешается/падает — не ломаем UI, а аккуратно пихаем текст как ответ
				result = new SessionNode();
				result.set("answer", aiText.trim());
				result.set("waitAnswer", false);
				result.set("success", false);
				result.set("error", "Ошибка парсинга структуры ответа: " + parseEx.getMessage());
			}

			populateNodeUsage(result, responseData, this.providerName);
			result.set("ai-model", modelHandler.config("ai-model"));

			// В любом случае при успехе сетевого запроса сбрасываем флаг ожидания для фронта
			//result.set("waitAnswer", false);
			//result.set("success", true);
		} catch (java.net.http.HttpTimeoutException e) {
			writeDebugLog("TIMEOUT: " + e.getMessage(), finalUrl, payloadJson, "");
			AppHandler.toConsole(providerName + " Timeout: " + e.getMessage());

			result.set("waitAnswer", false);
			result.set("success", false);
			result.set("error", "Timeout");
			result.set("error/retryable", true);
			result.set("error/message", "Ошибка " + providerName + " (Таймаут): " + e.getMessage());

		} catch (Exception e) {
			String errDetail = e.toString();
      writeDebugLog("ERROR: " + errDetail, finalUrl, payloadJson, "");
      AppHandler.toConsole(providerName + " Error: " + errDetail);

			result.set("waitAnswer", false);
			result.set("success", false);
			result.set("error", "Connection Error");
			result.set("error/retryable", false);
			result.set("error/message", "Ошибка " + providerName + ": " + e.getMessage());
		}
		return result;
	}

	// По умолчанию для OpenAI-совместимых это "messages". Gemini переопределит на "contents"
	protected String getMessagesKey() {
		return "messages";
	}

	protected void populateNodeUsage(SessionNode node, Storage responseData, String providerType) {
		try {
			if ("GeminiProvider".equals(providerType)) {
				if (responseData.Exists("usageMetadata")) {
					node.set("metadata/tokens_prompt", responseData.getInt("usageMetadata/promptTokenCount"));
					node.set("metadata/tokens_answer", responseData.getInt("usageMetadata/candidatesTokenCount"));
					node.set("metadata/tokens_total", responseData.getInt("usageMetadata/totalTokenCount"));
				}
				if (responseData.Exists("modelVersion")) {
					node.set("metadata/model_version", responseData.getStr("modelVersion"));
				}
			} else if ("OpenAIProvider".equals(providerType)) {
				if (responseData.Exists("usage")) {
					node.set("metadata/tokens_prompt", responseData.getInt("usage/prompt_tokens"));
					node.set("metadata/tokens_answer", responseData.getInt("usage/completion_tokens"));
					node.set("metadata/tokens_total", responseData.getInt("usage/total_tokens"));
				}
				if (responseData.Exists("model")) {
					node.set("metadata/model_version", responseData.getStr("model"));
				}
			}
		} catch (Exception e) {
			AppHandler.toConsole("Ошибка统一фикации метаданных: " + e.getMessage());
		}
	}

	private void writeDebugLog(String status, String url, String payload, String response) {
		try {
			java.io.File logFile = new java.io.File(".sparky/api_debug.log");
			java.io.File parent = logFile.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}

			java.time.LocalDateTime now = java.time.LocalDateTime.now();
			StringBuilder logEntry = new StringBuilder();
			logEntry.append("=== ").append(now.toString()).append(" [").append(providerName).append("] ===\n");
			logEntry.append("API: ").append(this.getClass().toString()).append("\n");
			logEntry.append("MODEL: ").append((String) modelHandler.config("model")).append("\n");
			logEntry.append("STATUS: ").append(status).append("\n");
			logEntry.append("URL: ").append(url).append("\n");
			logEntry.append("PAYLOAD:\n").append(payload).append("\n");
			if (response != null && !response.isEmpty()) {
				logEntry.append("RESPONSE:\n").append(response).append("\n");
			}
			logEntry.append("=========================================\n\n");

			java.nio.file.Files.writeString(
							logFile.toPath(),
							logEntry.toString(),
							java.nio.charset.StandardCharsets.UTF_8,
							java.nio.file.StandardOpenOption.CREATE,
							java.nio.file.StandardOpenOption.APPEND
			);
		} catch (Exception ex) {
			AppHandler.toConsole("Не удалось записать api_debug.log: " + ex.getMessage());
		}
	}
}
