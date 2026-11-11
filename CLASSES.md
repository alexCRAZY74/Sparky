# 📘 CLASSES.md — Core Structure & API Reference

---

## 📦 `su.crazy74.sparky.interfaces`

### `i_Session`
Контракт для управления жизненным циклом сессии чата. Связывает GUID окна с конкретным проектом и обеспечивает канал для передачи ответов от `BrainHandler` обратно в интерфейс.
* `ProjectHandler getProjectHandler()`: Возвращает привязанный `ProjectHandler` — то есть, «чей» это чат.
* `void ActivateNode(SessionNode node)`: Регистрирует новый узел и переводит UI в состояние ожидания.
* `void UpdateNode(SessionNode node)`: Принудительно обновляет данные узла в UI без смены текущего контекста.
* `SessionNode getActive()`: Возвращает узел, который прямо сейчас находится в обработке.
* `List<SessionNode> getHistory()`: Возвращает весь список узлов за текущую сессию.
* `Storage getProjectInformation()`: Возвращает структуру с описанием проекта.
* `public i_GermanProject getProject()`: Возвращает объект проекта, к которому привязана сессия.
* `boolean isInterrupted()`: Проверяет флаг прерывания сессии.
* `Storage config()`: Возвращает конфигурационный контейнер сессии.
* `Storage info()`: Возвращает метаданные сессии (заголовок, имя вкладки, проект).
* `void setTitle(String newTitle)`: Устанавливает новый заголовок сессии и обновляет имя вкладки в UI.

---

### `i_GermanProject`
Базовый интерфейс для представления проекта в экосистеме German. Гарантирует наличие минимально необходимых данных о проекте.
* `public String getDisplayName()`: Возвращает человекочитаемое название проекта.
* `public Storage getInformation()`: Возвращает структуру с описанием проекта.
* `public Storage config()`: Возвращает конфигурационный контейнер проекта.
* `public FileObject getFolder()`: Возвращает папку проекта.

---

### `i_RequestAI`
Универсальный контракт для любого ИИ-провайдера (Gemini, OpenAI, эмуляторы). Стандартизирует способ отправки запросов, чтобы `BrainHandler` мог работать с разными «мозгами» не меняя ни строчки кода.
* `public static i_RequestAI Construct(aiModelHandler model)`: Статический фабричный метод для создания экземпляра провайдера на основе переданного обработчика модели.
* `SessionNode Request(List<String> data)`: Отправляет контекст (историю + промпт) и возвращает результат в формате `SessionNode`.

---

## 📦 `su.crazy74.sparky.core`

### `Storage`
Универсальный контейнер данных на основе абстрактного `Object data`. Хранит произвольно вложенные структуры (объекты `Map` и массивы `List`) и позволяет работать с ними через строковые пути вида `"key/subkey/index"`. Является базовым классом для `SessionNode`.

#### Конструкторы
* `public Storage()`: Создаёт пустое хранилище с дефолтной `HashMap`.
* `public Storage(Object data)`: Оборачивает произвольный объект (Map, List и др.). Если `data == null` — инициализирует пустую `HashMap`.

#### Статические фабрики
* `public static Storage fromJson(String json)`: Парсит JSON-строку в абстрактный `Object` и возвращает новый экземпляр `Storage`. При ошибке разбора возвращает пустой `Storage`.

#### Сериализация
* `public String toJson()`: Возвращает компактный JSON без переносов строк. Сериализует напрямую `data` (поддерживает плоские массивы в корне).
* `public String toFmtJson()`: Возвращает Pretty Print JSON с отступами.

#### Чтение
* `public Object get(String path)`: Возвращает значение по пути. Поддерживает числовые индексы для массивов (`"items/0/title"`). Если `path` — `null` или `""` — возвращает корневую Map целиком. Возвращает `null`, если путь не существует.
* `public Map<String, Object> get()`: Возвращает хранилище как `Map` (если внутри лежит `Map`, иначе пустую `HashMap` для обратной совместимости).
* `public Object getRaw()`: Возвращает сырой внутренний объект `data` без приведения типов.
* `public String getStr(String path)`: Возвращает значение как `String` через `String.valueOf()`. Возвращает `null` при отсутствии пути.
* `public boolean getBool(String path)`: Понимает `Boolean` и строки `"true"` / `"false"`. Возвращает `false` при отсутствии ключа или несовместимом типе.
* `public int getInt(String path)`: Понимает `Number` и строки-числа. Возвращает `0` при отсутствии ключа или несовместимом типе.
* `public float getFloat(String path)`: Понимает `Number` и строки-числа. Возвращает `0.0f` при отсутствии или несовместимости.
* `public List<Object> getList(String path)`: Возвращает список по пути, если это JSON-массив, иначе `null`.
* `public List<String> getStrList(String path)`: Возвращает список строковых значений по пути.
* `public List<String> childrenKeys(String path)`: Возвращает список ключей объекта (или строковых индексов массива) по указанному пути.
* `public List<String> childrenKeys()`: Возвращает список ключей корневого уровня (ключи Map или индексы List).

