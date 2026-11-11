package su.crazy74.sparky.action;

import su.crazy74.sparky.core.LanguageHandler;
import su.crazy74.sparky.core.ProjectHandler;
import su.crazy74.sparky.interfaces.i_GermanProject;
import su.crazy74.sparky.ui.ChatTopComponent;
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

@ActionID(
				category = "Window",
				id = "su.crazy74.sparky.ui.NewChatAction"
)
@ActionRegistration(
				displayName = "#CTL_NewChatAction",
				//iconBase = "su/crazy74/sparky/icons/bubble16x16.png",
				lazy = false // Нам нужен false, чтобы NetBeans дергал createContextAwareInstance
)
@ActionReferences({
	// 1. Твое старое доброе верхнее меню
	@ActionReference(path = "Menu/Window/Sparky", position = 0),

	// 2. Железобетонный путь для контекстного меню ЛЮБОГО проекта слева в дереве
	@ActionReference(path = "Editors/Actions", position = 0),
	@ActionReference(path = "Projects/Actions", position = 0)
})
@Messages("CTL_NewChatAction=New AI-Chat")
public final class NewChatAction extends AbstractAction implements ContextAwareAction {

	public NewChatAction() {
		super(LanguageHandler.getDefault().getStr("menu/new_chat", "New AI-Chat"));
		putValue(Action.SMALL_ICON, org.openide.util.ImageUtilities.loadImageIcon("su/crazy74/sparky/icons/bubble16x16.png", true));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		openChat(null);
	}

	@Override
	public Action createContextAwareInstance(Lookup actionContext) {
		return new DelegateAction(actionContext);
	}

	private static void openChat(i_GermanProject project) {
		ChatTopComponent chat = (project != null)
						? new ChatTopComponent(project)
						: new ChatTopComponent();
		chat.open();
		chat.requestActive();
	}

	private static class DelegateAction extends AbstractAction {

		private final Lookup context;

		public DelegateAction(Lookup context) {
			super(LanguageHandler.getDefault().getStr("menu/new_chat", "New AI-Chat"));
			putValue(Action.SMALL_ICON, org.openide.util.ImageUtilities.loadImageIcon("su/crazy74/sparky/icons/bubble16x16.png", true));
			this.context = context;
			putValue("iconBase", "su/crazy74/sparky/icons/bubble16x16.png");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Project project = context.lookup(Project.class);
			if (project != null) {
				openChat(new ProjectHandler(project));
			} else {
				openChat(null);
			}
		}
	}
}
