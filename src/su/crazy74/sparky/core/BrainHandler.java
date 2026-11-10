package su.crazy74.sparky.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import su.crazy74.sparky.interfaces.i_Session;
import su.crazy74.sparky.ai.aiModelHandler;
import su.crazy74.sparky.interfaces.i_RequestAI;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

public class BrainHandler {

	private static BrainHandler Instance;

	public static BrainHandler getDefault() {
		if (Instance == null) {
			Instance = new BrainHandler();
		}
		return Instance;
	}

	@SuppressWarnings("unchecked")
	public static void RequestAI(i_Session session) {
		Boolean debugTrace = false;
		if (session.isInterrupted()) {
			return;
		}
		if (debugTrace) {
			AppHandler.toConsole(" - RequestAI()");
		}
		List<String> request = new java.util.ArrayList<>();

		aiModelHandler aiModel = aiModelHandler.Construct(session);

		// Параметры чата
		RequestPartJSON sessionPart = new RequestPartJSON(session.info().toFmtJson());
		sessionPart.title = "session-info";
		request.add(sessionPart.toString());

		// Окружение и самоидентификация
		Storage env = new Storage();
		env.set("model", aiModel.config("model"));
		env.set("model-title", aiModel.config("model-title"));
		env.set("current_time", java.time.ZonedDateTime.now().toString());
		env.set("timezone", java.time.ZoneId.systemDefault().toString());
		env.set("os", System.getProperty("os.name"));
		env.set("java_version", System.getProperty("java.version"));

		RequestPartJSON envPart = new RequestPartJSON(env.toFmtJson());
		envPart.title = "Окружение и самоидентификация";
		envPart.location = "ide://~netbeans/environment";
		request.add(envPart.toString());

		// Профиль пользователя
		RequestPartJSON userPart = new RequestPartJSON(StoredNotes.toFmtJson(AppHandler.getProfileFile()));
		userPart.title = "Профиль пользователя";
		request.add(userPart.toString());

		// Проект
		ProjectHandler hProject = ProjectHandler.BySession(session);
		if (hProject != null && hProject.getFolder() != null) {
			if (session.config().getBool("AlwaysSend/ProjectInfo")) {
				request.add(RequestPartJSON.Build(request, "ide://~project/info", "Проект",
								hProject.getInformation().toFmtJson()).toString());
			}
			if (session.config().getBool("AlwaysSend/ProjectFileTree")) {
				String projectTree = hProject.FileTree();
				if (projectTree != null) {
					request.add(RequestPart.Build(request, "file://~project/:tree", "Дерево проекта", projectTree).toString());
				}
			}
			try {
				AddFilePart(session, request, hProject.ToURI(hProject.getProfileFile()), "project_profile");
			} catch (URISyntaxException ex) {
				Exceptions.printStackTrace(ex);
				AppHandler.toConsole(ex.toString());
			}
			if (debugTrace) {
				AppHandler.toConsole(" - project_profile done");
			}

			Boolean TrainersExists;
			try {
				TrainersExists = AddFilePart(session, request, hProject.ToURI(hProject.getTrainersFile()), "Список файлов-тренеров");
			} catch (URISyntaxException ex) {
				TrainersExists = false;
				Exceptions.printStackTrace(ex);
				AppHandler.toConsole(ex.toString());
			}
			if (debugTrace) {
				AppHandler.toConsole(" - TrainersExists: " + TrainersExists);
			}
			if (TrainersExists) {
				if (debugTrace) {
					AppHandler.toConsole(" - CheckTrainers start");
				}
				CheckTrainers(request, session);
				if (debugTrace) {
					AppHandler.toConsole(" - CheckTrainers done");
				}
			}
		} else {
			if (session.config().getBool("AlwaysSend/ProjectInfo")) {
				String opened_projects = ideHandler.getOpenedProjectsJson();
				if (opened_projects != null) {
					request.add(RequestPartJSON.Build(request, "ide://~netbeans/opened_projects",
									"Открытые проекты в IDE", opened_projects).toString());
				}
			}
		}

		if (session.config().getBool("AlwaysSend/OpenTabs")) {
			String opened_tabs = ideHandler.getOpenedTabs(session.getProject());
			if (opened_tabs != null) {
				request.add(RequestPartJSON.Build(request, "ide://~netbeans/opened_tabs", "Открытые вкладки IDE", opened_tabs).toString());
			}
		}
		if (session.config().getBool("AlwaysSend/ProjectMapGuide")) {
			AddInstructionPart(session, request, "system://~prompts/ai_project_map_guide.md");
		}

		if (debugTrace) {
			AppHandler.toConsole(" - CheckNeeds start");
		}
		CheckNeeds(request, session);
		if (debugTrace) {
			AppHandler.toConsole(" - CheckNeeds done");
		}

		// Собираем историю
		List<SessionNode> history = session.getHistory();
		if (history != null) {
			for (SessionNode node : history) {
				// Пропускаем, если эта нода уже активная (чтобы не дублировать)
				if (node.getStr("guid").equals(session.getActive().getStr("guid"))) {
					continue;
				}

				request.add(new RequestPartJSON(node).toString());
			}
		}

		// Системный промпт (system_prompt.md)
		AddInstructionPart(session, request, "system://~prompts/system_prompt.md");

		// Добавляем текущий хвост
		SessionNode current = session.getActive();
		if (current != null) {
			current.Remove("metadata");
		}
		RequestPartJSON activePart = new RequestPartJSON(current);
		request.add(activePart.toString());

		i_RequestAI API = aiModel.getAPI();
		//SessionNode answer = API.Request(request);
		SessionNode answer;
		try {
			answer = API.Request(request);
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
		} catch (Exception e) {
			return;
		}
		//AppHandler.toConsole("answer: " + answer.toFmtJson());

		if (answer.Exists("answer")) {
			answer.set("answer/time", java.time.Instant.now().toString());
		}

		if (answer.Exists("session-title")) {
			SwingUtilities.invokeLater(() -> {
				session.setTitle(answer.getStr("session-title"));
			});
		}
		if (answer.Exists("profile_update")) {
			StoredNotes.Update(AppHandler.getProfileFile(), new Storage((java.util.Map) answer.get("profile_update")));
		}
		if (answer.Exists("project_update") && hProject != null) {
			AppHandler.toConsole("project_update: " + hProject.getProfileFile().toURI());
			StoredNotes.Update(hProject.getProfileFile(), new Storage((java.util.Map) answer.get("project_update")));
		}

		AppHandler.toConsole(answer.toFmtJson());
		if (answer.Exists("guid")) {
			if (!answer.getBool("success")) {
				answer.set("waitAnswer", true);
			}
			session.UpdateNode(answer);
			if (answer.getBool("success")) {
				// Страховка: если модель сказала true, но забыла прислать сам текст
				if (!answer.Exists("answer") || answer.getStr("answer/text").isEmpty()) {
					SessionNode finalNode = new SessionNode(session.getActive().get());
					finalNode.set("waitAnswer", false);
					finalNode.set("answer/text", "<p>Герман: Ошибка генерации. Модель завершила цикл, но прислала пустой ответ. Пожалуйста, сбрось контекст чата.</p>");
					session.UpdateNode(finalNode);
				} else {
					// Штатное завершение, разлочиваем UI
					SessionNode finalNode = new SessionNode(session.getActive().get());
					finalNode.set("waitAnswer", false);
					session.UpdateNode(finalNode);
				}
			} else {
				Thread worker = new Thread(() -> {
					try {
						Thread.sleep(5000);
						if (!session.isInterrupted()) {
							RequestAI(session);
						}
					} catch (Exception ex) {
					}
				});
				session.setThread(worker);
				worker.start();
			}
		} else {
			int retries = current.getInt("retry_count") + 1;
			SessionNode node = new SessionNode(current.get());
			node.set("retry_count", retries);
			if (answer.Exists("error")) {
				node.set("success", false);
				node.set("error", answer.get("error"));

				// Анализируем текст ошибки на предмет перегрузки сервера
				String errorStr = answer.getStr("error");
				String errorMsg = answer.getStr("error/message");
				String fullErrorText = (errorStr + " " + (errorMsg != null ? errorMsg : "")).toLowerCase();

				// Проверяем, упёрлись ли мы в квоту или баланс (это фатально, ретраить бессмысленно)
				boolean isQuotaExceeded = fullErrorText.contains("quota exceeded")
								|| fullErrorText.contains("resource exhausted")
								|| fullErrorText.contains("insufficient balance")
								|| fullErrorText.contains("billing details");

				// Временная перегрузка (которую ДЕЙСТВИТЕЛЬНО стоит ретраить)
				boolean isHighDemand = !isQuotaExceeded && (fullErrorText.contains("high demand")
								|| fullErrorText.contains("overloaded")
								|| fullErrorText.contains("503")
								|| fullErrorText.contains("429")
								|| fullErrorText.contains("rate limit"));

				int maxRetries = isHighDemand ? 15 : 7;
				long sleepTimeMs = isHighDemand ? 30000L : 15000L;

				// Выбираем адекватный заголовок ошибки для UI
				String statusMessage;
				if (isQuotaExceeded) {
					statusMessage = "Превышена квота / лимит тарифного плана";
				} else if (isHighDemand) {
					statusMessage = "Модель перегружена (High Demand)";
				} else {
					statusMessage = "Ошибка соединения";
				}

				// Если это квота — сразу обрубаем ретрии (maxRetries = 0)
				if (isQuotaExceeded) {
					node.set("waitAnswer", false);
					node.set("system", statusMessage + ". Проверьте баланс и детали тарифного плана.");
					session.UpdateNode(node);
				} else if (retries <= maxRetries) {
					node.set("waitAnswer", true);
					node.set("system", statusMessage + ". Попытка " + retries + " из " + maxRetries + ". Ждем " + (sleepTimeMs / 1000) + " сек...");
					session.UpdateNode(node);
					if (!session.isInterrupted()) {
						Thread worker = new Thread(() -> {
							try {
								Thread.sleep(sleepTimeMs);
							} catch (Exception ex) {
							}
							RequestAI(session);
						});
						session.setThread(worker);
						worker.start();
					}

				} else {
					node.set("waitAnswer", false);
					node.set("system", statusMessage + " после " + maxRetries + " попыток.");
					session.UpdateNode(node);
				}
			}

		}
	}

