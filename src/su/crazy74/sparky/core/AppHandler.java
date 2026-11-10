package su.crazy74.sparky.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.Places;
import org.openide.util.Exceptions;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

public class AppHandler {

	private static AppHandler Instance;
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private Map<String, Object> configMap;

	public AppHandler() {
		javafx.application.Platform.setImplicitExit(false);
	}
	public static AppHandler getDefault() {
		if (Instance == null) {
			Instance = new AppHandler();
		}
		return Instance;
	}

	// Загрузка данных из JSON в Map (Dictionary)
	public Map<String, Object> getConfig() {
		if (configMap == null) {
			loadConfig();
		}
		return configMap;
	}

	public void loadConfig() {
		FileObject fo = getConfigFile();
		File file = FileUtil.toFile(fo);

		try (Reader reader = new FileReader(file)) {
			Type type = new TypeToken<Map<String, Object>>() {
			}.getType();
			configMap = gson.fromJson(reader, type);
			if (configMap == null) {
				configMap = new HashMap<>();
			}
		} catch (IOException e) {
			configMap = new HashMap<>();
		}
	}

	public void saveConfig() {
		if (configMap == null) {
			return;
		}
		FileObject fo = getConfigFile();
		File file = FileUtil.toFile(fo);

		try (Writer writer = new FileWriter(file)) {
			gson.toJson(configMap, writer);
		} catch (IOException e) {
			Exceptions.printStackTrace(e);
		}
	}

	public FileObject getConfigFile() {
		return AppConfig.getConfigFile();
	}

	public static Storage getEnv() {
		Storage env = new Storage();
		env.set("lang_code", LanguageHandler.getDefault().getCurrentLang());
		env.set("lang_data", LanguageHandler.getDefault().getStorage().getRaw());
		String pluginVersion = "unknown";
    for (org.openide.modules.ModuleInfo info : org.openide.util.Lookup.getDefault().lookupAll(org.openide.modules.ModuleInfo.class)) {
        if ("org.crazygerman".equals(info.getCodeNameBase())) {
            pluginVersion = info.getSpecificationVersion() != null ? info.getSpecificationVersion().toString() : "1.0";
            break;
        }
    }
    env.set("lang_data/plugin/version", pluginVersion);
		AppHandler.toConsole("pluginVersion: " + pluginVersion);
		//AppHandler.toConsole("env: " + env.toFmtJson());
		return env;
	}
	
	public static FileObject getProfileFile() {
		//File configDir = new File(Places.getUserDirectory(), "config/sparky");
		File configDir = AppConfig.getConfigDir();
		if (!configDir.exists()) {
			configDir.mkdirs();
		}
		File configFile = new File(configDir, "user_profile.json");
		try {
			if (!configFile.exists()) {
				configFile.createNewFile();
				// Запишем пустой объект {}, чтобы файл был валидным JSON
				try (FileWriter fw = new FileWriter(configFile)) {
					fw.write("{}");
				}
			}
			return FileUtil.toFileObject(configFile);
		} catch (IOException ex) {
			Exceptions.printStackTrace(ex);
		}
		return null;
	}

	public static void toConsole(String x) {
		InputOutput io = IOProvider.getDefault().getIO("Sparky Console", false);
		io.getOut().println(x);
		io.getOut().close();
	}
}
