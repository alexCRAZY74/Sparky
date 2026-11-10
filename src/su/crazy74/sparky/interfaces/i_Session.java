package su.crazy74.sparky.interfaces;

import su.crazy74.sparky.core.ProjectHandler;
import su.crazy74.sparky.core.SessionNode;
import su.crazy74.sparky.core.Storage;
import java.util.List;

public interface i_Session {

	ProjectHandler getProjectHandler();
	public void ActivateNode(SessionNode node);
	public void UpdateNode(SessionNode node);
	public SessionNode getActive();
	public List<SessionNode> getHistory();
	public Storage getProjectInformation();
	public i_GermanProject getProject();
	public boolean isInterrupted();
	public Storage config();
	public Storage info();
	public void setTitle(String newTitle);
	public String getTtile();
	public void setThread(Thread newThread);
}
