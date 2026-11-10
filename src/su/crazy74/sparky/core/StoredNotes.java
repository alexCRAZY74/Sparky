package su.crazy74.sparky.core;

import java.io.File;
import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

public class StoredNotes {

	protected Storage Data = null;
	protected FileObject assignedFile = null;

	public StoredNotes(FileObject fo) {
		this.assignedFile = fo;
	}

	public StoredNotes Load() {
		if (this.assignedFile == null || this.assignedFile.isVirtual()) {
			this.Data = new Storage();
			return this;
		}

		File file = FileUtil.toFile(this.assignedFile);
		try {
			String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");

			// Проверяем расширение файла (приводим к нижнему регистру на всякий случай)
			String ext = this.assignedFile.getExt().toLowerCase();

			if ("md".equals(ext)) {
				// Если маркдаун — гоним через наш универсальный Flexmark-парсер
				this.Data = Storage.fromMarkdown(content);
			} else {
				// По дефолту (или если json) — старый добрый GSON
				this.Data = Storage.fromJson(content);
			}

		} catch (IOException e) {
			this.Data = new Storage();
		}
		return this;
	}

	public StoredNotes Save() {
		if (this.assignedFile == null || this.Data == null) {
			return this;
		}

		File file = FileUtil.toFile(this.assignedFile);
		try {
			String finalContent;
			String ext = this.assignedFile.getExt().toLowerCase();

			if ("md".equals(ext)) {
				// Собираем карту обратно в Markdown структуру
				finalContent = this.Data.toMarkdown();
			} else {
				// Иначе генерим форматированный JSON
				finalContent = this.Data.toFmtJson();
			}

			java.nio.file.Files.write(file.toPath(), finalContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		} catch (IOException e) {
			Exceptions.printStackTrace(e);
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public StoredNotes Update(Storage newData) {
		java.util.Map<String, Object> updates = newData.get();
		if (updates == null || this.Data == null) {
			return this;
		}

		com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String now = sdf.format(new java.util.Date());

		for (String key : updates.keySet()) {
			if (key.equals("changes")) {
				continue;
			}

			Object newValue = updates.get(key);

			if (Data.Exists(key)) {
				Object oldValue = Data.get(key);

				if (oldValue != null && !oldValue.equals(newValue)) {

					Storage historyEntry = new Storage();
					historyEntry.set("stored", now);

					// ХИТРОСТЬ ТУТ: Если старое значение — это сложная структура (карта или список задач),
					// мы превращаем его в плоскую строку, чтобы маркдаун-генератор не сошел с ума 
					// и не начал дублировать заголовки и списки в геометрической прогрессии!
					if (oldValue instanceof java.util.Map || oldValue instanceof java.util.List) {
						// Переводим структуру в компактный JSON-текст для истории
						String jsonString = gson.toJson(oldValue).replace("\r\n", "\n");
						historyEntry.set("previous_value", "```json\n" + jsonString + "\n```");
					} else {
						historyEntry.set("previous_value", oldValue);
					}

					String historyPath = "changes/" + key;

					java.util.List<Object> historyList = (java.util.List<Object>) Data.get(historyPath);
					if (historyList == null) {
						historyList = new java.util.ArrayList<>();
					}

					historyList.add(historyEntry.get());
					Data.set(historyPath, historyList);
				}
			}

			Data.set(key, newValue);
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public StoredNotes Update_old(Storage newData) {
		if (newData == null) {
			return this;
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
		return this;
	}

	public static void Update(FileObject source, Storage newData) {
		StoredNotes instance = new StoredNotes(source);
		instance.Load().Update(newData).Save();
	}

	public static String toFmtJson(FileObject source) {
		StoredNotes instance = new StoredNotes(source);
		return instance.Load().Data.toFmtJson();
	}
}