#### Запись
* `public void set(String path, Object value)`: Записывает значение по пути. Промежуточные узлы, которых не существует, создаёт автоматически как `Map` или `List` (если следующий ключ — число или `[]`). Поддерживает оператор автодобавления в конец списка через `[]` (например, `"keylist/[]"`).
* `public void set(Object newdata)`: Полностью замещает внутренний объект `data`.
* `public void set(Map<String, Object> newdata)`: Замещает хранилище переданной Map.
* `public void set(List<Object> newdata)`: Замещает хранилище переданным списком.

#### Слияние (Merge)
* `public void Merge(Map<String, Object> target)`: Выполняет глубокое рекурсивное слияние (deep merge) полей из `target` в корневой объект `data` (если он является Map), не удаляя остальные поля.
* `public void Merge(String path, Map<String, Object> target)`: Выполняет глубокое рекурсивное слияние полей из `target` во вложенную Map по указанному пути. Если узел отсутствует или не является Map, автоматически создаёт на его месте новую Map.
* `public void Merge(Storage target)`: Слияние данных из другого хранилища `Storage` в текущее.
* `public void Merge(String path, Storage target)`: Слияние данных из другого хранилища `Storage` во вложенный узел по указанному пути.

#### Удаление
* `public void Remove(String path)`: Удаляет ключ из объекта или элемент массива по числовому индексу. Ничего не делает, если `path` — `null` или `""`.

#### Проверки
* `public boolean Exists(String path)`: Возвращает `true`, если по указанному пути есть ненулевое значение.
* `public Boolean isEmpty()`: Возвращает `true`, если внутреннее хранилище абсолютно пусто.
* `public String typeOf(String path)`: Возвращает тип значения по пути: `"object"` / `"array"` / `"string"` / `"number"` / `"boolean"` / `"null"` / `"<classname>"` для прочих типов.

#### Виртуальные узлы (StorageNode)
* `public StorageNode getNode(String path)`: Возвращает виртуальный узел `StorageNode` для безопасной древовидной навигации. Если узел по пути отсутствует или является примитивом, бережно создаёт на его месте новую Map.
* Вложенный класс `StorageNode` (легковесная объектная обёртка):
    * `public Object getValue()` / `setValue(Object val)`: Чтение и запись атомарного значения узла через служебное поле `_value`.
    * `public Map<String, String> getMeta()`: Возвращает карту метаданных узла через служебное поле `_meta` (например, для Канбан-статусов).
    * `public StorageNode child(String key)`: Переходит к вложенному дочернему узлу по имени ключа.
    * `public Set<String> getChildrenKeys()`: Возвращает список ключей текущего уровня, исключая служебные `_value` и `_meta`.
    * `public Map<String, Object> asMap()`: Возвращает сырой экспорт структуры.

#### Работа с Markdown (Парсинг и генерация)
* `public static Storage fromMarkdown(String mdText)`: Парсит входящий Markdown-текст на основе заголовков разного уровня и HTML-комментариев метаданных (`<!-- key=value -->`), воссоздавая вложенный `Storage`.
* `public String toMarkdown()`: Сериализует объектную древовидную модель `Storage` обратно в структурированный Markdown-файл.

#### 🚀 Backlog
* **Smart Path:** Поддержка синтаксиса `key/[0]` и авто-добавления в конец через `key/[]`.

---

### `AppHandler`
Глобальный менеджер настроек и системного вывода. Управляет файлом `settings.json` в папке конфигурации NetBeans и выводом в именованную консоль «German Console».
* `public static AppHandler getDefault()`: Возвращает единственный экземпляр (Singleton).
* `public void loadConfig()`: Читает настройки из `config/crazy_sparky/settings.json`.
* `public void saveConfig()`: Записывает текущие настройки обратно в файл.
* `public static void toConsole(String text)`: Печатает сообщение в Output-окно IDE (вкладка «Sparky Console»).
* `public static FileObject getProfileFile()`: Возвращает указатель на файл профиля пользователя (`config/crazy_sparky/user_profile.json`).

