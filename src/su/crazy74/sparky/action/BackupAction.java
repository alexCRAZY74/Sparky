package su.crazy74.sparky.action;

import su.crazy74.sparky.core.AppHandler;
import su.crazy74.sparky.core.LanguageHandler;
import su.crazy74.sparky.core.BackupManager;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

@ActionID(
				category = "Build",
				id = "su.crazy74.sparky.action.BackupAction"
)
@ActionRegistration(
				displayName = "#CTL_BackupAction",
				lazy = false // Заставляем IDE опрашивать createContextAwareInstance на предмет контекста
)
@ActionReferences({
	@ActionReference(path = "Editors/Actions", position = 500),
	@ActionReference(path = "Projects/Actions", position = 500),

	@ActionReference(path = "Loaders/content/unknown/Actions", position = 500),
	@ActionReference(path = "Loaders/text/x-java/Actions", position = 500),
	@ActionReference(path = "Loaders/text/x-json/Actions", position = 500),
	@ActionReference(path = "Editors/text/x-json/Actions", position = 500),
	@ActionReference(path = "Loaders/text/html/Actions", position = 500),
	@ActionReference(path = "Loaders/text/xhtml/Actions", position = 500),
	@ActionReference(path = "Loaders/text/x-php5/Actions", position = 500),
	@ActionReference(path = "Editors/text/x-php5/Actions", position = 500),
	@ActionReference(path = "Loaders/text/x-php/Actions", position = 500),
	@ActionReference(path = "Editors/text/x-php/Actions", position = 500),
	@ActionReference(path = "Loaders/text/javascript/Actions", position = 0),
	@ActionReference(path = "Loaders/text/css/Actions", position = 500),
	@ActionReference(path = "Editors/text/css/Actions", position = 500),
	@ActionReference(path = "Loaders/text/x-scss/Actions", position = 500),
	@ActionReference(path = "Editors/text/x-scss/Actions", position = 500),
	@ActionReference(path = "Loaders/text/scss/Actions", position = 500),
	@ActionReference(path = "Editors/text/scss/Actions", position = 500)
})
@Messages("CTL_BackupAction=Sparky: Создать бэкап")
public final class BackupAction extends AbstractAction implements ContextAwareAction {

	public BackupAction() {
		super(LanguageHandler.getDefault().getStr("menu/backup", "Sparky: Create Backup"));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// Не используется
	}

	@Override
	public Action createContextAwareInstance(Lookup actionContext) {
		return new DelegateAction(actionContext);
	}

	private static class DelegateAction extends AbstractAction {

		private final Lookup context;

		public DelegateAction(Lookup context) {
			super(LanguageHandler.getDefault().getStr("menu/backup", "Sparky: Create Backup"));
			this.context = context;

			DataObject dob = context.lookup(DataObject.class);
			if (dob != null) {
				FileObject fo = dob.getPrimaryFile();
				// Жесткий фильтр: папки — нафиг, пускаем только файлы внутри проектов
				if (fo != null && !fo.isFolder() && FileOwnerQuery.getOwner(fo) != null) {
					setEnabled(true);
					return;
				}
			}
			setEnabled(false);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			DataObject dob = context.lookup(DataObject.class);
			if (dob != null) {
				FileObject sourceFile = dob.getPrimaryFile();
				if (sourceFile != null) {
					Project project = FileOwnerQuery.getOwner(sourceFile);
					if (project != null) {
						BackupManager.inProject(project).Backup(sourceFile);
					}
				}
			}
		}
	}
}
