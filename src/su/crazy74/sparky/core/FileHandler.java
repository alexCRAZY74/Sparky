package su.crazy74.sparky.core;

import java.io.File;
import java.net.URI;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

public class FileHandler {

	protected ProjectHandler hProject;
	protected FileObject sourceFile;
	protected String errorMessage = null;

	// Приватный конструктор, чтобы создавать объекты только через фабрику
	private FileHandler() {
	}

	public static FileHandler fromURI(Storage form, ProjectHandler defaultProject) {
		return fromURI(form.getStr("uri"), defaultProject);
	}

	public static FileHandler fromURI(String uriStr, ProjectHandler defaultProject) {
		FileHandler instance = new FileHandler();

		if (uriStr == null || uriStr.isEmpty()) {
			instance.errorMessage = "Пустой URI файла.";
			return instance;
		}

		try {
			// 1. Проверяем макросы, требующие контекст проекта (~project, ~plugin, ~trainers, + совместимость с @)
			if (uriStr.startsWith("file://~project") || uriStr.startsWith("file://@project")
							|| uriStr.startsWith("file://~plugin") || uriStr.startsWith("file://@plugin")
							|| uriStr.startsWith("file://~trainers") || uriStr.startsWith("file://@trainers")) {

				if (defaultProject == null || defaultProject.getFolder() == null) {
					instance.errorMessage = "Макрос использует контекст проекта, но текущая сессия не привязана к проекту.";
					return instance;
				}
				instance.hProject = defaultProject;
			}

			// Сценарий 1: Относительный путь ~project
			if (uriStr.startsWith("file://~project") || uriStr.startsWith("file://@project")) {
				FileObject projectRoot = defaultProject.getFolder();
				String relativePath = uriStr.substring(uriStr.indexOf('/', 7)); // Отрезаем до первого слэша
				if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
					relativePath = relativePath.substring(1);
				}
				instance.sourceFile = projectRoot.getFileObject(relativePath);
				if (instance.sourceFile == null) {
					instance.errorMessage = "Файл не найден внутри проекта по пути: " + relativePath;
				}
			} // Сценарий 2: Относительный путь ~plugin
			else if (uriStr.startsWith("file://~plugin") || uriStr.startsWith("file://@plugin")) {
				FileObject pluginDir = defaultProject.getPluginDir();
				if (pluginDir == null) {
					instance.errorMessage = "Не удалось получить директорию плагина.";
					return instance;
				}
				String relativePath = uriStr.substring(uriStr.indexOf('/', 7));
				if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
					relativePath = relativePath.substring(1);
				}
				instance.sourceFile = pluginDir.getFileObject(relativePath);
				if (instance.sourceFile == null) {
					instance.errorMessage = "Файл не найден в директории плагина по пути: " + relativePath;
				}
			} // Сценарий 3: Относительный путь ~trainers
			else if (uriStr.startsWith("file://~trainers") || uriStr.startsWith("file://@trainers")) {
				FileObject pluginDir = defaultProject.getPluginDir();
				if (pluginDir == null) {
					instance.errorMessage = "Не удалось получить директорию плагина (для ~trainers).";
					return instance;
				}
				FileObject trainersDir = pluginDir.getFileObject("trainers");
				if (trainersDir == null) {
					try {
						trainersDir = FileUtil.createFolder(pluginDir, "trainers");
					} catch (java.io.IOException ex) {
						instance.errorMessage = "Не удалось создать папку тренеров: " + ex.getMessage();
						return instance;
					}
				}
				String relativePath = uriStr.substring(uriStr.indexOf('/', 7));
				if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
					relativePath = relativePath.substring(1);
				}
				instance.sourceFile = trainersDir.getFileObject(relativePath);
				if (instance.sourceFile == null) {
					instance.errorMessage = "Файл не найден в папки тренеров по пути: " + relativePath;
				}
			} // Сценарий 4: Относительный путь ~user
			else if (uriStr.startsWith("file://~user") || uriStr.startsWith("file://@user")) {
				File userDir = org.openide.modules.Places.getUserDirectory();
				FileObject userDirObj = FileUtil.toFileObject(userDir);
				String relativePath = uriStr.substring(uriStr.indexOf('/', 7));
				if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
					relativePath = relativePath.substring(1);
				}
				instance.sourceFile = userDirObj.getFileObject(relativePath);
				if (instance.sourceFile == null) {
					instance.errorMessage = "Файл не найден в пользовательской директории: " + relativePath;
				}
				if (defaultProject != null) {
					instance.hProject = defaultProject;
				}
			} 			// Сценарий 5: Прямой путь (file:/...)
			else {
				String cleanUri = uriStr;
				if (cleanUri.startsWith("file://") && !cleanUri.startsWith("file:///")) {
					cleanUri = "file:///" + cleanUri.substring(7);
				}
				URI uri = new URI(cleanUri);
				File file = new File(uri);
				instance.sourceFile = FileUtil.toFileObject(file);

				if (instance.sourceFile == null) {
					instance.errorMessage = "Файл не существует или недоступен по пути: " + uriStr;
					return instance;
				}

				Project netbeansProject = FileOwnerQuery.getOwner(instance.sourceFile);
				if (netbeansProject == null) {
					if (defaultProject != null) {
						instance.hProject = defaultProject;
					} else {
						instance.errorMessage = "Безопасный патч заблокирован: файл не входит ни в один открытый проект NetBeans.";
						return instance;
					}
				} else {
					instance.hProject = new ProjectHandler(netbeansProject);
				}
			}

		} catch (Exception e) {
			instance.errorMessage = "Ошибка разбора URI: " + e.getMessage();
		}

		return instance;
	}