	protected static boolean AddFilePart(i_Session session, List<String> request, String uriStr) throws URISyntaxException {
		return AddFilePart(session, request, uriStr, null);
	}

	protected static boolean AddFilePart(i_Session session, List<String> request, String uriStr, String partTitle) throws URISyntaxException {
		if (uriStr == null || uriStr.isEmpty()) {
			return false;
		}

		if (uriStr.endsWith("/:changes")) {
			String cleanUri = uriStr.replace("/:changes", "");
			FileHandler hFile = FileHandler.fromURI(cleanUri, session.getProjectHandler());
			if (hFile.getErrorMessage() == null) {
				request.add(RequestPart.Build(request, uriStr, "История изменений", hFile.getDiffWithLatestBackup()).toString());
				return true;
			}
			return false;
		}

		FileHandler hFile = FileHandler.fromURI(uriStr, session.getProjectHandler());
		if (!hFile.isValid()) {
			return false;
		}

		String content = hFile.getContent();
		if (content != null && !content.isEmpty()) {
			RequestPart part;
			if (uriStr.endsWith(".json")) {
				part = new RequestPartJSON();
			} else if (uriStr.endsWith(".md")) {
				part = new RequestPartMarkdown();
			} else {
				part = new RequestPart();
			}

			part.title = (partTitle != null && !partTitle.isEmpty()) ? partTitle : "Файл: " + hFile.sourceFile.getName();
			part.location = uriStr;
			part.content = content;
			request.add(part.toString());
			return true;
		}

		return false;
	}

