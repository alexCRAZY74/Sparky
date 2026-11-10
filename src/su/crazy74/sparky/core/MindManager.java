package su.crazy74.sparky.core;

import su.crazy74.sparky.interfaces.i_GermanProject;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openide.filesystems.FileUtil;

public class MindManager {

	protected i_GermanProject hProject;
	protected DataLayer dlMapData = null;
	protected DataLayer dlMapConfig = null;
	protected DataLayer dlMapState = null;
	protected String rootGuid = null;

	public static MindManager Project(i_GermanProject project) {
		MindManager mm = new MindManager();
		mm.hProject = project;
		return mm;
	}

	public Storage commandGet(Storage form) {
		switch (form.getStr("command-get")) {
			case "map-tree":
				return this.getNodeArray();
		}
		return new Storage();
	}

	public Storage commandSet(Storage form) {
		switch (form.getStr("command-set")) {
			case "map-tree":
				return this.saveNodeArray(form);
		}
		AppHandler.toConsole("commandSet: " + form.toFmtJson());
		Storage result = new Storage();
		result.set("success", true);
		return result;
	}

	public Storage saveNodeArray(Storage form) {
		Storage result = new Storage();
		if (!form.Exists("nodes") || form.typeOf("nodes") != "array") {
			result.set("success", false);
			return result;
		}
		this.dlMapData = null;
		this.dlMapConfig = null;
		this.dlMapState = null;
		this.rootGuid = null;
		for (Object nodeRawObject : form.getList("nodes")) {
			Storage nodeTmp = new Storage(nodeRawObject);
			if (nodeTmp.getBool("isroot")) {
				this.rootGuid = nodeTmp.getStr("id");
			}
		}

		//AppHandler.toConsole("saveNodeArray(): ");
		List<String> stateKeys = java.util.Arrays.asList("expanded", "direction");
		for (Object nodeRawObject : form.getList("nodes")) {
			Storage node = new Storage(nodeRawObject);
			//AppHandler.toConsole("node: " + node.toFmtJson());
			if (node.getBool("isroot")) {
				node.Remove("data/type");
				this.config().set(node.get("data"));
				node.Remove("data");
				node.set("data/type", "root");
			}
			String nodeID = node.getStr("id");
			Storage cleanNode = new Storage();
			for (String key : node.childrenKeys()) {
				if (stateKeys.contains(key)) {
					this.states().set(nodeID + "/" + key, node.get(key));
					continue;
				}
				cleanNode.set(key, node.get(key));
			}
			this.nodes().set("[]", cleanNode.getRaw());
		}
		//AppHandler.toConsole("config: " + this.config().toFmtJson());
		//AppHandler.toConsole("states: " + this.states().toFmtJson());
		//AppHandler.toConsole("nodes json: " + this.nodes().toFmtJson());
		//AppHandler.toConsole("nodes markdown: \r\n" + this.nodes().toMarkdown());
		try {
			this.nodes().Save();
			this.states().Save();
			this.config().Save();
			/*File pluginDir = FileUtil.toFile(this.hProject.getPluginDir());
			File configFile = new File(pluginDir, "project_map_.json");
			DataLayer dlMapData = new DataLayer(configFile);
			dlMapData.set(this.nodes().getRaw());
			dlMapData.Save();*/
			result.set("success", true);
		} catch (Exception ex) {
			result.set("success", false);
		}
		return result;
	}