---


### `BrainHandler`

Основной координатор логики ИИ. Управляет циклом рекурсивных запросов к API, формирует многослойный контекст (`RequestPart`) и обрабатывает дозапрос ресурсов (`needs`).

* `public static void RequestAI(i_Session session)`: Главная точка входа. Проверяет флаг прерывания, собирает JSON-пакеты (Профиль, Проект, История, Открытые вкладки) и отправляет их ИИ-провайдеру. При необходимости уходит на новый круг рекурсии.
* `protected static boolean AddFilePart(...)`: Загружает содержимое файла или разность изменений (`/:changes`) и добавляет блок в контекст.
* `protected static boolean AddInstructionPart(...)`: Загружает системную инструкцию из ресурсов плагина (`system://@prompts/...`).
* `protected static void CheckTrainers(...)`: Считывает реестр файлов-тренеров (`trainers.json`) и автоматически прикрепляет обязательные инструкции.
* `protected static void CheckNeeds(...)`: Обрабатывает массив запросов ресурсов (`needs`) из текущего этапа и дозагружает требуемые данные.
* `static class RequestPart`: Структура для форматирования текстовых блоков контекста в Multipart-подобный вид с заголовками (`Content-Type`, `Content-Title`, `Content-Location`).
* `static class RequestPartJSON`: Специализированная обёртка над `RequestPart` для передачи данных в формате `application/json`.
* `static class RequestPartMarkdown`: Специализированная обёртка над `RequestPart` для передачи данных в формате `application/markdown`.

---

### `StoredNotes`
Компонент долгосрочной контекстной памяти. Реализует хранение данных в формате ключ-значение на базе `Storage` с автоматическим ведением истории изменений. Содержит статические методы-фасады для быстрой работы с файлами без ручного создания инстансов.

*Статические методы (Быстрый доступ):*
* `public static void Update(FileObject source, Storage newData)`: Быстрое обновление данных. Сам создаёт инстанс, загружает файл, накатывает изменения с логированием истории, и сохраняет на диск.
* `public static String toFmtJson(FileObject source)`: Быстрое чтение. Загружает указанный файл и возвращает его содержимое в виде красивой JSON-строки.

*Методы инстанса:*
* `public StoredNotes(FileObject fo)`: Конструктор с привязкой к файлу.
* `public StoredNotes Load()`: Загружает JSON-данные из файла в память.
* `public StoredNotes Save()`: Записывает текущее состояние памяти на диск в UTF-8.
* `public StoredNotes Update(Storage updates)`: Накатывает изменения на внутренний `Storage` с архивацией старых значений в `changes/`.
* `public Storage getData()`: Возвращает внутренний контейнер `Storage`.

---

### `ProjectHandler`

Обертка над NetBeans `Project`. Отвечает за изоляцию контекста конкретного проекта, фильтрацию временных файлов и генерацию карты репозитория.

* `public static ProjectHandler BySession(i_Session session)`: Фабричный метод получения обработчика из текущей сессии чата.
* `public FileObject getPluginDir()`: Возвращает или создает FileObject скрытой директории `.sparky` в корне проекта.
* `public FileObject getConfigFile()`: Возвращает конфигурационный файл `config.json` проекта.
* `public FileObject getTrainersFile()`: Возвращает файл списка файлов-тренеров `trainers.json`.
* `public FileObject getProfileFile()`: Возвращает файл профиля проекта `profile.json`.
* `public String ToURI(FileObject fo)`: Преобразует `FileObject` в относительный URI-путь вида `file://@project/...`.
* `public Storage getInformation()`: Возвращает объект `Storage` с базовой мета-информацией (имя, отображаемое имя, абсолютный путь к папке).
* `public String FileTree()`: Точка входа для построения текстового дерева проекта.
* `public String FileTree(File folder, String indent, boolean isLast)`: Рекурсивный строитель дерева. Автоматически игнорирует скрытые файлы, а также директории, прописанные в черном списке `treeIgnore` (`nbproject/`, `build/`, `vendor/`, `node_modules/` и др.).

---

