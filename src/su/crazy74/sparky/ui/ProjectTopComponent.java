package su.crazy74.sparky.ui;

import su.crazy74.sparky.core.AppHandler;
import su.crazy74.sparky.core.FileHandler;
import su.crazy74.sparky.core.MindManager;
import su.crazy74.sparky.core.ProjectHandler;
import su.crazy74.sparky.core.Storage;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import su.crazy74.sparky.interfaces.i_GermanProject;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;
import org.openide.util.Exceptions;
import java.util.concurrent.Future;
import org.netbeans.api.project.ui.OpenProjects;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
				dtd = "-//su.crazy74.sparky.ui//Project//EN",
				autostore = false
)
@TopComponent.Description(
				preferredID = "ProjectTopComponent",
				iconBase = "su/crazy74/sparky/icons/06-wallet-icon24x24.png",
				persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "su.crazy74.sparky.ui.ProjectTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
/*@TopComponent.OpenActionRegistration(
				displayName = "#CTL_ProjectAction",
				preferredID = "ProjectTopComponent"
)*/
@Messages({
	"CTL_ProjectAction=Project",
	"CTL_ProjectTopComponent=Project Window",
	"HINT_ProjectTopComponent=This is a Project window"
})
public final class ProjectTopComponent extends JavaFxWindow {

	private final ProjectJSBridge webBridge = new ProjectJSBridge();

	public ProjectTopComponent() {
		_Construct(resolveDefaultProject());
	}

	public ProjectTopComponent(i_GermanProject project) {
		_Construct(project);
	}

	private static i_GermanProject resolveDefaultProject() {
		i_GermanProject defaultProject = null;
		try {
			Future<org.netbeans.api.project.Project[]> futureProjects = org.netbeans.api.project.ui.OpenProjects.getDefault().openProjects();
			org.netbeans.api.project.Project[] openProjectsArray = futureProjects.get();

			if (openProjectsArray != null && openProjectsArray.length > 0) {
				for (org.netbeans.api.project.Project p : openProjectsArray) {
					defaultProject = new ProjectHandler(p);
					break;
				}
			}
		} catch (Throwable t) {
			// Exceptions.printStackTrace(t); // Опционально: логировать ошибку, если нужно
		}
		return defaultProject;
	}

	private void _Construct(i_GermanProject project) {
		initComponents();
		if (project != null) {
			setName(project.getDisplayName());
		} else {
			setName(Bundle.CTL_ProjectTopComponent());
		}
		setToolTipText(Bundle.HINT_ProjectTopComponent());
		initFxContainer(project);
	}

	@Override
	protected void loadContent() {
		try {
			AppHTML html = new AppHTML("project");
			javafx.application.Platform.runLater(() -> {
				webEngine.load(html.fileUrl);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void bindBridge(javafx.scene.web.WebEngine engine) {
		try {
			netscape.javascript.JSObject jsWindow = (netscape.javascript.JSObject) engine.executeScript("window");
			jsWindow.setMember("Backend", webBridge);
		} catch (Throwable t) {
			AppHandler.toConsole("bindBridge error: " + t.getMessage());
			t.printStackTrace();
		}
	}

	/*protected void bindBridge(javafx.scene.web.WebEngine engine) {
		netscape.javascript.JSObject window = (netscape.javascript.JSObject) engine.executeScript("window");
		window.setMember("Backend", webBridge);
	}*/

	public class ProjectJSBridge {

		public void writeToConsole(String line) {
			AppHandler.toConsole(line);
		}

		public void openURI(String UriStr) {
			javax.swing.SwingUtilities.invokeLater(() -> {
				try {
					java.net.URI uri = new java.net.URI(UriStr);
					String scheme = uri.getScheme();
					if ("file".equals(scheme)) {
						java.io.File file = new java.io.File(uri);
						if (file.isDirectory() && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
							try {
								Desktop.getDesktop().open(file);
							} catch (IOException ex) {
								Exceptions.printStackTrace(ex);
							}
						} else {
							FileHandler.fromURI(UriStr, (ProjectHandler) project).openAtOffset(0);
						}
					} else {
						if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
							try {
								Desktop.getDesktop().browse(uri);
							} catch (IOException ex) {
								Exceptions.printStackTrace(ex);
							}
						}
					}
				} catch (URISyntaxException ex) {
					Exceptions.printStackTrace(ex);
				}
			});
		}

		public String getEnvironment() {
			return AppHandler.getEnv().toJson();
		}

		public String getMapData(String json) {
			if (project == null) {
				return new Storage(new java.util.ArrayList<>()).toJson();
			}
			Storage form = Storage.fromJson(json);
			return MindManager.Project(project).commandGet(form).toJson();
		}

		public String saveMapData(String json) {
			if (project == null) {
				Storage res = new Storage();
				res.set("success", false);
				return res.toJson();
			}
			Storage form = Storage.fromJson(json);
			return MindManager.Project(project).commandSet(form).toJson();
		}

		public String selectFile() {
			return showFileChooser(javax.swing.JFileChooser.FILES_ONLY);
		}

		public String selectFolder() {
			return showFileChooser(javax.swing.JFileChooser.DIRECTORIES_ONLY);
		}

		private String showFileChooser(int mode) {
			final String[] result = new String[]{""};
			Runnable r = () -> {
				javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
				if (project != null && project.getFolder() != null) {
					java.io.File projectDir = org.openide.filesystems.FileUtil.toFile(project.getFolder());
					if (projectDir != null && projectDir.exists()) {
						chooser.setCurrentDirectory(projectDir);
					}
				}
				chooser.setFileSelectionMode(mode);
				int returnVal = chooser.showOpenDialog(null);
				if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
					java.io.File file = chooser.getSelectedFile();
					if (file != null) {
						String uri = file.toURI().toString();
						if (uri.startsWith("file:/") && !uri.startsWith("file:///")) {
							uri = uri.replace("file:/", "file:///");
						}
						result[0] = uri;
					}
				}
			};
			if (javax.swing.SwingUtilities.isEventDispatchThread()) {
				r.run();
			} else {
				try {
					javax.swing.SwingUtilities.invokeAndWait(r);
				} catch (Exception ex) {
					Exceptions.printStackTrace(ex);
				}
			}
			return result[0];
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 400, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 300, Short.MAX_VALUE)
    );
  }// </editor-fold>//GEN-END:initComponents

  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables
	@Override
	public void componentOpened() {
		// TODO add custom code on component opening
	}

	@Override
	public void componentClosed() {
		// TODO add custom code on component closing
	}

	void writeProperties(java.util.Properties p) {
		// better to version settings since initial version as advocated at
		// http://wiki.apidesign.org/wiki/PropertyFiles
		p.setProperty("version", "1.0");
		// TODO store your settings
	}

	void readProperties(java.util.Properties p) {
		String version = p.getProperty("version");
		// TODO read your settings according to their version
	}
}