	public Storage getNodeArray() {
		//AppHandler.toConsole("getNodeArray()");
		Storage result = new Storage(new java.util.ArrayList<>());
		this.dlMapData = null;
		this.dlMapConfig = null;
		this.dlMapState = null;
		this.rootGuid = null;

		if (this.nodes().Load().isEmpty()) {
			Storage root = new Storage();
			root.set("isroot", true);
			root.set("id", UUID.randomUUID().toString());
			root.set("topic", this.hProject.getDisplayName());
			root.set("data/type", "root");
			if (this.config().Exists("kanban_settings")) {
				root.set("data/kanban_settings", this.config().get("kanban_settings"));
			}

			result.set("[]", root.getRaw());
		} else {
			//AppHandler.toConsole("this.nodes(): " + this.nodes().toFmtJson());
			for (Object nodeObject : this.nodes().getList(null)) {
				Storage node = new Storage(nodeObject);
				if (node.getBool("isroot")) {
					this.rootGuid = node.getStr("id");
				}
			}
			for (Object nodeObject : this.nodes().getList(null)) {
				Storage node = new Storage(nodeObject);
				if (node.getBool("isroot")) {
					node.Merge("data", this.config());
				}
				String nodeID = node.getStr("id");
				if (this.states().Exists(nodeID)) {
					node.Merge((Map<String, Object>) this.states().get(nodeID));
				}
				result.set("[]", node.getRaw());
			}
		}

		//AppHandler.toConsole("getNodeArray(): " + result.toFmtJson());
		//AppHandler.toConsole("const debugProjectMapData = " + result.toFmtJson() + ";");
		return result;

		/*Storage plug = new Storage();
			plug.set("isroot", true);
			plug.set("id", UUID.randomUUID().toString());
			plug.set("topic", this.hProject.getDisplayName());
			plug.set("data/type", "root");
			if (this.config().Exists("kanban_settings")) {
				plug.set("data/kanban_settings", this.config().get("kanban_settings"));
			}
		Storage plugset = new Storage();
		plugset.set("[]", plug);
		return plugset;*/
	}

	public DataLayer states() {
		if (this.dlMapState == null) {
			if (this.rootGuid == null || this.rootGuid.isEmpty()) {
				this.rootGuid = "map_default_root";
			}

			File configFile = new File(AppConfig.getConfigDir(), "map_" + this.rootGuid + ".json");
			this.dlMapState = new DataLayer(configFile);
			this.dlMapState.Load();
		}
		return this.dlMapState;
	}

