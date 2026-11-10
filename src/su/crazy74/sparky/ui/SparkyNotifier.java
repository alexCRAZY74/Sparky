package su.crazy74.sparky.ui;

import su.crazy74.sparky.core.LanguageHandler;
import su.crazy74.sparky.interfaces.i_Session;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SparkyNotifier {

	private static TrayIcon trayIcon;
	
	public static void ChatDone(i_Session chat) {
		String title = LanguageHandler.getDefault().getStr("plugin/name", "Sparky");
		if (chat != null && chat.info() != null && chat.info().Exists("tab-title")) {
			title = chat.info().getStr("tab-title");
		}
		notify(title, LanguageHandler.getDefault().getStr("notify/chat_done", "Ответ готов"), TrayIcon.MessageType.INFO);
	}

	public static void ChatError(i_Session chat) {
		String title = LanguageHandler.getDefault().getStr("plugin/name", "Sparky");
		if (chat != null && chat.info() != null && chat.info().Exists("tab-title")) {
			title = chat.info().getStr("tab-title");
		}
		notify(title, LanguageHandler.getDefault().getStr("notify/chat_error", "Ошибка выполнения"), TrayIcon.MessageType.ERROR);
	}

	public static void notify(String title, String message, TrayIcon.MessageType type) {
		if (!SystemTray.isSupported()) {
			return;
		}

		try {
			SystemTray tray = SystemTray.getSystemTray();

			if (trayIcon == null) {
				// Заглушка-иконка 16x16 для трея, если ещё не создана
				Image image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = (Graphics2D) image.getGraphics();
				g.setColor(new Color(200, 60, 40)); // Фирменный акцент
				g.fillRect(0, 0, 16, 16);
				g.dispose();

				trayIcon = new TrayIcon(image, LanguageHandler.getDefault().getStr("notify/tray_title", "Искра (Sparky)"));
				trayIcon.setImageAutoSize(true);
				tray.add(trayIcon);
			}

			try {
				Toolkit.getDefaultToolkit().beep();
			} catch (Throwable ignored) {
			}
			trayIcon.displayMessage(title, message, type);
		} catch (Exception e) {
			su.crazy74.sparky.core.AppHandler.toConsole("SparkyNotifier error: " + e.getMessage());
		}
	}
}