### `MindManager`
Центральный компонент управления ментальной картой проекта. Координирует чтение, изменение и сохранение структуры карты, разделяя данные на три слоя: саму карту (`project_map.md`), её конфигурацию (`map_config.json`) и интерактивные состояния веток (`nap_*.json`).

#### Методы
* `public static MindManager Project(i_GermanProject project)`: Фабричный метод инициализации менеджера для указанного проекта.
* `public Storage commandGet(Storage form)`: Диспетчер GET-запросов от фронтенда (команда `"map-tree"`).
* `public Storage commandSet(Storage form)`: Диспетчер SET-запросов от фронтенда (команда `"map-tree"`).
* `public Storage saveNodeArray(Storage form)`: Принимает плоский массив узлов от jsMind, извлекает состояния отображения (свернут/развернут) в `states()`, настройки корня в `config()`, а чистую структуру сохраняет в `nodes()`.
* `public Storage getNodeArray()`: Собирает плоский массив узлов для jsMind, обогащая структуру из `nodes()` метаданными из `config()` и состояниями из `states()`.
* `public DataLayer states()`: Возвращает слой состояний веток. Хранится в `<userdir>/config/crazy_sparky/nap_<rootGuid>.json`.
* `public DataLayer config()`: Возвращает слой конфигурации карты (настройки Канбан-колонок). Хранится в `<project_root>/.sparky/map_config.json`.
* `public DataLayer nodes()`: Возвращает слой структуры карты. Хранится в `<project_root>/.sparky/project_map.md`.

#### Вложенный класс `DataLayer`
Специализированный наследник `Storage`, привязанный к физическому файлу. Внутренне оперирует плоским списком узлов ментальной карты. Отвечает за прозрачную загрузку и сохранение данных в форматах JSON или Markdown.
* `public void Load()`: Загружает контент с диска. Автоматически определяет формат по расширению файла (`.md` или `.json`).
* `public void Save()`: Записывает текущее состояние памяти на диск в UTF-8, сериализуя их в Markdown или JSON.
* `public String toMarkdown()`: Транслирует плоский список узлов в древовидный Markdown с сохранением метаданных в HTML-комментариях.
* `public static Storage fromMarkdown(String mdText)`: Парсит древовидный Markdown с HTML-комментариями метаданных, включая многострочные, обратно в плоский список узлов `Storage` с автоматическим восстановлением связей `parentid`.

---

### `ideHandler`

Информатор о состоянии среды разработки. Связывает контекст ИИ с действиями пользователя внутри NetBeans.

* `public static List<String> getOpenedTabsList()`: Опрашивает реестр `TopComponent` среды и собирает URI всех открытых в данный момент файлов в редакторе.
* `public static String getOpenedTabs()`: Форматирует список открытых вкладок в JSON-строку для отправки в ИИ.
* `public static List<Map<String, Object>> getOpenedProjects()`: Возвращает список всех открытых в IDE проектов.
* `public static String getOpenedProjectsJson()`: Форматирует список открытых проектов в JSON.

---

### `AppConfig`
Центральный узел управления конфигурацией с двухслойной логикой чтения. Все методы — **статические**, экземпляр класса не создаётся. При первом обращении к любому методу **автоматически** загружает `UserData` с диска и инициализирует `SystemData` из окружения IDE.

```
AppConfig.get("some/path")
    ├── UserData (settings.json)   ← приоритет
    └── SystemData (UIManager)     ← fallback
```

#### Чтение
* `public static Storage getAiModelsList(i_Session session)`: Возвращает список доступных ИИ-моделей на основе пользовательских настроек `UserData` (ветка `defaultCompany/ai-models`).
* `public static boolean Exists(String path)`: Проверяет существование ключа в `UserData` или `SystemData`.
* `public static List<String> childrenKeys(String path)`: Возвращает список дочерних ключей по указанному пути из `UserData`.
* `public static List<String> childrenKeys()`: Возвращает список корневых ключей из `UserData`.
* `public static Object get(String path)`: Основной метод. Ищет в `UserData`, при отсутствии — в `SystemData`.
* `public static String getStr(String path)`: Обёртка над `get()`. Возвращает `null` при отсутствии ключа.
* `public static boolean getBool(String path)`: Обёртка над `get()`. Возвращает `false` при отсутствии ключа.
* `public static int getInt(String path)`: Обёртка над `get()`. Возвращает `0` при отсутствии ключа.