	public DataLayer config() {
		if (this.dlMapConfig == null) {
			File pluginDir = FileUtil.toFile(this.hProject.getPluginDir());
			File configFile = new File(pluginDir, "map_config.json");
			
			if (!configFile.exists()) {
				try {
					configFile.createNewFile();
					String content = "{\n" +
"  \"kanban_settings\": {\n" +
"		\"lists\": [\n" +
"			{\n" +
"				\"code\": \"workflow\",\n" +
"				\"title\": \"Статус задачи\",\n" +
"				\"options\": [\n" +
"					{ \"value\": \"todo\", \"text\": \"Сделать\", \"icon\": \"fa fa-circle-o\", \"color\": \"#6c757d\" },\n" +
"					{ \"value\": \"in_progress\", \"text\": \"В работе\", \"icon\": \"fa fa-play-circle\", \"color\": \"#007bff\" },\n" +
"					{ \"value\": \"return_test\", \"text\": \"Возврат с теста\", \"icon\": \"fa fa-arrow-circle-left\", \"color\": \"#dc3545\" },\n" +
"					{ \"value\": \"on_hold\", \"text\": \"Отложена\", \"icon\": \"fa fa-pause-circle\", \"color\": \"#6c757d\" },\n" +
"					{ \"value\": \"canceled\", \"text\": \"Отменена\", \"icon\": \"fa fa-times-circle\", \"color\": \"#6c757d\" },\n" +
"					{ \"value\": \"done\", \"text\": \"Готово\", \"icon\": \"fa fa-check-circle\", \"color\": \"#28a745\" }\n" +
"				]\n" +
"			},\n" +
"			{\n" +
"				\"code\": \"priority\",\n" +
"				\"title\": \"Приоритет\",\n" +
"				\"options\": [\n" +
"					{ \"value\": \"urgent\", \"text\": \"Срочно\", \"icon\": \"fa fa-bolt\", \"color\": \"#dc3545\" },\n" +
"					{ \"value\": \"critical\", \"text\": \"Критично\", \"icon\": \"fa fa-exclamation-circle\", \"color\": \"#bd2130\" },\n" +
"					{ \"value\": \"high\", \"text\": \"Важно\", \"icon\": \"fa fa-bookmark\", \"color\": \"#e67e22\" },\n" +
"					{ \"value\": \"1\", \"text\": \"1-я очередь\", \"icon\": \"num-icon paren num-1\", \"color\": \"#e74c3c\" },\n" +
"					{ \"value\": \"2\", \"text\": \"2-я очередь\", \"icon\": \"num-icon paren num-2\", \"color\": \"#e67e22\" },\n" +
"					{ \"value\": \"3\", \"text\": \"3-я очередь\", \"icon\": \"num-icon paren num-3\", \"color\": \"#f1c40f\" },\n" +
"					{ \"value\": \"4\", \"text\": \"4-я очередь\", \"icon\": \"num-icon paren num-4\", \"color\": \"#2ecc71\" },\n" +
"					{ \"value\": \"5\", \"text\": \"5-я очередь\", \"icon\": \"num-icon paren num-5\", \"color\": \"#95a5a6\" }\n" +
"				]\n" +
"			},\n" +
"			{\n" +
"				\"code\": \"score\",\n" +
"				\"title\": \"Оценка\",\n" +
"				\"options\": [\n" +
"					{ \"value\": \"0\", \"text\": \"0\", \"icon\": \"fa fa-star-o\", \"color\": \"#6c757d\" },\n" +
"					{ \"value\": \"1\", \"text\": \"1\", \"icon\": \"fa fa-star\", \"color\": \"#17a2b8\" },\n" +
"					{ \"value\": \"2\", \"text\": \"2\", \"icon\": \"fa fa-star\", \"color\": \"#17a2b8\" },\n" +
"					{ \"value\": \"3\", \"text\": \"3\", \"icon\": \"fa fa-star\", \"color\": \"#20c997\" },\n" +
"					{ \"value\": \"4\", \"text\": \"4\", \"icon\": \"fa fa-star\", \"color\": \"#28a745\" },\n" +
"					{ \"value\": \"5\", \"text\": \"5\", \"icon\": \"fa fa-star\", \"color\": \"#dc3545\" },\n" +
"					{ \"value\": \"5_plus\", \"text\": \"5+\", \"icon\": \"fa fa-star\", \"color\": \"#ffc107\" }\n" +
"				]\n" +
"			}\n" +
"		]\n" +
"	}\n" +
"}";
					java.nio.file.Files.write(configFile.toPath(), content.getBytes("UTF-8"));
				} catch (Exception e) {
				}
			}
			
			this.dlMapConfig = new DataLayer(configFile);
			this.dlMapConfig.Load();
			if (this.dlMapConfig.isEmpty()) {
				this.dlMapConfig.set(Storage.fromJson("{\"kanban_settings\": {\n" +
"				\"lists\": [\n" +
"					{\n" +
"						\"code\": \"workflow\",\n" +
"						\"title\": \"Статус задачи\",\n" +
"						\"options\": [\n" +
"							{ \"value\": \"todo\", \"text\": \"Сделать\", \"icon\": \"fa fa-circle-o\", \"color\": \"#6c757d\" },\n" +
"							{ \"value\": \"in_progress\", \"text\": \"В работе\", \"icon\": \"fa fa-play-circle\", \"color\": \"#007bff\" },\n" +
"							{ \"value\": \"return_test\", \"text\": \"Возврат с теста\", \"icon\": \"fa fa-arrow-circle-left\", \"color\": \"#dc3545\" },\n" +
"							{ \"value\": \"on_hold\", \"text\": \"Отложена\", \"icon\": \"fa fa-pause-circle\", \"color\": \"#6c757d\" },\n" +
"							{ \"value\": \"canceled\", \"text\": \"Отменена\", \"icon\": \"fa fa-times-circle\", \"color\": \"#6c757d\" },\n" +
"							{ \"value\": \"done\", \"text\": \"Готово\", \"icon\": \"fa fa-check-circle\", \"color\": \"#28a745\" }\n" +
"						]\n" +
"					},\n" +
"					{\n" +
"						\"code\": \"priority\",\n" +
"						\"title\": \"Приоритет\",\n" +
"						\"options\": [\n" +
"							{ \"value\": \"high\", \"text\": \"Важно\", \"icon\": \"fa fa-arrow-up\", \"color\": \"#ffc107\" },\n" +
"							{ \"value\": \"critical\", \"text\": \"Критично\", \"icon\": \"mif-fire\", \"color\": \"#dc3545\" }\n" +
"						]\n" +
"					},\n" +
"					{\n" +
"						\"code\": \"score\",\n" +
"						\"title\": \"Оценка\",\n" +
"						\"options\": [\n" +
"							{ \"value\": \"0\", \"text\": \"0\", \"icon\": \"fa fa-star-o\", \"color\": \"#6c757d\" },\n" +
"							{ \"value\": \"1\", \"text\": \"1\", \"icon\": \"fa fa-star\", \"color\": \"#17a2b8\" },\n" +
"							{ \"value\": \"2\", \"text\": \"2\", \"icon\": \"fa fa-star\", \"color\": \"#17a2b8\" },\n" +
"							{ \"value\": \"3\", \"text\": \"3\", \"icon\": \"fa fa-star\", \"color\": \"#20c997\" },\n" +
"							{ \"value\": \"4\", \"text\": \"4\", \"icon\": \"fa fa-star\", \"color\": \"#28a745\" },\n" +
"							{ \"value\": \"5\", \"text\": \"5\", \"icon\": \"fa fa-star\", \"color\": \"#dc3545\" },\n" +
"							{ \"value\": \"5_plus\", \"text\": \"5+\", \"icon\": \"fa fa-star\", \"color\": \"#ffc107\" }\n" +
"						]\n" +
"					}\n" +
"				]\n" +
"			}}").getRaw());
			}
		}
		return this.dlMapConfig;
	}

