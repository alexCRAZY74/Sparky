package su.crazy74.sparky.ui;

import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.ImageUtilities;
import su.crazy74.sparky.interfaces.i_GermanProject;
import su.crazy74.sparky.action.EditProjectAction;
import su.crazy74.sparky.action.NewChatAction;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;


public class NodeProject extends AbstractNode {

	private static final String ICON_PATH = "crazy/module/german/icons/06-wallet-icon16x16.png";
	private final i_GermanProject project;

	public NodeProject(i_GermanProject project) {

		super(Children.LEAF, Lookups.singleton(project));
		this.project = project;
		setName("GermanProjectNode");
	}

	@Override
	public String getDisplayName() {
		return this.project.getDisplayName();
	}

	@Override
	public Image getIcon(int type) {
		return ImageUtilities.loadImage(ICON_PATH);
	}

	@Override
	public Image getOpenedIcon(int type) {
		return getIcon(type);
	}


	@Override
	public Action[] getActions(boolean context) {
		return new Action[]{
			new NewChatAction(),
			new EditProjectAction()
		};
	}
}
