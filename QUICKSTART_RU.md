# Быстрый старт: Sparky для Apache NetBeans

Sparky — инженерный ассистент и инструмент планирования задач, встроенный в среду разработки Apache NetBeans. Репозиторий проекта доступен на [GitHub](https://github.com/alexCRAZY74/Sparky).

---

## 1. Требования к среде и JavaFX

Для работы интерфейса плагина (встроенный браузер ментальной карты и чата) требуется среда исполнения Java с полноценной поддержкой **JavaFX (OpenJFX)**.

* **Рекомендуемая сборка:** **BellSoft Liberica JDK 21 (пакет Full)**. Стандартные сборки OpenJDK или Oracle JDK без JavaFX вызовут ошибку `ClassNotFoundException: javafx.*` при открытии окон плагина.
* **Среда разработки:** Apache NetBeans 21 или новее.
* **Конфигурация:** Убедитесь, что NetBeans запущен именно под этой JDK (параметр `jdkhome` в `etc/netbeans.conf`).

---

## 2. Установка плагина

1. Скачайте бинарный пакет плагина `*.nbm` из раздела [Releases](https://github.com/alexCRAZY74/Sparky/releases).
2. В главном меню NetBeans откройте: **Tools → Plugins → вкладка Downloaded**.
3. Нажмите кнопку **Add Plugins...**, выберите скачанный `.nbm` файл и подтвердите установку (**Install**).
4. Перезапустите IDE при необходимости.

---

## 3. Настройка ИИ-провайдера

Файл конфигурации открывается в один клик через главное меню: **Window → Sparky → Настройки** (при отсутствии плагин создаёт его автоматически в `config/crazy_sparky/settings.json`).

Минимальная конфигурация для старта:

```json
{
  "language": "ru",
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
        "apikey": "ВАШ_КЛЮЧ_API"
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

Для работы через OpenAI-совместимый API укажите `"transport": "openai"`, соответствующий адрес вебхука и токен авторизации.

---

## 4. Первый запуск и работа

1. Откройте рабочий проект в NetBeans.
2. В контекстном меню проекта выберите **Sparky: Чат** (или откройте окно через **Window → Sparky → Чат**).
3. Все регламенты по архитектуре и конфигурации зашиты в плагин. Достаточно поставить задачу в свободной форме — Искра не только адаптирует файлы проекта, но и самостоятельно настроит параметры самого плагина, подтянет контекст через протокол `needs` и сформирует точечные патчи.
