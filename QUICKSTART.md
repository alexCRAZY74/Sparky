# Quick Start: Sparky for Apache NetBeans

Sparky is an engineering assistant and task-planning tool built into the Apache NetBeans IDE. The project repository is available at [GitHub](https://github.com/alexCRAZY74/Sparky).

---

## 1. Environment Requirements and JavaFX

The plugin interface (the embedded browser for the mind map and the chat) requires a Java runtime with full **JavaFX (OpenJFX)** support.

* **Recommended build:** **BellSoft Liberica JDK 21 (Full package)**. Standard OpenJDK or Oracle JDK builds without JavaFX cause a `ClassNotFoundException: javafx.*` error when the plugin windows are opened.
* **IDE:** Apache NetBeans 21 or newer.
* **Configuration:** Make sure NetBeans is actually running on this JDK (the `jdkhome` parameter in `etc/netbeans.conf`).

---

## 2. Installing the Plugin

1. Download the plugin's binary package `*.nbm` from the [Releases](https://github.com/alexCRAZY74/Sparky/releases) section.
2. In the NetBeans main menu open: **Tools → Plugins → Downloaded** tab.
3. Click **Add Plugins...**, select the downloaded `.nbm` file and confirm the installation (**Install**).
4. Restart the IDE if prompted.

---

## 3. Configuring the AI Provider

Open the configuration file from the main menu: **Window → Sparky → Settings**. If the file does not exist, the plugin creates it automatically at `config/crazy_sparky/settings.json`.

Minimal configuration to get started:

```json
{
  "language": "en",
  "defaultCompany": {
    "ai-models": {
      "gemini-flash-latest": {
        "default": true,
        "title": "Gemini 2.5 Flash",
        "provider": "google-gemini",
        "model": "gemini-2.5-flash"
      }
    },
    "ai-providers": {
      "google-gemini": {
        "transport": "gemini",
        "webhook": "https://generativelanguage.googleapis.com/v1beta/models/{$model}:generateContent?key={$apikey}",
        "apikey": "YOUR_API_KEY"
      }
    }
  },
  "Session": {
    "AlwaysSend": {
      "OpenTabs": false,
      "ProjectInfo": true,
      "ProjectFileTree": false
    }
  }
}
```

To work through an OpenAI-compatible API, set `"transport": "openai"`, the corresponding webhook address and the authorisation token.

---

## 4. First Run and Usage

1. Open a working project in NetBeans.
2. In the project's context menu choose **Sparky: Chat** (or open the window via **Window → Sparky → Chat**).
3. All rules for architecture and configuration are built into the plugin. Simply state the task in free form: Sparky will not only adapt the project files but also configure the plugin's own parameters, pull in context through the `needs` protocol and produce targeted patches.
