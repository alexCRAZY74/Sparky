**English** | [Русский](README_RU.md)

---

# 💠 Sparky (Iskra)

**A planning tool and engineering AI assistant for NetBeans**

---

## 📖 What It Is

Sparky is a planning and development tool inside Apache NetBeans, designed for both solo and team work. The project repository is available at [GitHub](https://github.com/alexCRAZY74/Sparky).

An interactive mind map of the project is stored directly in the repository as Markdown. The AI assistant relies on this map: it sees the modules, tasks and their statuses, and solves each task in the context of the whole system. The team records its development rules in trainer files, and the assistant works within those rules.

Installation and setup are described in the [Quick Start](QUICKSTART.md).

---

## 🎯 Core Approach

* **Project structure instead of fragment search.** There is no vector index or code chunking. Through `project_map.md` the model sees the module tree, kanban statuses and tasks, and requests the files it needs through `needs`, receiving them in full.
* **Team experience in trainer files.** Lead developers record standards, templates and error-handling rules. The assistant requests the relevant trainer for the task at hand, so less redundant boilerplate is produced and code quality is levelled across the team.
* **Safe changes.** The model works through targeted patches and never rewrites whole files. A backup is created before any change.
* **Stored in Git, no service lock-in.** The map, tasks, settings and backups are plain Markdown and JSON files. There is no subscription for the plugin: you pay only for the use of the API you choose.

---

## ✨ Key Features

### 🗺️ Planning with Mind Maps

* Visualisation of modules, architecture, ideas and tasks (jsMind) right in the IDE window.
* Kanban statuses and time intervals on task nodes.
* Heavy task data (patches, changed files, AI context) is loaded on demand when a node is selected.

### 👥 Teamwork Through Git

* The map is stored in the repository (`.sparky/project_map.md`) as a flat hierarchical Markdown list.
* Plans and tasks are synchronised with ordinary commits and branches. The history of the plan is the history of the repository.
* The assistant's notes on the project live in `.sparky/` and can be synchronised together with the map, so the accumulated context is available to the whole team.

### 🧠 An Assistant That Knows the Whole Project

* **Iterative `needs` protocol.** The assistant itself states which files, IDE resources or trainer files it needs, receives them and continues working.
* **Working with the map.** The assistant edits `project_map.md` with the same targeted patches as code: it updates a task status or drafts an initial project plan.
* **Long-term memory.** The assistant reads and updates two sets of notes on its own. The user profile is kept in the NetBeans user directory; project notes are kept in the project's `.sparky/` directory and can be synchronised through Git. Thanks to the profile, the assistant adapts to the user: it takes into account communication style and preferences, so the same remarks need repeating less often. These are ordinary files, and the user can edit them by hand.
* **Multiple providers.** Gemini and OpenAI-compatible APIs are supported. Requests go directly to the provider over REST; the address and model are set in the configuration. The plugin has no server of its own.

### 🛡️ Shadow Backups

* A copy of a file is created automatically **before** its content is passed to the AI or changed, and can also be created manually from the context menu.
* The directory structure is preserved and versions are distinguished by timestamp: `.file_name.extension.yyyy-MM-dd_HH-mm-ss.bak`. The `.backup/` directory is excluded from Git.

---

## 🎓 Trainer Files

A trainer file is a text instruction for the AI, written by a person who knows the project and its architecture well.

1. **Creation.** The stronger programmers on the team describe the rules: how to write services, how to handle errors, how to structure modules. The leads' knowledge stays in the repository and works for the whole team.
2. **Index.** Available trainers are listed in `trainers.json`. By default the assistant sees this list with every request. The behaviour is configurable.
3. **Use.** The assistant requests the relevant trainer through `needs` when the task calls for it. Leads are responsible for writing this list so that the AI knows what to request and when.

---

## 🏗 Architecture

The system is built on loose coupling: components interact through interfaces and the universal `Storage` data structure.

| Component | Purpose |
| --- | --- |
| `BrainHandler` | Runs the iterative dialogue with the AI and handles `needs` requests |
| `ProjectHandler` | Manages project context and shadow backups |
| `aiModelHandler` | Dispatcher of AI models and providers (Gemini, OpenAI-compatible APIs) |
| `StoredNotes` | Long-term memory with change history |
| `Storage` | Hierarchical storage addressed by paths (`key/subkey/0`) |
| `JSBridge` | Two-way Java ↔ WebView bridge |

### Iterative Interaction Protocol

```text
User ──(request)──> BrainHandler ──(context)──> AI model
                          │                         │
                          │<──────(needs list)──────┘
                          │
                    ProjectHandler
                          │
              backup ──> payload ──> AI model
                                         │
                      apply patch <─(response)─┘
```

Progress is shown in the interface through `stages` (`wait`, `process`, `done`). The `profile_update` block in a response updates the long-term memory.

---

## 📁 Data Storage

In the project directory:

```text
PROJECT.md               # Architecture description of the project
.sparky/
├── project_map.md       # Mind map and task structure (Markdown)
├── trainers.json        # Index of trainer files
└── .backup/             # Shadow copies of sources (*.bak, in .gitignore)
```

In the NetBeans user directory:

```text
config/crazy_sparky/
├── user_profile.json    # User profile (long-term memory)
└── settings.json        # Plugin settings
```

---

## 🧰 Technology Stack

* **Language and platform:** Java 21, JavaFX 21, NetBeans
* **Interface:** WebView (JavaFX), jQuery, SASS
* **Parsers:** Flexmark (Markdown), GSON (JSON)
* **Visualisation:** jsMind
* **Integration:** `executeScript` (Java → JS) and `JSBridge` (JS → Java)

---

## 👤 Author

AlexCRAZY74 (Almaty, Kazakhstan) — [alexcrazy74.github.io/Me](https://alexcrazy74.github.io/Me/)
