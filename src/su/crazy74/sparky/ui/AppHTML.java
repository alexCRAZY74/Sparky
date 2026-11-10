package su.crazy74.sparky.ui;

import su.crazy74.sparky.core.AppHandler;
import su.crazy74.sparky.core.AppConfig;
import java.io.File;

public class AppHTML {

	public String fileUrl;
	public String baseUrl;
	public String content;

	public AppHTML(String baseName) {
		try {
			String filenameBase = baseName + ".html";
			String filenameLive = baseName + "_live.html";

			/*java.io.File chatFile = org.openide.modules.InstalledFileLocator.getDefault()
							.locate("modules/ext/html/" + filenameBase, "crazy.module.german", false);*/
			File chatFile = org.openide.modules.InstalledFileLocator.getDefault()
							.locate("html/" + filenameBase, "crazy.module.german", false);
			String errMsg;
			if (chatFile == null || !chatFile.exists()) {
				errMsg = "html/" + filenameBase + " not exists";
				System.err.println(errMsg);
				AppHandler.toConsole(errMsg);

				// Пробуем альтернативный стандартный путь кластера NetBeans:
				chatFile = org.openide.modules.InstalledFileLocator.getDefault()
								.locate("modules/ext/html/" + filenameBase, "crazy.module.german", false);
			}
			if (chatFile == null || !chatFile.exists()) {
				errMsg = filenameBase + " not exists";
				System.err.println(errMsg);
				AppHandler.toConsole(errMsg);
				return;
			}
			AppHandler.toConsole("html-file: " + chatFile.toString());
			java.nio.file.Path tempPath = java.nio.file.Paths.get(chatFile.getParent(), filenameLive);
			content = new String(java.nio.file.Files.readAllBytes(chatFile.toPath()), "UTF-8");

			baseUrl = chatFile.getParentFile().toURI().toURL().toExternalForm();
			content = content.replace("${baseUrl}", baseUrl);

			content = applyTheme(content);
			final String finalContent = content;
			java.nio.file.Files.write(tempPath, finalContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			fileUrl = tempPath.toUri().toURL().toExternalForm();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String toHex(java.awt.Color color) {
		if (color == null) {
			return "#000000";
		}
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	private String applyTheme(String html) {
		return html.replace("{{--uiBg--}}", AppConfig.getStr("NetBeans/UI/Color/Panel/background"))
						.replace("{{--uiFg--}}", AppConfig.getStr("NetBeans/UI/Color/Label/foreground"))
						.replace("{{--uiPanel--}}", AppConfig.getStr("NetBeans/UI/Color/Explorer/background"))
						.replace("{{--uiBorder--}}", AppConfig.getStr("NetBeans/UI/Color/Component/border"))
						.replace("{{--uiActive--}}", AppConfig.getStr("NetBeans/UI/Color/List/selectionBackground"))
						.replace("{{--editBg--}}", AppConfig.getStr("NetBeans/Editor/Color/background"))
						.replace("{{--editFg--}}", AppConfig.getStr("NetBeans/Editor/Color/foreground"))
						.replace("{{--editSelection--}}", AppConfig.getStr("NetBeans/Editor/Color/selectionBackground"))
						.replace("{{--uiFontFamily--}}", "\"" + AppConfig.getStr("NetBeans/UI/Font/family") + "\", sans-serif")
						.replace("{{--uiFontSize--}}", AppConfig.getStr("NetBeans/UI/Font/size"))
						.replace("{{--editFontFamily--}}", "\"" + AppConfig.getStr("NetBeans/Editor/Font/family") + "\", monospace")
						.replace("{{--editFontSize--}}", AppConfig.getStr("NetBeans/Editor/Font/size"))
						// Вот теперь всё сходится:
						.replace("{{--syntaxKeyword--}}", AppConfig.getStr("NetBeans/Editor/Syntax/Color/keyword"))
						.replace("{{--syntaxString--}}", AppConfig.getStr("NetBeans/Editor/Syntax/Color/string"))
						.replace("{{--syntaxComment--}}", AppConfig.getStr("NetBeans/Editor/Syntax/Color/comment"))
						.replace("{{--syntaxNumber--}}", AppConfig.getStr("NetBeans/Editor/Syntax/Color/number"))
						.replace("{{--syntaxType--}}", AppConfig.getStr("NetBeans/Editor/Syntax/Color/type"));
	}
}