#### Работа с файлом настроек
* `public static File getConfigDir()`: Возвращает `File` для `<userdir>/config/crazy_sparky/`. Если папка не существует — **создаёт** её.
* `public static FileObject getConfigFile()`: Возвращает `FileObject` для `<userdir>/config/crazy_sparky/settings.json`. Если файл или папка не существуют — **создаёт** их с содержимым `{}`.
* `public static void LoadFromFile()`: Читает `settings.json` в `UserData`. При ошибке инициализирует пустой `UserData`.
* `public static void SaveToFile()`: Сохраняет `UserData` в файл в формате Pretty Print (UTF-8).
* `public static void UpdateFromSystem()`: Обновляет `SystemData` из текущего `UIManager`. Вызывать при смене темы NetBeans.

> ⚠️ Публичного метода `set()` нет. Запись идёт напрямую через `UserData` (доступно внутри пакета `su.crazy74.sparky.core`) с последующим вызовом `SaveToFile()`.

#### Системные ключи (`SystemData`, только чтение)

| Путь | Описание |
|---|---|
| `NetBeans/UI/Color/Panel/background` | Фон панелей |
| `NetBeans/UI/Color/Label/foreground` | Цвет текста |
| `NetBeans/UI/Color/Explorer/background` | Фон дерева проектов |
| `NetBeans/UI/Color/Component/border` | Цвет рамок |
| `NetBeans/UI/Color/List/selectionBackground` | Цвет выделения |
| `NetBeans/Editor/Color/background` | Фон редактора |
| `NetBeans/Editor/Color/foreground` | Цвет текста редактора |
| `NetBeans/Editor/Color/selectionBackground` | Цвет выделения редактора |
| `NetBeans/UI/Font/family` | Семейство UI-шрифта |
| `NetBeans/UI/Font/size` | Размер UI-шрифта |
| `NetBeans/Editor/Font/family` | Семейство шрифта редактора (`"Monospaced"`) |
| `NetBeans/Editor/Font/size` | Размер шрифта редактора |
| `NetBeans/Editor/Syntax/Color/keyword` | Цвет ключевых слов |
| `NetBeans/Editor/Syntax/Color/string` | Цвет строк |
| `NetBeans/Editor/Syntax/Color/comment` | Цвет комментариев |
| `NetBeans/Editor/Syntax/Color/number` | Цвет чисел |
| `NetBeans/Editor/Syntax/Color/type` | Цвет типов |
| `Session/AlwaysSend/OpenTabs` | Флаг принудительной отправки открытых вкладок (`true`/`false`) |
| `Session/AlwaysSend/ProjectInfo` | Флаг принудительной отправки метаданных проекта (`true`/`false`) |
| `Session/AlwaysSend/ProjectFileTree` | Флаг принудительной отправки дерева файлов проекта (`true`/`false`) |

---

### `SessionNode`
Специализированный контейнер для одного цикла диалога (Вопрос–Ответ). Пустой наследник `Storage` — новой логики не добавляет, просто делает код наглядным: сразу видно, что перед тобой сессионные данные, а не общие настройки.

**Наследование:** `SessionNode extends Storage`

**Основные ключи:** GUID запроса, текст вопроса, текст ответа, список статусов (`stages`).

---

### `BackupManager`
Компонент автоматического и ручного резервного копирования исходного кода. Отвечает за изоляцию бэкапов внутри скрытой директории `.german/.backup/` текущего проекта, воссоздает оригинальную структуру подпапок и обеспечивает версионность файлов с использованием человекочитаемых меток времени.

#### Конструкторы
* `public BackupManager(ProjectHandler project)`: Инициализирует менеджер на основе готового обработчика проекта.
* `public BackupManager(Project project)`: Конструктор-фасад. Автоматически оборачивает стандартный нетбинсовский `Project` в `ProjectHandler`.

#### Статические фабрики
* `public static BackupManager inProject(ProjectHandler project)`: Фабричный метод для быстрого создания инстанса (fluent API).
* `public static BackupManager inProject(Project project)`: Фабричный метод, принимающий базовый `Project`.

#### Методы
* `public FileObject getFolder()`: Возвращает или создает целевую директорию для хранения резервных копий (`<project_root>/.sparky/.backup/`). Возвращает `null`, если проект недоступен.
* `public void Backup(FileObject sourceFile)`: Запускает процесс резервного копирования для указанного файла:
    1. Вычисляет относительный путь файла от корня проекта с помощью `FileUtil.getRelativePath`.
    2. Зеркально воссоздает структуру родительских папок внутри `.backup/` через `FileUtil.createFolder`.
    3. Генерирует уникальное имя файла с временной меткой в формате `yyyy-MM-dd_HH-mm-ss` (например, `Main.java.2026-05-22_18-35-02.bak`).
    4. Выполняет атомарное копирование средствами NetBeans FileSystem API и логирует результат в «Sparky Console».