	public DataLayer nodes() {
		if (this.dlMapData == null) {
			File pluginDir = FileUtil.toFile(this.hProject.getPluginDir());
			File configFile = new File(pluginDir, "project_map.md");
			this.dlMapData = new DataLayer(configFile);
			//this.dlMapData.Load();
		}
		return this.dlMapData;
	}

	protected class DataLayer extends Storage {

		protected File assignedFile = null;

		public DataLayer(File file) {
			this.assignedFile = file;
		}

		public DataLayer Load() {
			if (!this.assignedFile.exists()) {
				return this;
			}
			try {
				String content = new String(java.nio.file.Files.readAllBytes(this.assignedFile.toPath()), "UTF-8");
				String ext = FileUtil.toFileObject(this.assignedFile).getExt().toLowerCase();
				if ("md".equals(ext)) {
					this.set(DataLayer.fromMarkdown(content).getRaw());
				} else {
					this.set(DataLayer.fromJson(content).getRaw());
				}
			} catch (Exception e) {
			}
			return this;
		}

		public void Save() {
			if (this.assignedFile == null) {
				return;
			}
			try {
				if (!this.assignedFile.exists()) {
					this.assignedFile.createNewFile();
				}
				String content;
				String ext = FileUtil.toFileObject(this.assignedFile).getExt().toLowerCase();

				if ("md".equals(ext)) {
					content = this.toMarkdown();
				} else {
					content = this.toFmtJson();
				}

				java.nio.file.Files.write(this.assignedFile.toPath(), content.getBytes("UTF-8"));
			} catch (Exception e) {
				AppHandler.toConsole("Ошибка сохранения файла: " + e.getMessage());
			}
		}

		public String toMarkdown() {
			List<Object> nodeList = this.getList("");
			if (nodeList == null || nodeList.isEmpty()) {
				return "";
			}

			StringBuilder sb = new StringBuilder();

			for (Object item : nodeList) {
				if (!(item instanceof Map)) {
					continue;
				}

				@SuppressWarnings("unchecked")
				Map<String, Object> node = (Map<String, Object>) item;

				// 1. Заголовок узла (# Topic)
				String topic = node.containsKey("topic") ? String.valueOf(node.get("topic")) : "Без названия";
				sb.append("# ").append(topic).append("\n");

				String noteText = null;

				// Достаем note из "data", если он там есть
				if (node.get("data") instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> dataMap = (Map<String, Object>) node.get("data");
					if (dataMap.containsKey("note") && dataMap.get("note") != null) {
						noteText = String.valueOf(dataMap.get("note"));
					}
				} else if (node.containsKey("note") && node.get("note") != null) {
					noteText = String.valueOf(node.get("note"));
				}

				// 2. Печатаем описание (note) сразу под заголовком
				if (noteText != null && !noteText.trim().isEmpty()) {
					sb.append(noteText.trim()).append("\n");
				}

				// 3. Системные свойства в атрибуты
				if (node.containsKey("id")) {
					sb.append("- id: ").append(node.get("id")).append("\n");
				}

				if (node.containsKey("parentid") && node.get("parentid") != null) {
					sb.append("- parentid: ").append(node.get("parentid")).append("\n");
				}

				if (node.containsKey("isroot") && Boolean.TRUE.equals(node.get("isroot"))) {
					sb.append("- isroot: true\n");
				}

				// 4. Пользовательские атрибуты из "data" (пропуская note)
				if (node.get("data") instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> dataMap = (Map<String, Object>) node.get("data");
					for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
						if ("note".equals(entry.getKey())) {
							continue;
						}
						sb.append("- ").append(entry.getKey()).append(": ")
										.append(formatValue(entry.getValue())).append("\n");
					}
				}

				// 5. Прочие свойства вне "data" (пропуская note и служебные)
				for (Map.Entry<String, Object> entry : node.entrySet()) {
					String key = entry.getKey();
					if ("id".equals(key) || "parentid".equals(key) || "isroot".equals(key)
									|| "topic".equals(key) || "data".equals(key) || "note".equals(key)) {
						continue;
					}
					sb.append("- ").append(key).append(": ")
									.append(formatValue(entry.getValue())).append("\n");
				}

				sb.append("\n");
			}

