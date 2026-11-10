package su.crazy74.sparky.core;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

public class BackupManager {

	protected final ProjectHandler hProject;

	public BackupManager(ProjectHandler project) {
		this.hProject = project;
	}

	public BackupManager(Project project) {
		this.hProject = new ProjectHandler(project);
	}

	public static BackupManager inProject(ProjectHandler project) {
		BackupManager instance = new BackupManager(project);
		return instance;
	}

	public static BackupManager inProject(Project project) {
		BackupManager instance = new BackupManager(project);
		return instance;
	}

	public FileObject getFolder() {
		if (this.hProject != null) {
			File backupDir = new File(FileUtil.toFile(this.hProject.getPluginDir()), ".backup");
			if (!backupDir.exists()) {
				backupDir.mkdirs();
			}
			return FileUtil.toFileObject(backupDir);
		}
		return null;
	}

	public void Backup(FileObject sourceFile) {
		if (sourceFile == null || this.hProject == null || this.hProject.getFolder() == null) {
			return;
		}

		AppHandler.toConsole("Запуск бэкапа для файла: " + sourceFile.getNameExt());

		String relativePath = FileUtil.getRelativePath(this.hProject.getFolder(), sourceFile);
		if (relativePath == null) {
			AppHandler.toConsole("   [Ошибка] Не удалось вычислить относительный путь для: " + sourceFile.getPath());
			return;
		}

		FileObject backupRoot = this.getFolder();
		if (backupRoot == null) {
			AppHandler.toConsole("   [Ошибка] Папка бэкапов недоступна.");
			return;
		}

		try {
			String parentPath = "";
			int lastSlash = relativePath.lastIndexOf('/');
			if (lastSlash != -1) {
				parentPath = relativePath.substring(0, lastSlash);
			}

			FileObject targetFolder = backupRoot;
			if (!parentPath.isEmpty()) {
				targetFolder = FileUtil.createFolder(backupRoot, parentPath);
			}

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
			String timeString = LocalDateTime.now().format(formatter);

			String backupName = sourceFile.getName();
			String backupExt = sourceFile.getExt() + "." + timeString + ".bak";

			FileObject backupFile = sourceFile.copy(targetFolder, backupName, backupExt);

			AppHandler.toConsole("   [Успешно] Бэкап создан: " + FileUtil.getRelativePath(backupRoot, backupFile));

		} catch (IOException ex) {
			AppHandler.toConsole("   [Ошибка] Не удалось скопировать файл: " + ex.getMessage());
			Exceptions.printStackTrace(ex);
		}
	}
}