---

### `FileHandler`
Компонент изолированной работы с целевыми файлами и их проектными контекстами в экосистеме German. Выступает в роли защитного конвейера: берёт на себя всю грязную логику парсинга URI, автоматического поиска проекта-владельца через среду NetBeans, принудительного резервного копирования и высокопроизводительного наката патчей.

#### Поля
* `protected ProjectHandler hProject`: Обработчик проекта, к которому железно привязан данный файл.
* `protected FileObject sourceFile`: Ссылка на физический файл в файловой системе NetBeans.
* `protected String errorMessage`: Строковое описание ошибки валидации (если инициализация или проверки заблокировали работу).

#### Статические фабрики
* `public static FileHandler fromURI(Storage form, ProjectHandler defaultProject)`: Фасадный метод, извлекающий строку `uri` из JSON-объекта запроса.
* `public static FileHandler fromURI(String uriStr, ProjectHandler defaultProject)`: Главный калибратор контекста. Разбирает два сценария путей:
    1. **Макрос `@project/...`**: Проверяет наличие `defaultProject` из сессии чата. Если чат привязан, вычисляет абсолютный путь относительно корня проекта. Если сессия пустая — бракует операцию с ошибкой.
    2. **Прямой URI `file:/...`**: Превращает строку в `FileObject`. Запрашивает у IDE владельца через `FileOwnerQuery.getOwner`. Если файл открыт «со стороны» и не входит ни в один открытый проект NetBeans — накладывает вето на автоматический патч.

#### Методы (Fluent API / Конвейер)
* `public boolean isValid()`: Возвращает `true`, если файл существует, проект определён и система готова к безопасной модификации.
* `public String getErrorMessage()`: Возвращает текст ошибки, заблокировавшей конвейер.
* `public FileHandler Backup()`: Проверяет валидность и, если всё чисто, мгновенно отправляет файл в `BackupManager.inProject(...)` для создания точки восстановления перед любым изменением кода. Возвращает `this`.
* `public Storage ApplyCodePatch(Storage form)`: Ключевой движок точечной модификации кода. Принимает параметры `start`, `origin` («было») и `replace` («стало»):
    1. Открывает документ в памяти редактора NetBeans через `EditorCookie` без риска перезаписать несохранённый пользовательский ввод.
    2. Нормализует переносы строк (`\r\n` -> `\n`) для полной независимости от Windows/Linux разницы в подсчётах ИИ.
    3. Выполняет **высокооптимизированный веерный поиск** совпадения `origin` во все стороны от координаты `start` с использованием `String.regionMatches()`. Это исключает нагрузку на память и зависание интерфейса IDE даже на файлах в тысячи строк.
    4. При промахе веера (дикий сдвиг) активирует резервный план Б — полный `indexOf` по тексту файла.
    5. Пересчитывает виртуальные индексы совпадения обратно на физические координаты документа с учётом скрытых символов возврата каретки `\r`.
    6. Удаляет старый кусок, вставляет новый и принудительно фиксирует изменения на диске через `editorCookie.saveDocument()`.
    7. Возвращает `Storage` со статусом `success: true/false` и описанием проблемы в случае неудачи.

* `public String getDiffWithLatestBackup()`: Генерирует экономный построчный diff изменений между последним бэкапом и текущим состоянием файла в IDE.
* `public boolean openAtOffset(int offset)`: Открывает файл в редакторе NetBeans и позиционирует курсор по абсолютному смещению.
* `public boolean openAtOffset(Storage form)`: Открывает файл в редакторе NetBeans и позиционирует курсор на основе формы (смещение `start` + веерный поиск фрагмента `origin`).

---

## 📦 `su.crazy74.sparky.ai`

