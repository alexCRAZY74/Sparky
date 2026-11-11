package su.crazy74.sparky.ui;

import su.crazy74.sparky.core.AppHandler;
import su.crazy74.sparky.interfaces.i_GermanProject;
import java.awt.BorderLayout;
import java.util.UUID;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.openide.windows.TopComponent;

abstract public class JavaFxWindow extends TopComponent {

	protected final String guid = UUID.randomUUID().toString();
	protected i_GermanProject project;
	protected JFXPanel webContent;
	protected WebEngine webEngine;

	public String getGUID() {
		return this.guid;
	}

	public i_GermanProject getProject() {
		return this.project;
	}

	protected void initFxContainer(i_GermanProject project) {
		this.project = project;
		AppHandler.toConsole("initFxContainer");
		javax.swing.SwingUtilities.invokeLater(() -> {
			try {
				initSwingContainer();
				Platform.runLater(this::initFXContent);
			} catch (Throwable t) {
				String err = "initFxContainer error: " + t.getMessage();
				AppHandler.toConsole(err);
				t.printStackTrace();
			} finally {
        AppHandler.toConsole("initFxContainer success");
      }
		});
	}

	private void initSwingContainer() {
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(JFXPanel.class.getClassLoader());
		AppHandler.toConsole("initSwingContainer");
		try {
			webContent = new JFXPanel();
			setLayout(new BorderLayout());
			add(webContent, BorderLayout.CENTER);
			revalidate();
			repaint();
		} catch (Throwable t) {
			String err = "initSwingContainer ERROR: " + t.getMessage();
			AppHandler.toConsole(err);
			t.printStackTrace();
		} finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
			AppHandler.toConsole("initSwingContainer success");
		}
	}

	private void initFXContent() {
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(JFXPanel.class.getClassLoader());
		AppHandler.toConsole("initFXContent");
		try {
			Platform.setImplicitExit(false);
			WebView view = new WebView();
			webEngine = view.getEngine();

			loadContent();

			webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
				if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
					bindBridge(webEngine);
					onPageLoaded(webEngine);
				}
			});

			Scene scene = new Scene(view);
			webContent.setScene(scene);
			AppHandler.toConsole("initFXContent success");
		} catch (Throwable t) {
			String err = "JAVAFX ERROR: " + t.getMessage();
			AppHandler.toConsole(err);
			t.printStackTrace();
		} finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
			AppHandler.toConsole("initFXContent success");
		}
	}

	/**
	 * Загрузка HTML-шаблона в WebEngine.
	 */
	abstract protected void loadContent();

	/**
	 * Привязка JS-моста (Backend) к окну браузера.
	 */
	abstract protected void bindBridge(WebEngine engine);

	/**
	 * Сигнал успешной загрузки страницы для запуска клиентских скриптов.
	 */
	protected void onPageLoaded(WebEngine engine) {
		Platform.runLater(() -> {
			engine.executeScript("$(document).trigger('startWork');");
		});
	}
}
