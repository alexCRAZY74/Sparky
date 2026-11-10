package su.crazy74.sparky.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import su.crazy74.sparky.interfaces.i_GermanProject;
import su.crazy74.sparky.ui.ChatTopComponent;
import su.crazy74.sparky.ui.ProjectTopComponent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.text.JTextComponent;
import org.netbeans.api.project.FileOwnerQuery;
import org.openide.cookies.EditorCookie;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.windows.TopComponent;

public class ideHandler {

	public static String getOpenedTabs() {
		return getOpenedTabs(null);
	}

	public static String getOpenedTabs(i_GermanProject currentProject) {
		List<Map<String, Object>> tabsList = getOpenedTabsList(currentProject);
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		return gson.toJson(tabsList);
	}

	public static List<Map<String, Object>> getOpenedTabsList() {
		return getOpenedTabsList(null);
	}

	public static List<Map<String, Object>> getOpenedTabsList(i_GermanProject currentProject) {
		List<Map<String, Object>> tabsList = new ArrayList<>();

		FileObject currentFolder = (currentProject != null) ? currentProject.getFolder() : null;

		Set<TopComponent> opened = TopComponent.getRegistry().getOpened();
		for (TopComponent tc : opened) {
			Map<String, Object> tabInfo = new HashMap<>();

			// 1. Проект / Ментальная карта
			if (tc instanceof ProjectTopComponent) {
				ProjectTopComponent projectTc = (ProjectTopComponent) tc;
				tabInfo.put("type", "mindmap");
				tabInfo.put("title", projectTc.getName());
				tabInfo.put("guid", projectTc.getGUID());

				if (projectTc.getProject() != null && projectTc.getProject().getFolder() != null) {
					tabInfo.put("project_folder", projectTc.getProject().getFolder().toURI().toString());
				}

				boolean isSame = false;
				if (currentFolder != null && projectTc.getProject() != null && projectTc.getProject().getFolder() != null) {
					isSame = currentFolder.equals(projectTc.getProject().getFolder());
				}
				tabInfo.put("is_same_project", isSame);

				tabsList.add(tabInfo);
				continue;
			}

			// 2. Чат
			if (tc instanceof ChatTopComponent) {
				ChatTopComponent chatTc = (ChatTopComponent) tc;
				tabInfo.put("type", "chat");
				tabInfo.put("title", chatTc.getName());
				tabInfo.put("guid", chatTc.getGUID());

				if (chatTc.getProject() != null && chatTc.getProject().getFolder() != null) {
					tabInfo.put("project_folder", chatTc.getProject().getFolder().toURI().toString());
				}

				boolean isSame = false;
				if (currentFolder != null && chatTc.getProject() != null && chatTc.getProject().getFolder() != null) {
					isSame = currentFolder.equals(chatTc.getProject().getFolder());
				}
				tabInfo.put("is_same_project", isSame);

				tabsList.add(tabInfo);
				continue;
			}

			// 3. Файлы (только реально открытые в редакторе тексты/документы)
			DataObject dob = tc.getLookup().lookup(DataObject.class);
			if (dob != null) {
				// Игнорируем папки и сущности без EditorCookie
				if (dob.getPrimaryFile().isFolder()) {
					continue;
				}

				EditorCookie ec = tc.getLookup().lookup(EditorCookie.class);
				if (ec == null) {
					continue;
				}

				tabInfo.put("type", "file");

				FileObject file = dob.getPrimaryFile();
				Project netbeansProject = FileOwnerQuery.getOwner(file);
				boolean isSame = false;
				if (currentFolder != null && netbeansProject != null) {
					isSame = currentFolder.equals(netbeansProject.getProjectDirectory());
				}
				tabInfo.put("is_same_project", isSame);

				if (netbeansProject != null) {
					tabInfo.put("project_folder", netbeansProject.getProjectDirectory().toURI().toString());
				}

				String fullUri = file.toURI().toString();

				String shortUri = fullUri;
				if (currentFolder != null) {
					FileObject pluginDir = currentFolder.getFileObject(".german");
					FileObject trainersDir = (pluginDir != null) ? pluginDir.getFileObject("trainers") : null;

					if (trainersDir != null && FileUtil.getRelativePath(trainersDir, file) != null) {
						shortUri = "file://~trainers/" + FileUtil.getRelativePath(trainersDir, file);
					} else if (pluginDir != null && FileUtil.getRelativePath(pluginDir, file) != null) {
						shortUri = "file://~plugin/" + FileUtil.getRelativePath(pluginDir, file);
					} else if (FileUtil.getRelativePath(currentFolder, file) != null) {
						shortUri = "file://~project/" + FileUtil.getRelativePath(currentFolder, file);
					}
				}

				if (shortUri.equals(fullUri)) {
					File userDir = org.openide.modules.Places.getUserDirectory();
					if (userDir != null) {
						FileObject userDirObj = FileUtil.toFileObject(userDir);
						if (userDirObj != null) {
							String relUser = FileUtil.getRelativePath(userDirObj, file);
							if (relUser != null) {
								shortUri = "file://~user/" + relUser;
							}
						}
					}
				}

				tabInfo.put("uri", shortUri);
				if (isSame) {
					tabInfo.put("full_uri", fullUri);
				}

				final JTextComponent[] foundEditor = new JTextComponent[1];
				try {
					java.awt.EventQueue.invokeAndWait(() -> {
						JTextComponent[] panes = ec.getOpenedPanes();
						if (panes != null && panes.length > 0) {
							foundEditor[0] = panes[0];
						}
					});
				} catch (Exception e) {
					System.err.println("Ошибка получения panes в AWT: " + e.getMessage());
				}

				if (foundEditor[0] != null) {
					JTextComponent editor = foundEditor[0];
					tabInfo.put("cursor_position", editor.getCaretPosition());

					String selectedText = editor.getSelectedText();
					if (selectedText != null && !selectedText.isEmpty()) {
						tabInfo.put("selected_text", selectedText);
					}
				}

				tabsList.add(tabInfo);
			}
		}
		return tabsList;
	}

	public static String getOpenedProjectsJson() {
		List<Map<String, Object>> List = getOpenedProjects();
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		return gson.toJson(List);
	}

	public static List<Map<String, Object>> getOpenedProjects() {
		List<Map<String, Object>> projectsList = new java.util.ArrayList<>();
		Project[] openProjects = OpenProjects.getDefault().getOpenProjects();
		for (Project p : openProjects) {
			ProjectHandler ph = new ProjectHandler(p);
			projectsList.add(ph.getInformation().get());
		}
		return projectsList;
	}
}
