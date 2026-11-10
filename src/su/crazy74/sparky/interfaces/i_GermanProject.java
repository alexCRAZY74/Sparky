package su.crazy74.sparky.interfaces;

import su.crazy74.sparky.core.Storage;
import org.openide.filesystems.FileObject;

public interface i_GermanProject {
	public String getDisplayName();
	public Storage getInformation();
	public Storage config();
	public FileObject getFolder();
	public FileObject getPluginDir();
}