### `aiModelHandler`
Класс-диспетчер для управления конфигурацией моделей ИИ и провайдеров в рамках текущей сессии.
* `public static aiModelHandler Construct(i_Session session)`: Фабричный метод инициализации обработчика на основе активной сессии, настроек проекта и выбранной модели.
* `public Object config(String path)`: Возвращает значение конфигурации по указанному пути с автоматическим приведением типов (Number, Boolean, String).
* `public i_RequestAI getAPI()`: Фабричный метод получения конкретного экземпляра ИИ-провайдера (например, `DebugEmulator`), привязанного к данной модели.
* `public String getApiKey()`: Возвращает выбранный API-ключ (с поддержкой кэширования и извлечения первого элемента в случае массива).
* `public String getURL()`: Возвращает сформированный URL-адрес вебхука с автоматической подстановкой модели (`{$model}`) и API-ключа (`{$apikey}`).

### `DebugEmulator`
Реализует `i_RequestAI`, перенаправляя все запросы на локальный веб-сервер через HTTP POST. Позволяет тестировать обработку ответов и цепочки промптов без обращения к реальным API.

### `BaseAIProvider`
Базовый абстрактный класс, реализующий `i_RequestAI`. Содержит общую логику выполнения HTTP-запросов к API, авторизации, логирования транзакций в `api_debug.log` и обработки ошибок. Наследники переопределяют ключевые методы сборки сообщений и извлечения текста.
* `protected abstract Object buildMessages(List<String> data)`: Специфичная для API сборка истории сообщений.
* `protected abstract void preparePayload(Storage payload, List<String> data)`: Конфигурирование специфичных параметров запроса (температура, формат ответа).
* `protected abstract String extractAiText(Storage responseData)`: Извлечение текстового ответа из JSON-структуры провайдера.
* `protected String getMessagesKey()`: Возвращает ключ массива сообщений в payload (по умолчанию `"messages"`).

### `OpenAIProvider`
Реализация `BaseAIProvider` для OpenAI-совместимых API. Формирует историю в формате массива объектов с ролями `system`, `user`, `assistant` и настраивает JSON-режим через параметр `response_format`.

### `GeminiProvider`
Реализация `BaseAIProvider` для Google Gemini API. Переопределяет ключ истории на `"contents"`, выносит системный промпт в `systemInstruction`, распределяет роли по спецификации Google (`user`/`model`) и задает MIME-тип в `generationConfig`.

---

## 📦 `su.crazy74.sparky.ui`

### `JavaFxWindow`
Базовый абстрактный компонент для окон NetBeans (`TopComponent`), содержащих встроенный браузер JavaFX `WebView` (`JFXPanel`). Отвечает за безопасную многопоточную инициализацию JavaFX, загрузку HTML-шаблона и связывание JS-моста.
* `public String getGUID()`: Возвращает уникальный идентификатор (GUID) текущего окна.
* `public i_GermanProject getProject()`: Возвращает объект проекта, привязанного к окну.
* `protected void initFxContainer(i_GermanProject project)`: Инициализирует контейнеры Swing и JavaFX с корректным переключением потоков диспетчеризации.
* `abstract protected void loadContent()`: Загрузка целевого HTML-шаблона в `WebEngine`.
* `abstract protected void bindBridge(WebEngine engine)`: Привязка JS-моста к объекту `window` браузера.
* `protected void onPageLoaded(WebEngine engine)`: Оповещение веб-приложения об успешной загрузке страницы (`$(document).trigger('startWork')`).

---

### `ChatTopComponent`
Основное окно взаимодействия (Чат). Наследует `JavaFxWindow`, хостит `WebView` с интерфейсом чата (`chat.html`). Включает две внутренние реализации:

#### `ChatSession`
Реализует интерфейс `i_Session`. Управляет жизненным циклом сессии, хранит историю сообщений и координирует обновление UI.
* `public ProjectHandler getProjectHandler()`: Возвращает обработчик текущего проекта.
* `public i_GermanProject getProject()`: Возвращает объект проекта, привязанного к сессии.
* `public Storage config()`: Формирует конфигурацию сессии, объединяя глобальные настройки и настройки проекта.
* `public Storage getProjectInformation()`: Возвращает метаданные проекта.
* `public boolean isInterrupted()`: Проверяет, прервана ли текущая операция.
* `public void ActivateNode(SessionNode node)`: Активирует новый узел диалога, переносит предыдущий в историю и запускает обработку ИИ в фоновом потоке.
* `public void RetryCurrent(Storage form)`: Перезапускает обработку текущего узла с возможностью смены модели.
* `protected void InterruptCurrent()`: Прерывает выполнение текущего запроса.
* `protected void UpdateNodeUI(SessionNode node)`: Сериализует узел и отправляет его в WebView через JS-метод `jsonUpdateRequest`.
* `protected SessionNode CleanNode(SessionNode node)`: Фильтрует данные узла, оставляя только необходимые для фронтенда поля.
* `public void UpdateNode(SessionNode node)`: Обновляет данные узла в памяти и синхронизирует их с UI.
* `public SessionNode getActive()`: Возвращает активный узел.
* `public List<SessionNode> getHistory()`: Возвращает историю сессии.
* `public void setTitle(String newTitle)`: Устанавливает заголовок сессии и обновляет имя вкладки TopComponent.
* `public Storage info()`: Возвращает метаданные сессии и проекта.