	protected static boolean AddInstructionPart(i_Session session, List<String> request, String uriStr) {
		if (uriStr == null || uriStr.isEmpty()) {
			return false;
		}

		// Проверяем схему и алиас
		boolean isPrompts = uriStr.startsWith("system://~prompts/") || uriStr.startsWith("system://@prompts/");
		if (!isPrompts) {
			return false;
		}

		// Вытаскиваем путь после алиаса (например, /ai_trainer_r_remainder_v1.md)
		int prefixLen = uriStr.startsWith("system://~prompts") ? "system://~prompts".length() : "system://@prompts".length();
		String path = uriStr.substring(prefixLen);

		if (path.isEmpty()) {
			return false;
		}

		// Прямая склейка ресурса из JAR
		String resourcePath = "/su/crazy74/sparky" + path;

		try (java.io.InputStream is = BrainHandler.class.getResourceAsStream(resourcePath)) {
			if (is == null) {
				AppHandler.toConsole("BrainHandler: Инструкция не найдена: " + resourcePath);
				return false;
			}

			String content = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

			RequestPartMarkdown part = new RequestPartMarkdown();
			part.title = "Системная инструкция " + (path.startsWith("/") ? path.substring(1) : path);
			part.location = uriStr;
			part.content = content;

			request.add(part.toString());
			return true;
		} catch (Exception ex) {
			Exceptions.printStackTrace(ex);
		}

		return false;
	}