// Добавим вспомогательный метод для получения содержимого файла
	public String getContent() {
		if (!isValid()) {
			return null;
		}
		try {
			File file = FileUtil.toFile(this.sourceFile);
			if (file != null && file.exists() && file.isFile()) {
				return java.nio.file.Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8);
			}
		} catch (Exception ex) {
			Exceptions.printStackTrace(ex);
		}
		return null;
	}

	public boolean isValid() {
		return this.errorMessage == null && this.sourceFile != null && this.hProject != null;
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	public FileHandler Backup() {
		// Делаем бэкап только если проверки Шага 1 прошли успешно
		if (this.isValid()) {
			BackupManager.inProject(this.hProject).Backup(this.sourceFile);
		}
		return this;
	}

	public Storage ApplyCodePatch(Storage form) {
		Storage result = new Storage();
		AppHandler.toConsole("FileHandler.ApplyCodePatch: " + form.toFmtJson());

		// 1. Проверяем валидность контекста (был ли найден файл и проект на шаге инициализации)
		if (this.hProject == null || this.sourceFile == null) {
			result.set("success", false);
			result.set("error", "Невалидный контекст файла или проекта. Возможно, бэкап не удался.");
			return result;
		}

		try {
			int startHint = Integer.parseInt(form.getStr("start"));
			String origin = form.getStr("origin");
			String replace = form.getStr("replace");

			if (origin == null || origin.isEmpty()) {
				result.set("success", false);
				result.set("error", "Оригинальный текст (origin) пуст.");
				return result;
			}

			// 2. Достаем документ через Cookie редактора NetBeans
			org.openide.loaders.DataObject dataObject = org.openide.loaders.DataObject.find(this.sourceFile);
			org.openide.cookies.EditorCookie editorCookie = dataObject.getLookup().lookup(org.openide.cookies.EditorCookie.class);

			if (editorCookie == null) {
				result.set("success", false);
				result.set("error", "Не удалось открыть редактор для файла " + this.sourceFile.getNameExt());
				return result;
			}

			javax.swing.text.StyledDocument doc = editorCookie.openDocument();
			if (doc == null) {
				result.set("success", false);
				result.set("error", "Документ пуст или заблокирован.");
				return result;
			}

			// 3. Читаем весь текущий текст из памяти редактора
			String fullText = doc.getText(0, doc.getLength());
			int actualIdx = -1;

			// Нормализуем переносы строк (\r\n -&gt; \n, \r -&gt; \n) для веерного поиска
			String normFullText = fullText.replace("\r\n", "\n").replace("\r", "\n");
			String normOrigin = origin.replace("\r\n", "\n").replace("\r", "\n");
			String normReplace = replace != null ? replace.replace("\r\n", "\n").replace("\r", "\n") : "";

			int maxLen = normFullText.length();
			int startAnchor = Math.min(Math.max(startHint, 0), maxLen);

			// Шаг А: Точный поиск без тримминга
			actualIdx = findMatch(normFullText, normOrigin, startAnchor);

			// Шаг Б: Если не нашли, пробуем триммированный поиск (игнорируем внешние пробелы/переносы)
			boolean wasTrimmedMatch = false;
			if (actualIdx == -1) {
				String trimmedOrigin = normOrigin.trim();
				if (!trimmedOrigin.isEmpty()) {
					actualIdx = findMatch(normFullText, trimmedOrigin, startAnchor);
					if (actualIdx != -1) {
						normOrigin = trimmedOrigin;
						wasTrimmedMatch = true;
					}
				}
			}

			// Шаг В: Специальный фоллбэк для полной замены файла (если точный и триммированный поиск провалились)
			if (actualIdx == -1) {
				boolean isEntireFileReplacement = (startHint == 0 && normOrigin.length() > maxLen * 0.8)
								|| normOrigin.trim().length() > maxLen * 0.9;

				if (isEntireFileReplacement) {
					actualIdx = 0;
					normOrigin = normFullText; // Заменяем весь текст целиком
					AppHandler.toConsole("   [Патч] Точный поиск провалился, но обнаружена полная замена файла. Применяем фоллбэк.");
				}
			}

			// 5. Если цель найдена — точечно вырезаем старый оригинал и пишем замену
			if (actualIdx != -1) {
				// Выравниваем начальные и конечные переносы строк между normOrigin и normReplace
				if (!normOrigin.startsWith("\n") && normReplace.startsWith("\n")) {
					normReplace = normReplace.substring(1);
				}
				if (normOrigin.endsWith("\n") && !normReplace.endsWith("\n") && !normReplace.isEmpty()) {
					normReplace += "\n";
				} else if (!normOrigin.endsWith("\n") && normReplace.endsWith("\n")) {
					normReplace = normReplace.substring(0, normReplace.length() - 1);
				}

				// Пересчитываем нормализованные индексы обратно на физические с учетом реальных \r\n в файле
				int realStart = getRealOffset(fullText, normFullText, actualIdx);
				int realEnd = getRealOffset(fullText, normFullText, actualIdx + normOrigin.length());

				// Модифицируем документ прямо в памяти NetBeans
				doc.remove(realStart, realEnd - realStart);
				doc.insertString(realStart, normReplace, null);

				// Принудительно сохраняем изменения из буфера редактора на диск
				editorCookie.saveDocument();

				result.set("success", true);
				AppHandler.toConsole("   [Патч] Успешно применен к файлу на физической позиции: " + realStart);
			} else {
				result.set("success", false);
				result.set("error", "Оригинальный кусок кода не найден в файле. Возможно, он уже изменен.");
				AppHandler.toConsole("   [Ошибка] Оригинальный текст патча не найден в файле.");
			}

		} catch (Exception e) {
			result.set("success", false);
			result.set("error", "Ошибка применения патча: " + e.getMessage());
			AppHandler.toConsole("   [Ошибка] Исключение при выполнении патча: " + e.getMessage());
		}

		return result;
	}

	public boolean openAtOffset(int offset) {
		Storage dummy = new Storage();
		dummy.set("start", offset);
		return openAtOffset(dummy);
	}

	/**
	 * Открывает файл в редакторе NetBeans и позиционирует курсор на основе формы
	 * (смещение + поиск фрагмента).
	 */
	public boolean openAtOffset(Storage form) {
		if (!this.isValid()) {
			return false;
		}
		try {
			int startHint = form.getInt("start");
			String origin = form.getStr("origin");

			org.openide.loaders.DataObject dataObject = org.openide.loaders.DataObject.find(this.sourceFile);
			org.openide.cookies.EditorCookie editorCookie = dataObject.getLookup().lookup(org.openide.cookies.EditorCookie.class);
			if (editorCookie == null) {
				return false;
			}

			javax.swing.text.StyledDocument doc = editorCookie.openDocument();
			int targetOffset = startHint;

			if (doc != null && origin != null && !origin.isEmpty()) {
				String fullText = doc.getText(0, doc.getLength());
				String normFullText = fullText.replace("\r\n", "\n").replace("\r", "\n");
				String normOrigin = origin.replace("\r\n", "\n").replace("\r", "\n");

				int maxLen = normFullText.length();
				int startAnchor = Math.min(Math.max(startHint, 0), maxLen);

				int actualIdx = findMatch(normFullText, normOrigin, startAnchor);

				if (actualIdx == -1) {
					String trimmedOrigin = normOrigin.trim();
					if (!trimmedOrigin.isEmpty()) {
						actualIdx = findMatch(normFullText, trimmedOrigin, startAnchor);
					}
				}

				if (actualIdx != -1) {
					targetOffset = getRealOffset(fullText, normFullText, actualIdx);
				}
			}

			final int finalOffset = targetOffset;
			editorCookie.open();
			javax.swing.SwingUtilities.invokeLater(() -> {
				try {
					javax.swing.text.JTextComponent[] panes = editorCookie.getOpenedPanes();
					if (panes != null && panes.length > 0) {
						int safeOffset = Math.min(Math.max(finalOffset, 0), panes[0].getDocument().getLength());
						panes[0].setCaretPosition(safeOffset);
						panes[0].requestFocusInWindow();
					}
				} catch (Exception ex) {
					AppHandler.toConsole("   [Ошибка] Не удалось спозиционировать курсор: " + ex.getMessage());
				}
			});
			return true;
		} catch (Exception e) {
			AppHandler.toConsole("   [Ошибка] Исключение при открытии файла: " + e.getMessage());
			return false;
		}
	}

	private int findMatch(String text, String pattern, int startAnchor) {
		int maxLen = text.length();
		int patternLen = pattern.length();
		if (patternLen == 0) {
			return -1;
		}

		for (int offset = 0; offset < maxLen; offset++) {
			int forwardCheck = startAnchor + offset;
			if (forwardCheck + patternLen <= maxLen) {
				if (text.regionMatches(forwardCheck, pattern, 0, patternLen)) {
					return forwardCheck;
				}
			}

			int backwardCheck = startAnchor - offset;
			if (offset > 0 && backwardCheck >= 0 && backwardCheck + patternLen <= maxLen) {
				if (text.regionMatches(backwardCheck, pattern, 0, patternLen)) {
					return backwardCheck;
				}
			}
		}

		return text.indexOf(pattern);
	}

	/**
	 * Пересчитывает индекс из нормализованного текста (где только \n) в реальный
	 * индекс исходного текста (где могут быть \r\n).
	 */
	private int getRealOffset(String fullText, String normFullText, int normTargetIdx) {
		if (normTargetIdx <= 0) {
			return 0;
		}

		int normCursor = 0;
		int realCursor = 0;
		int fullLen = fullText.length();

		while (realCursor < fullLen && normCursor < normTargetIdx) {
			char cReal = fullText.charAt(realCursor);

			// Если встретили виндовый перенос \r\n, то в normFullText ему 
			// соответствует всего один символ \n. Пропускаем \r в физическом смещении.
			if (cReal == '\r' && realCursor + 1 < fullLen && fullText.charAt(realCursor + 1) == '\n') {
				realCursor++; // Шагаем по \r
				continue;     // На следующей итерации захватим \n и сдвинем оба курсора
			}

			realCursor++;
			normCursor++;
		}

		return realCursor;
	}

	/**
	 * Генерирует экономный текст изменений (diff) между последним бэкапом и
	 * текущим состоянием файла в IDE
	 */
	public String getDiffWithLatestBackup() {
		if (this.sourceFile == null || this.hProject == null) {
			return "Файл или проект не инициализированы.";
		}

		try {
			// 1. Стучимся к нашему BackupManager
			BackupManager bm = BackupManager.inProject(this.hProject);
			String relativePath = FileUtil.getRelativePath(this.hProject.getFolder(), this.sourceFile);
			String parentPath = "";
			int lastSlash = relativePath.lastIndexOf('/');
			if (lastSlash != -1) {
				parentPath = relativePath.substring(0, lastSlash);
			}
			File backupDir = new File(bm.getFolder().getPath(), parentPath);

			FileObject backupFolder = FileUtil.toFileObject(backupDir);
			//FileObject backupFolder = bm.getFolder();

			if (backupFolder == null) {
				return "Папка не найдена.";
			}
			// 2. Ищем последний по времени .bak файл для нашего sourceFile
			// (Твой BackupManager сохраняет их с расширением типа .java.2026-06-01_21-18-47.bak)
			FileObject latestBackup = null;
			long latestTime = 0;

			// Перебираем файлы в бэкап-папке (или по относительному пути)
			for (FileObject f : backupFolder.getChildren()) {
				// Проверяем, что бэкап относится к нашему файлу
				if (f.getName().startsWith(this.sourceFile.getName()) && f.getExt().contains("bak")) {
					long fileTime = f.lastModified().getTime();
					if (fileTime > latestTime) {
						latestTime = fileTime;
						latestBackup = f;
					}
				}
			}

			if (latestBackup == null) {
				return "Бэкап для этого файла ещё не создавался. Сравнивать не с чем.";
			}

			// 3. Чисто пролетарский диффер на стандартной Java (без капризных библиотек IDE)
			java.util.List<String> oldLines = new java.util.ArrayList<>();
			java.util.List<String> newLines = new java.util.ArrayList<>();

			// Читаем старый файл (бэкап)
			try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(latestBackup.getInputStream(), "UTF-8"))) {
				String line;
				while ((line = r.readLine()) != null) {
					oldLines.add(line);
				}
			}

			// Читаем текущий файл
			try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(this.sourceFile.getInputStream(), "UTF-8"))) {
				String line;
				while ((line = r.readLine()) != null) {
					newLines.add(line);
				}
			}

			java.lang.StringBuilder sb = new java.lang.StringBuilder();
			int maxLines = Math.max(oldLines.size(), newLines.size());
			boolean hasChanges = false;

			// Сравниваем файлы построчно
			for (int i = 0; i < maxLines; i++) {
				String oldLine = (i < oldLines.size()) ? oldLines.get(i) : null;
				String newLine = (i < newLines.size()) ? newLines.get(i) : null;

				// Если строки отличаются
				if ((oldLine == null && newLine != null) || (oldLine != null && !oldLine.equals(newLine))) {
					hasChanges = true;
					sb.append(String.format("Строка %d:\n", i + 1));
					if (oldLine != null) {
						sb.append("Было : ").append(oldLine).append("\n");
					} else {
						sb.append("Было : [Конец файла]\n");
					}
					if (newLine != null) {
						sb.append("Стало: ").append(newLine).append("\n");
					} else {
						sb.append("Стало: [Строка удалена]\n");
					}
					sb.append("-----------------------------------\n");
				}
			}

			if (!hasChanges) {
				return "Изменений не обнаружено.";
			}

			return sb.toString();

		} catch (Exception ex) {
			return "[Ошибка NetBeans Diff]: " + ex.getMessage();
		}
	}

}
