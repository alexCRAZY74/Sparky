package su.crazy74.sparky.core;

import su.crazy74.sparky.interfaces.i_Session;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.Places;
import org.openide.util.Exceptions;

public class AppConfig {

	private static Storage SystemData;
	private static Storage UserData;

	public static Storage getAiModelsList(i_Session session) {
		CheckInit();
		/*
    не забыть!
    у сессии может быть проект, а у проекта может быть прописана компания
    А у компании могут быть свои модели
		 */
		Storage result = new Storage();
		String companyKey = "defaultCompany";
		String basePath = companyKey + "/ai-models";

		for (String modelKey : UserData.childrenKeys(basePath)) {
			if (!UserData.getBool(basePath + "/" + modelKey + "/skip")) {
				String title = UserData.getStr(basePath + "/" + modelKey + "/title");
				if (UserData.getBool(basePath + "/" + modelKey + "/default")) {
					result.set("default", modelKey);
				}
				if (title == null || title.isEmpty()) {
					title = modelKey;
				}
				result.set("list/" + modelKey, title);
				result.set("Models/" + modelKey, UserData.get(basePath + "/" + modelKey));
			}
		}

		return result;
	}

	public static boolean Exists(String path) {
		return UserData.Exists(path) || SystemData.Exists(path);
	}

	public static List<String> childrenKeys(String path) {
		return UserData.childrenKeys(path);
	}

	public static List<String> childrenKeys() {
		return UserData.childrenKeys();
	}

	public static Object get(String path) {
		CheckInit();
		if (UserData.Exists(path)) {
			return UserData.get(path);
		} else {
			return SystemData.get(path);
		}
	}

	protected static void CheckInit() {
		if (UserData == null) {
			LoadFromFile();
		}
		if (SystemData == null) {
			UpdateFromSystem();
		}
	}

	public static void UpdateFromSystem() {
		if (SystemData == null) {
			SystemData = new Storage();
		}

		// --- UI Colors ---
		SystemData.set("NetBeans/UI/Color/Panel/background", colorToHex(javax.swing.UIManager.getColor("Panel.background"), "#FFFFFF"));
		SystemData.set("NetBeans/UI/Color/Label/foreground", colorToHex(javax.swing.UIManager.getColor("Label.foreground"), "#000000"));
		SystemData.set("NetBeans/UI/Color/Explorer/background", colorToHex(javax.swing.UIManager.getColor("Nb.Explorer.background"), "#FFFFFF"));
		SystemData.set("NetBeans/UI/Color/Component/border", colorToHex(javax.swing.UIManager.getColor("Component.borderColor"), "#CCCCCC"));
		SystemData.set("NetBeans/UI/Color/List/selectionBackground", colorToHex(javax.swing.UIManager.getColor("List.selectionBackground"), "#0066CC"));

		// --- Editor Colors ---
		SystemData.set("NetBeans/Editor/Color/background", colorToHex(javax.swing.UIManager.getColor("EditorPane.background"), "#FFFFFF"));
		SystemData.set("NetBeans/Editor/Color/foreground", colorToHex(javax.swing.UIManager.getColor("EditorPane.foreground"), "#000000"));
		SystemData.set("NetBeans/Editor/Color/selectionBackground", colorToHex(javax.swing.UIManager.getColor("EditorPane.selectionBackground"), "#0066CC"));

		// --- Fonts ---
		java.awt.Font uiFont = javax.swing.UIManager.getFont("Label.font");
		SystemData.set("NetBeans/UI/Font/family", (uiFont != null) ? uiFont.getFamily() : "\"Segoe UI\", sans-serif");
		SystemData.set("NetBeans/UI/Font/size", ((uiFont != null) ? uiFont.getSize() : 13) + 1);

		SystemData.set("NetBeans/Editor/Font/family", "\"Consolas\", \"Monaco\", Monospaced, monospace");
		SystemData.set("NetBeans/Editor/Font/size", ((uiFont != null) ? uiFont.getSize() : 13) + 1);

		// --- Syntax Highlighting (Синтаксис NetBeans) ---
		// В будущем здесь будет вызов TokenHierarchy или Editor Settings API,
		// а пока задаем дефолты в правильной ветке.
		SystemData.set("NetBeans/Editor/Syntax/Color/keyword", "#000080");
		SystemData.set("NetBeans/Editor/Syntax/Color/string", "#008000");
		SystemData.set("NetBeans/Editor/Syntax/Color/comment", "#808080");
		SystemData.set("NetBeans/Editor/Syntax/Color/number", "#FF0000");
		SystemData.set("NetBeans/Editor/Syntax/Color/type", "#20B2AA");

		SystemData.set("Session/AlwaysSend/OpenTabs", true);
		SystemData.set("Session/AlwaysSend/ProjectMapGuide", true);
		SystemData.set("Session/AlwaysSend/ProjectInfo", true);
		SystemData.set("Session/AlwaysSend/ProjectFileTree", false);
	}

	private static String colorToHex(java.awt.Color color, String def) {
		if (color == null) {
			if (def == null) {
				return "#000000";
			}
			return def;
		}
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	private static String colorToHex(java.awt.Color color) {
		return colorToHex(color, null);
	}

	public static String getStr(String path) {
		Object val = get(path);
		return (val != null) ? String.valueOf(val) : null;
	}

	public static boolean getBool(String path) {
		Object val = get(path);
		if (val instanceof Boolean) {
			return (Boolean) val;
		}
		if (val instanceof String) {
			return Boolean.parseBoolean((String) val);
		}
		return false;
	}

	public static int getInt(String path) {
		Object val = get(path);
		if (val instanceof Number) {
			return ((Number) val).intValue();
		}
		if (val instanceof String) {
			try {
				return Integer.parseInt((String) val);
			} catch (Exception e) {
			}
		}
		return 0;
	}
	
	public static File getConfigDir() {
		File configDir = new File(Places.getUserDirectory(), "config/crazy_sparky");
		if (!configDir.exists()) {
			configDir.mkdirs();
		}
		return configDir;
	}

	public static FileObject getConfigFile() {
		File configFile = new File(getConfigDir(), "settings.json");
		try {
			if (!configFile.exists()) {
				configFile.createNewFile();
				// Запишем пустой объект {}, чтобы файл был валидным JSON
				try (FileWriter fw = new FileWriter(configFile)) {
					fw.write("{}");
				}
			}
			return FileUtil.toFileObject(configFile);
		} catch (Exception ex) {
			Exceptions.printStackTrace(ex);
		}
		return null;
	}

	public static void LoadFromFile() {
		FileObject fo = getConfigFile();
		File file = FileUtil.toFile(fo);
		try {
			String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");
			UserData = Storage.fromJson(content);
		} catch (IOException e) {
			UserData = new Storage();
		}
	}

	public static void SaveToFile() {
		FileObject fo = getConfigFile();
		File file = FileUtil.toFile(fo);
		try {
			final String finalContent = UserData.toFmtJson();
			java.nio.file.Files.write(file.toPath(), finalContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		} catch (IOException e) {
			Exceptions.printStackTrace(e);
		}
	}
}
