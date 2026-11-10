package su.crazy74.sparky.core;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.openide.util.Exceptions;

public class LanguageHandler {

  private static LanguageHandler instance;
  private Storage storage;
  private String currentLang = "ru";

  public LanguageHandler() {
    String lang = AppConfig.getStr("language");
    if (lang == null || lang.isBlank()) {
      lang = Locale.getDefault().getLanguage();
    }
    loadLanguage(lang);
  }

  public static LanguageHandler getDefault() {
    if (instance == null) {
      instance = new LanguageHandler();
    }
    return instance;
  }

  public void loadLanguage(String lang) {
    if (lang == null || lang.isBlank()) {
      lang = "ru";
    }
    this.currentLang = lang;
    String resourcePath = "/su/crazy74/sparky/language_" + lang + ".json";
    InputStream is = getClass().getResourceAsStream(resourcePath);

		//E:\Work\CrazyGerman\src\crazy\module\german\language_ru.json
		//E:\Work\Sparky\src\su\crazy74\sparky\language_ru.json
    if (is == null && !"ru".equals(lang)) {
      resourcePath = "/su/crazy74/sparky/language_ru.json";
      is = getClass().getResourceAsStream(resourcePath);
      this.currentLang = "ru";
    }

    try {
      if (is != null) {
        try (InputStream stream = is) {
          String jsonText = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
          this.storage = Storage.fromJson(jsonText);
        }
      } else {
        this.storage = new Storage();
        AppHandler.toConsole("Language file not found: " + resourcePath);
      }
    } catch (Exception e) {
      this.storage = new Storage();
      Exceptions.printStackTrace(e);
    }
  }

  public String getCurrentLang() {
    return currentLang;
  }

  public String getStr(String path) {
    return getStr(path, path);
  }

  public String getStr(String path, String defaultValue) {
    if (storage == null) {
      return defaultValue;
    }
    String val = storage.getStr(path);
    return (val != null) ? val : defaultValue;
  }

  public String getJson() {
    return storage != null ? storage.toJson() : "{}";
  }

  public Storage getStorage() {
    return storage;
  }
}
