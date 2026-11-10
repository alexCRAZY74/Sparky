package su.crazy74.sparky.ai;

import su.crazy74.sparky.core.AppConfig;
import su.crazy74.sparky.core.AppHandler;
import su.crazy74.sparky.core.ProjectHandler;
import su.crazy74.sparky.core.SessionNode;
import su.crazy74.sparky.core.Storage;
import su.crazy74.sparky.interfaces.i_RequestAI;
import su.crazy74.sparky.interfaces.i_Session;

public class aiModelHandler {

	protected Storage data = new Storage();
	protected String chosenApiKey = null;

	public aiModelHandler() {
	}

	public static aiModelHandler Construct(i_Session session) {
		aiModelHandler model = new aiModelHandler();
		ProjectHandler hProject = session.getProjectHandler();
		String companyKey = "defaultCompany";
		if (hProject != null) {
			Storage pConfig = hProject.config();
			//потом тут будем смотреть компанию
		}
		String basePath = companyKey + "/ai-models";
		SessionNode current = session.getActive();
		//AppHandler.toConsole("session.getActive() " + current.toFmtJson());
		String selectedKey = null;
		if (current != null && current.Exists("ai-model")) {
			selectedKey = current.getStr("ai-model");
		}
		if (selectedKey == null) {
			for (String modelKey : AppConfig.childrenKeys(basePath)) {
				if (AppConfig.getBool(basePath + "/" + modelKey + "/default")) {
					selectedKey = modelKey;
				}
			}
		}
		if (selectedKey != null) {
			String modelPath = basePath + "/" + selectedKey;
			model.data.set("ai-model", selectedKey);
			model.data.set("key-model", modelPath);
			String providerPath = companyKey + "/ai-providers/" + AppConfig.getStr(modelPath + "/provider");
			model.data.set("key-provider", providerPath);
			if (AppConfig.Exists(modelPath + "/model")) {
				model.data.set("model", AppConfig.getStr(modelPath + "/model"));
			} else {
				model.data.set("model", selectedKey);
			}
			if (AppConfig.Exists(modelPath + "/title")) {
				model.data.set("model-title", AppConfig.getStr(modelPath + "/title"));
			} else {
				model.data.set("model-title", selectedKey);
			}
			model.data.set("provider", AppConfig.get(providerPath));
		}
		//AppHandler.toConsole("aiModel: "+model.data.toFmtJson());
		return model;
	}

	public Object config(String path) {
		Object value = this.data.get(path);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		if (value instanceof String) {
			return String.valueOf(value);
		}
		return value;
	}

	public i_RequestAI getAPI() {
		//AppHandler.toConsole("getAPI(): " + this.data.toFmtJson());
		String providerType = (String) this.config("provider/transport");
		if (providerType == null) {
			providerType = "";
		}
		switch (providerType.toLowerCase()) {
			case "openai":
				return OpenAIProvider.Construct(this);
			case "gemini":
				return GeminiProvider.Construct(this);
			case "gemini_v1":
				return GeminiProviderV1.Construct(this);
			default:
				return DebugEmulator.Construct(this);
		}
	}
  public String getApiKey(){
    if (chosenApiKey != null) {
      return chosenApiKey;
    }
    Object apiKeyObj = this.data.get("provider/apikey");
    if (apiKeyObj instanceof String) {
      chosenApiKey = (String) apiKeyObj;
    } else if (apiKeyObj instanceof java.util.List) {
      java.util.List<?> list = (java.util.List<?>) apiKeyObj;
      if (!list.isEmpty()) {
        chosenApiKey = String.valueOf(list.get(0));
      }
    }
    if (chosenApiKey == null) {
      chosenApiKey = "";
    }
    return chosenApiKey;
  }
  public String getURL(){
    String webhook = (String) this.config("provider/webhook");
    if (webhook == null) {
      return "";
    }
    String modelName = (String) this.config("model");
    if (modelName == null) {
      modelName = "";
    }
    String apiKey = this.getApiKey();
    return webhook.replace("{$model}", modelName).replace("{$apikey}", apiKey);
  }
}
