package su.crazy74.sparky.interfaces;

import su.crazy74.sparky.ai.aiModelHandler;
import su.crazy74.sparky.core.SessionNode;
import java.util.List;

public interface i_RequestAI {
	public static i_RequestAI Construct(aiModelHandler model){
		return null;
	};
	public SessionNode Request(List<String> data);
}
