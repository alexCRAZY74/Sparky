package su.crazy74.sparky.action;

import su.crazy74.sparky.core.AppConfig;
import su.crazy74.sparky.core.LanguageHandler;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Window", id = "su.crazy74.sparky.ui.OpenOptionsAction")
@ActionRegistration(
    displayName = "#CTL_OpenOptionsAction",
    iconBase = "crazy/module/german/icons/settings16x16.png",
    lazy = false
)
@ActionReference(path = "Menu/Window/Sparky", position = 2000)
@Messages("CTL_OpenOptionsAction=Config")
public final class OpenOptionsAction extends AbstractAction {

	public OpenOptionsAction() {
		super(LanguageHandler.getDefault().getStr("menu/options", "Config"));
		putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("crazy/module/german/icons/settings16x16.png", true));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		FileObject configFile = AppConfig.getConfigFile();

		try {
			if (configFile != null) {
				DataObject dataObject = DataObject.find(configFile);
				OpenCookie cookie = dataObject.getLookup().lookup(OpenCookie.class);
				if (cookie != null) {
					cookie.open();
				}
			}
		} catch (Exception ex) {
			Exceptions.printStackTrace(ex);
		}
	}
}
