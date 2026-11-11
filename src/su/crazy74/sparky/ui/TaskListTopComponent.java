package su.crazy74.sparky.ui;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import su.crazy74.sparky.core.ProjectHandler;

@ConvertAsProperties(
				dtd = "-//su.crazy74.sparky.ui//TaskList//EN",
				autostore = false
)
@TopComponent.Description(
				preferredID = "TaskListTopComponent",
				iconBase = "su/crazy74/sparky/icons/display24x24.png",
				persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "rightSlidingSide", openAtStartup = false)
@ActionID(category = "Window", id = "su.crazy74.sparky.ui.TaskListAction")
//@ActionReference(path = "Menu/Window/German", position = 100)
@TopComponent.OpenActionRegistration(
				displayName = "#CTL_TaskListAction",
				preferredID = "TaskListTopComponent"
)
@Messages({
	"CTL_TaskListAction=TaskList",
	"CTL_TaskListTopComponent=TaskList",
	"HINT_TaskListTopComponent=This is a TaskList window"
})
public final class TaskListTopComponent extends TopComponent implements ExplorerManager.Provider {

	private final ExplorerManager mgr = new ExplorerManager();
	private final PropertyChangeListener projectListener = this::handleProjectChange;

	public TaskListTopComponent() {
		setName(Bundle.CTL_TaskListTopComponent());
		setLayout(new BorderLayout());

		// Нативное дерево NetBeans
		BeanTreeView btv = new BeanTreeView();
		btv.setRootVisible(false); // Прячем пустой корень, чтобы видеть только список проектов
		add(btv, BorderLayout.CENTER);

		refreshProjects();
	}

	private void handleProjectChange(PropertyChangeEvent evt) {
		if (OpenProjects.PROPERTY_OPEN_PROJECTS.equals(evt.getPropertyName())) {
			refreshProjects();
		}
	}

	private void refreshProjects() {
		Project[] openProjects = OpenProjects.getDefault().getOpenProjects();

		// 1. Создаем контейнер-массив для нод
		Children.Array projectChildren = new Children.Array();

		for (Project p : openProjects) {
			// Оборачиваем проект в Handler и создаем твой NodeProject
			ProjectHandler ph = new ProjectHandler(p);
			// 2. Добавляем ноду в массив
			projectChildren.add(new Node[]{new NodeProject(ph)});
		}

		// 3. Устанавливаем этот массив как детей невидимого корня
		mgr.setRootContext(new AbstractNode(projectChildren));
	}

	@Override
	public ExplorerManager getExplorerManager() {
		return mgr;
	}

	@Override
	public void componentOpened() {
		OpenProjects.getDefault().addPropertyChangeListener(projectListener);
		refreshProjects();
	}

	@Override
	public void componentClosed() {
		OpenProjects.getDefault().removePropertyChangeListener(projectListener);
	}

	void writeProperties(java.util.Properties p) {
		p.setProperty("version", "1.0");
	}

	void readProperties(java.util.Properties p) {
	}

	// initComponents больше не нужен, так как верстка программная
}
