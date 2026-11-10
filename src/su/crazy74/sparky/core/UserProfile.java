package su.crazy74.sparky.core;

import java.io.File;
import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

public class UserProfile {

	public static Storage Data;

	public static String toFmtJson() {
		CheckInit();
		return Data.toFmtJson();
	}

	protected static void CheckInit() {
		if (Data == null) {
			Load();
		}
	}

	public static void Update(Storage newData) {
		CheckInit();
		if (newData == null) {
			return;
		}

		// Достаем карту новых данных
		java.util.Map<String, Object> updates = (java.util.Map<String, Object>) newData.get("");
		String now = new java.text.SimpleDateFormat("dd.MM.yyyy, HH:mm:ss").format(new java.util.Date());

		for (String key : updates.keySet()) {
			// Не логгируем изменения в самом разделе истории
			if (key.equals("changes")) {
				continue;
			}

			Object newValue = updates.get(key);

			// Если ключ уже существует и значение изменилось — пишем в историю
			if (Data.Exists(key)) {
				Object oldValue = Data.get(key);

				// Сравниваем, чтобы не плодить одинаковые записи в истории
				if (oldValue != null && !oldValue.equals(newValue)) {

					// Формируем структуру по твоему ТЗ[cite: 3]
					Storage historyEntry = new Storage();
					historyEntry.set("stored", now);
					historyEntry.set("previous_value", oldValue);

					// Путь в Storage: changes/имя_ключа (автоматически создаст список)
					String historyPath = "changes/" + key;

					java.util.List<Object> historyList = (java.util.List<Object>) Data.get(historyPath);
					if (historyList == null) {
						historyList = new java.util.ArrayList<>();
					}

					historyList.add(historyEntry.get());
					Data.set(historyPath, historyList);
				}
			}

			// Обновляем основное значение ключа
			Data.set(key, newValue);
		}

		// Фиксируем на диск[cite: 3]
		Save();
	}

	public static void Load() {
		FileObject fo = AppHandler.getProfileFile();
		File file = FileUtil.toFile(fo);
		try {
			String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");
			Data = Storage.fromJson(content);
		} catch (IOException e) {
			Data = new Storage();
		}
	}

	public static void Save() {
		FileObject fo = AppHandler.getProfileFile();
		File file = FileUtil.toFile(fo);
		try {
			final String finalContent = Data.toFmtJson();
			java.nio.file.Files.write(file.toPath(), finalContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		} catch (IOException e) {
			Exceptions.printStackTrace(e);
		}
	}

}
