
package su.crazy74.sparky.action;

import su.crazy74.sparky.core.LanguageHandler;
import su.crazy74.sparky.core.ProjectHandler;
import su.crazy74.sparky.interfaces.i_GermanProject;
import su.crazy74.sparky.ui.ProjectTopComponent;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

@ActionID(
    category = "Window",
    id = "su.crazy74.sparky.action.EditProjectAction"
)
@ActionRegistration(
    displayName = "#CTL_EditProjectAction",
    iconBase = "su/crazy74/sparky/icons/settings16x16.png",
    lazy = false
)
@ActionReferences({
    @ActionReference(path = "Menu/Window/Sparky", position = 10),
    @ActionReference(path = "Editors/Actions", position = 10),
    @ActionReference(path = "Projects/Actions", position = 10)
})
@Messages("CTL_EditProjectAction=Project Editor")
public final class EditProjectAction extends AbstractAction implements ContextAwareAction {

    public EditProjectAction() {
        super(LanguageHandler.getDefault().getStr("menu/project_map", "Project Editor"));
        putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("su/crazy74/sparky/icons/settings16x16.png", true));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        openProjectEditor(resolveProject(null));
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new DelegateAction(actionContext);
    }

    private static i_GermanProject resolveProject(Lookup context) {
        if (context != null) {
            i_GermanProject project = context.lookup(i_GermanProject.class);
            if (project != null) {
                return project;
            }
            Project nbProject = context.lookup(Project.class);
            if (nbProject != null) {
                return new ProjectHandler(nbProject);
            }
        }
        Project mainProject = org.netbeans.api.project.ui.OpenProjects.getDefault().getMainProject();
        if (mainProject != null) {
            return new ProjectHandler(mainProject);
        }
        Project[] openProjects = org.netbeans.api.project.ui.OpenProjects.getDefault().getOpenProjects();
        if (openProjects.length == 1) {
            return new ProjectHandler(openProjects[0]);
        }
        return null;
    }

    private static void openProjectEditor(i_GermanProject project) {
        ProjectTopComponent editor = (project != null)
                ? new ProjectTopComponent(project)
                : new ProjectTopComponent();
        editor.open();
        editor.requestActive();
    }

    private static class DelegateAction extends AbstractAction {

        private final Lookup context;

        public DelegateAction(Lookup context) {
            super(LanguageHandler.getDefault().getStr("menu/project_map", "Project Editor"));
            putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("su/crazy74/sparky/icons/settings16x16.png", true));
            this.context = context;
            putValue("iconBase", "su/crazy74/sparky/icons/settings16x16.png");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            openProjectEditor(resolveProject(context));
        }
    }
}