	protected static void CheckTrainers(List<String> request, i_Session session) {
		ProjectHandler ph = ProjectHandler.BySession(session);
		if (ph == null || ph.getFolder() == null) {
			return;
		}
		//FileObject fo = ph.getFolder().getFileObject(".german/trainers.json");
		File file = FileUtil.toFile(ph.getTrainersFile());
		try {
			String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");
		} catch (IOException e) {
			return;
		}
		List<Map> list;
		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String json = new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");
			list = gson.fromJson(json, new TypeToken<List<Map>>() {
			}.getType());
		} catch (Exception e) {
			AppHandler.toConsole(e.toString());
			return;
		}
		//AppHandler.toConsole("list: " + list);
		try {
			for (Map<String, Object> item : list) {
				Storage node = new Storage(item);
				//AppHandler.toConsole(node.toFmtJson());
				if (node.Exists("file") && node.getBool("required")) {
					try {
						if (node.Exists("title")) {
							AddFilePart(session, request, node.getStr("file"), node.getStr("title"));
						} else {
							AddFilePart(session, request, node.getStr("file"));
						}
					} catch (Exception e) {
					}
				}
			}
		} catch (Exception e) {
		}
	}

	protected static void CheckNeeds(List<String> request, i_Session session) {
		SessionNode current = session.getActive();
		List<Map> stages = (List<Map>) current.get("stages");
		if (stages == null) {
			return;
		}

		boolean activeStageProcessed = false; // Наш флаг последовательного конечного автомата

		for (Map<String, Object> stage : stages) {
			List<String> needs = (List<String>) stage.get("needs");
			String state = (String) stage.get("state");

			// 1. ЗАЩИТА: Работаем только с этапом в режиме "process"
			if (!"process".equals(state)) {
				continue;
			}

			// 2. ЗАЩИТА: Если один активный этап уже обработан в этом цикле, остальные сбрасываем в wait
			if (activeStageProcessed) {
				stage.put("state", "wait");
				continue;
			}

			if (needs == null || needs.isEmpty()) {
				activeStageProcessed = true;
				continue;
			}

			// 3. ЗАЩИТА: Страховка от "жадности" модели (не более 5 файлов на один шаг)
			if (needs.size() > 5) {
				Map<String, String> stagePayload = new HashMap<>();
				stagePayload.put("error", "Превышен лимит: запрошено " + needs.size() + " файлов. Максимум 5 за один шаг.");
				stage.put("payload", stagePayload);
				activeStageProcessed = true;
				continue;
			}

			// --- ТВОЯ РОДНАЯ ЛОГИКА СБОРА ДАННЫХ ---
			Map<String, String> stagePayload = new HashMap<>();

			for (String uriStr : needs) {
				try {
					java.net.URI uri = new java.net.URI(uriStr);
					String scheme = uri.getScheme();
					String authority = uri.getAuthority();
					String path = uri.getPath();

					if ("file".equals(scheme)) {
						// Дерево проекта — отдельный случай
						if ("~project".equals(authority) && "/:tree".equals(path)) {
							ProjectHandler ph = session.getProjectHandler();
							if (ph != null) {
								request.add(RequestPart.Build(request, uriStr, "Дерево проекта", ph.FileTree()).toString());
								stagePayload.put(uriStr, "delivered");
							} else {
								stagePayload.put(uriStr, "error: Session has no project context");
							}
							continue;
						}

						// Любые другие файлы (~project, ~plugin, ~trainers, ~user, абсолютные пути)
						FileHandler hFile = FileHandler.fromURI(uriStr, session.getProjectHandler());
						if (hFile.isValid()) {
							if (AddFilePart(session, request, uriStr)) {
								stagePayload.put(uriStr, "delivered");
							} else {
								stagePayload.put(uriStr, "not delivered");
							}
						} else {
							stagePayload.put(uriStr, "error: " + hFile.getErrorMessage());
						}
						continue;

					} else if ("system".equals(scheme)) {
						if ("~prompts".equals(authority) || "@prompts".equals(authority)) {
							if (AddInstructionPart(session, request, uriStr)) {
								stagePayload.put(uriStr, "delivered");
							} else {
								stagePayload.put(uriStr, "not delivered");
							}
							continue;
						}
					} else if ("ide".equals(scheme)) {
						if (("~netbeans".equals(authority) || "@netbeans".equals(authority)) && "/opened_tabs".equals(path)) {
							request.add(RequestPartJSON.Build(request, uriStr, "Открытые вкладки IDE", ideHandler.getOpenedTabs(session.getProject())).toString());
							stagePayload.put(uriStr, "delivered");
							continue;
						} else if (("~netbeans".equals(authority) || "@netbeans".equals(authority)) && "/opened_projects".equals(path)) {
							request.add(RequestPartJSON.Build(request, uriStr, "Открытые проекты в IDE", ideHandler.getOpenedProjectsJson()).toString());
							stagePayload.put(uriStr, "delivered");
							continue;
						} else if (("~project".equals(authority) || "@project".equals(authority)) && "/info".equals(path)) {
							request.add(RequestPartJSON.Build(request, uriStr, "Проект", session.getProjectInformation().toFmtJson()).toString());
							stagePayload.put(uriStr, "delivered");
							continue;
						}
					}

					stagePayload.put(uriStr, "error: Unknown resource URI");

				} catch (Exception e) {
					stagePayload.put(uriStr, "error: " + e.getMessage());
				}
			}

			stage.put("payload", stagePayload);
			activeStageProcessed = true; // Фиксируем, что текущий активный шаг отработал
		}
	}

	private static String buildTree(i_Session session) {
		ProjectHandler ph = ProjectHandler.BySession(session);
		if (ph == null || ph.getFolder() == null) {
			return null;
		}
		String projectPath = ph.getInformation().getStr("folder");
		return buildTree(new File(projectPath), "", true);
	}

	private static String buildTree(File folder, String indent, boolean isLast) {
		StringBuilder sb = new StringBuilder();
		AppHandler.toConsole("buildTree folder: " + folder.toPath());
		sb.append(indent).append(isLast ? "└── " : "├── ").append(folder.getName()).append("\n");

		// Фильтруем мусор сразу
		File[] files = folder.listFiles(f -> !f.getName().startsWith(".")
						&& !f.getName().equals("vendor")
						&& !f.getName().equals("node_modules"));

		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				boolean lastChild = (i == files.length - 1);
				sb.append(buildTree(files[i], indent + (isLast ? "    " : "│   "), lastChild));
			}
		}
		return sb.toString();
	}

	static class RequestPart {

		public String contentType = "text/plain";
		public String charset = "UTF-8";
		// Инициализируем сразу, чтобы не ловить NPE при вызове isEmpty()
		public String title = "";
		public String location = "";
		public String content = "";

		public static RequestPart Build(List<String> request, String location, String title, String content) {
			RequestPart part = new RequestPart();
			part.title = title;
			part.location = location;
			part.content = content;
			return part;
		}

		public String toString() {
			String reuslt = "Content-Type: " + this.contentType + ";";
			reuslt = reuslt + " charset: " + this.charset + ";\r\n";

			// Теперь проверки на isEmpty() безопасны
			if (!this.title.isEmpty()) {
				reuslt = reuslt + "Content-Title: " + this.title + ";\r\n";
			}
			if (!this.location.isEmpty()) {
				reuslt = reuslt + "Content-Location: " + this.location + ";\r\n";
			}

			reuslt = reuslt + "\r\n" + this.content;
			return reuslt;
		}
	}

	static class RequestPartJSON extends RequestPart {

		public RequestPartJSON(SessionNode node) {
			this.contentType = "application/json";
			this.content = node.toFmtJson();
		}

		public RequestPartJSON(String jsonString) {
			this.contentType = "application/json";
			this.content = jsonString;
		}

		public RequestPartJSON() {
			this.contentType = "application/json";
		}

		public static RequestPartJSON Build(List<String> request, String location, String title, String content) {
			RequestPartJSON part = new RequestPartJSON();
			part.title = title;
			part.location = location;
			part.content = content;
			return part;
		}
	}

	static class RequestPartMarkdown extends RequestPart {

		public RequestPartMarkdown() {
			this.contentType = "application/markdown";
		}
	}
}