			return sb.toString().trim();
		}

		private String formatValue(Object val) {
			if (val == null) {
				return "null";
			}
			if (val instanceof Map || val instanceof List) {
				return new Storage(val).toJson();
			}
			return String.valueOf(val);
		}

		public static Storage fromMarkdown(String mdText) {
			if (mdText == null || mdText.trim().isEmpty()) {
				return new Storage(new java.util.ArrayList<>());
			}

			List<Map<String, Object>> nodeList = new java.util.ArrayList<>();
			String[] lines = mdText.split("\\r?\\n");

			Map<String, Object> currentNode = null;
			Map<String, Object> currentData = null;
			StringBuilder currentNote = null;

			for (String line : lines) {
				String trimmed = line.trim();

				// 1. Начало нового узла по заголовку #
				if (trimmed.startsWith("# ")) {
					if (currentNode != null) {
						commitNode(currentNode, currentData, currentNote, nodeList);
					}

					currentNode = new java.util.LinkedHashMap<>();
					currentData = new java.util.LinkedHashMap<>();
					currentNote = new StringBuilder();

					String topic = trimmed.substring(2).trim();
					currentNode.put("topic", topic);
					continue;
				}

				if (currentNode == null) {
					continue;
				}

				// 2. Строка атрибута формата "- key: value"
				if (trimmed.startsWith("- ")) {
					String attrLine = trimmed.substring(2).trim();
					int colonIndex = attrLine.indexOf(':');

					if (colonIndex != -1) {
						String key = attrLine.substring(0, colonIndex).trim();
						String rawValue = attrLine.substring(colonIndex + 1).trim();

						Object parsedValue = parseValue(rawValue);

						if ("id".equals(key)) {
							currentNode.put("id", String.valueOf(parsedValue));
						} else if ("parentid".equals(key)) {
							currentNode.put("parentid", String.valueOf(parsedValue));
						} else if ("isroot".equals(key)) {
							if (parsedValue instanceof Boolean && (Boolean) parsedValue) {
								currentNode.put("isroot", true);
							}
						} else {
							currentData.put(key, parsedValue);
						}
					}
				} // 3. Текст до списка атрибутов (или между ними) собираем в note
				else if (!trimmed.isEmpty()) {
					if (currentNote.length() > 0) {
						currentNote.append("\n");
					}
					currentNote.append(line);
				}
			}

			// Фиксируем последний узел
			if (currentNode != null) {
				commitNode(currentNode, currentData, currentNote, nodeList);
			}

			return new Storage(nodeList);
		}

		private static void commitNode(Map<String, Object> node, Map<String, Object> data, StringBuilder note, List<Map<String, Object>> list) {
			if (note != null && note.length() > 0) {
				data.put("note", note.toString().trim());
			}
			if (!data.isEmpty()) {
				node.put("data", data);
			}
			list.add(node);
		}

		private static Object parseValue(String rawValue) {
			if ("true".equalsIgnoreCase(rawValue)) {
				return Boolean.TRUE;
			}
			if ("false".equalsIgnoreCase(rawValue)) {
				return Boolean.FALSE;
			}

			if ((rawValue.startsWith("{") && rawValue.endsWith("}")) || (rawValue.startsWith("[") && rawValue.endsWith("]"))) {
				try {
					Storage jsonParsed = Storage.fromJson(rawValue);
					if (jsonParsed != null && jsonParsed.getRaw() != null) {
						return jsonParsed.getRaw();
					}
				} catch (Exception ignored) {
				}
			}

			return rawValue;
		}

	}
}
