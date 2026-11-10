package su.crazy74.sparky.core;

import java.io.File;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.netbeans.api.project.ProjectUtils;
import su.crazy74.sparky.interfaces.i_GermanProject;
import su.crazy74.sparky.interfaces.i_Session;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.openide.filesystems.FileUtil;

public class ProjectHandler implements i_GermanProject {

	private final Project project;
	private final FileObject germanFolder;
	private final FileObject mapFile;
	protected List<String> treeIgnore = new ArrayList<>();

	public ProjectHandler(Project project) {
		// 1. Жестко зашитый базовый минимум, который нельзя отключать
		this.treeIgnore.add("nbproject/");
		this.treeIgnore.add(".german/.backup/");
		this.treeIgnore.add(".sparky/.backup/");

		this.project = project;
		this.germanFolder = getPluginDir();

		if (this.germanFolder != null) {
			this.mapFile = this.germanFolder.getFileObject("project_map.md");
		} else {
			this.mapFile = null;
		}

		// 2. Подгружаем пользовательские настройки игнорирования из config.json
		this.loadUserIgnorePaths();
		//AppHandler.toConsole("treeIgnore: " + this.treeIgnore);
	}

	public static ProjectHandler BySession(i_Session session) {
		return session.getProjectHandler();
	}

	public FileObject getPluginDir() {
		File projectDir = FileUtil.toFile(this.getFolder());
		File sparkyDir = new File(projectDir, ".sparky");
		File germanDir = new File(projectDir, ".german");

		File pluginDir = sparkyDir;
		if (!sparkyDir.exists() && germanDir.exists()) {
			pluginDir = germanDir;
		}
		if (!pluginDir.exists()) {
			pluginDir.mkdirs();
		}
		return FileUtil.toFileObject(pluginDir);
	}

	public FileObject getConfigFile() {
		return this.getFile("config.json");
	}

	public FileObject getTrainersFile() {
		return this.getFile("trainers.json");
	}

	public FileObject getProfileFile() {
		return this.getFile("profile.json");
	}

	private FileObject getFile(String name) {
		FileObject dir = getPluginDir();
		if (dir == null) {
			return null;
		}
		FileObject fo = dir.getFileObject(name);
		if (fo == null) {
			try {
				fo = FileUtil.createData(dir, name);
			} catch (java.io.IOException ex) {
				org.openide.util.Exceptions.printStackTrace(ex);
			}
		}
		return fo;
	}

	public String ToURI(FileObject fo) {
		if (fo == null) {
			return null;
		}
		//AppHandler.toConsole("ToURI: "+fo.toString());
		return fo.toURI().toString().replace(getFolder().toURI().toString(), "file://@project/");
	}

	@Override
	public String getDisplayName() {
		return ProjectUtils.getInformation(project).getDisplayName();
	}

	@Override
	public FileObject getFolder() {
		if (this.project != null) {
			return this.project.getProjectDirectory();
		}
		return null;
	}

	@Override
	public Storage getInformation() {
		Storage data = new Storage();
		if (this.project != null) {
			data.set("name", ProjectUtils.getInformation(this.project).getName());
			data.set("display_name", ProjectUtils.getInformation(this.project).getDisplayName());

			// Вместо getName() получаем полный путь через FileUtil
			data.set("folder", getFolder().getPath());
		}
		return data;
	}

	public String FileTree() {
		//AppHandler.toConsole("FileTree folder: " + getFolder().toURI());
		return FileTree(new File(getFolder().getPath()), "", true);
	}

	public String FileTree(File folder, String indent, boolean isLast) {
		StringBuilder sb = new StringBuilder();
		String relativePath = folder.toURI().toString().replace(getFolder().toURI().toString(), "");
		if (this.treeIgnore.contains(relativePath)) {
			return "";
		}
		//AppHandler.toConsole("FileTree relativePath: " + relativePath);
		sb.append(indent).append(isLast ? "└── " : "├── ").append(folder.getName()).append("\n");

		// Фильтруем мусор сразу
		File[] files = folder.listFiles(f -> !f.getName().startsWith(".")
						&& !f.getName().equals("vendor")
						&& !f.getName().equals("node_modules"));

		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				boolean lastChild = (i == files.length - 1);
				sb.append(FileTree(files[i], indent + (isLast ? "    " : "│   "), lastChild));
			}
		}
		return sb.toString();
	}

	private void loadUserIgnorePaths() {
		for (String path : this.config().getStrList("ignore_paths")) {
			if (path != null && !path.trim().isEmpty()) {
				String cleanPath = path.trim();
				// На всякий случай проверяем дубликаты
				if (!this.treeIgnore.contains(cleanPath)) {
					this.treeIgnore.add(cleanPath);
				}
			}
		}
	}

	@Override
	public Storage config() {
		Storage result = new Storage();
		try {
			FileObject configFile = this.getConfigFile();
			if (configFile != null && configFile.isValid()) {
				String jsonContent = new String(configFile.asBytes(), StandardCharsets.UTF_8);
				if (!jsonContent.trim().isEmpty()) {
					Storage configStorage = Storage.fromJson(jsonContent);
					return configStorage;
				}
			}
		} catch (Exception e) {
		}
		return result;
	}

}