#### `JSBridge`
Класс-мост для вызовов из JavaScript (WebView) в Java-код плагина.
* `public void requestToAPI(String strJSON)`: Принимает новый запрос пользователя в формате JSON и активирует его обработку.
* `public void RetryLast(String strJSON)`: Запрашивает повторную обработку последнего сообщения.
* `public void writeToConsole(String line)`: Выводит строку в системную консоль плагина.
* `public void setInterrupted()`: Сигнализирует о прерывании текущей операции пользователем.
* `public String getProjectData()`: Возвращает JSON-строку с метаданными проекта.
* `public String getEnvironment()`: Возвращает JSON-строку с параметрами окружения и версиями (AppHandler.getEnv()).
* `public String ApplyCodePatch(String jsonStr)`: Принимает параметры патча, выполняет резервное копирование и применяет изменения к файлу.
* `public void openFile(String jsonStr)`: Открывает целевой файл в редакторе NetBeans с позиционированием курсора по каретке.
* `public void smartBufferCopy(String text)`: Помещает в системный буфер обмена структуру данных, содержащую одновременно Markdown и чистый HTML.
* `public void copyToClipboard(String text)`: Копирует переданный текст в системный буфер обмена.
* `public String getModelsList()`: Возвращает JSON-список доступных моделей ИИ.

### `ProjectTopComponent`
Интерактивное окно отображения ментальной карты и настроек проекта. Наследует `JavaFxWindow`, хостит `WebView` с `project.html`.

#### `ProjectJSBridge` 
Класс-мост для взаимодействия JavaScript (WebView ментальной карты) с Java-бэкендом плагина.
* `public void writeToConsole(String line)`: Выводит строку в системную консоль плагина.
* `public void openURI(String UriStr)`: Открывает целевой URI в редакторе NetBeans (для файловых ссылок) или в браузере по умолчанию.
* `public String getEnvironment()`: Возвращает JSON-строку с параметрами окружения.
* `public String getMapData(String json)`: Запрашивает данные структуры ментальной карты через `MindManager`.
* `public String saveMapData(String json)`: Сохраняет текущее состояние ментальной карты через `MindManager`.

### `TaskListTopComponent`
Боковая панель со списком проектов. Использует NetBeans Explorer API (`BeanTreeView`) для отображения открытых проектов в виде дерева. Подписывается на события открытия/закрытия проектов в IDE.

### `NodeProject`
Визуальное представление проекта в дереве. Оборачивает `i_GermanProject` в ноду NetBeans. Определяет контекстное меню (ПКМ) с действиями «Новый чат» и «Редактор проекта».

### `AppHTML`
Подготовщик контента для WebView. Читает HTML-шаблоны, патчит пути к ресурсам (`${baseUrl}`) и динамически применяет текущую тему NetBeans через замену CSS-переменных. Единственный потребитель ключей из `AppConfig.SystemData`.

---

## 🌐 Front-end Layer (HTML/JS)

### `chat.html`
Базовый контейнер для WebView. Хранит CSS-переменные темы (заполняются через `AppHTML`), подключает стили (`reset.css`, `chat.css`) и скрипты (jQuery, `handler_chat.js`). `<body>` пустой — весь интерфейс генерируется динамически.

### `handler_chat.js`
Основной движок UI. Строит DOM с нуля, обрабатывает ввод и связывается с Java-бэкендом.
* **`MainHandler`**: Точка входа. Создаёт каркас страницы (header, content, footer с полем ввода и кнопкой). Прокси-обёртка для вызовов `window.Backend` (`requestToAPI`, `writeToConsole`).
* **`RequestHandler`**: Жизненный цикл одного сообщения. Генерирует GUID, рендерит блок `.question-n-answer`, отображает `stages` в виде `<ul>`, управляет состояниями `waitAnswer` и `error`.
