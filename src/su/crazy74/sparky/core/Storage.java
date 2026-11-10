package su.crazy74.sparky.core;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashSet;
import java.util.Set;

public class Storage {

  protected Object data;

  public void set(Object newdata) {
    this.data = newdata;
  }

  @SuppressWarnings("unchecked")
  public void set(Map<String, Object> newdata) {
    this.data = newdata != null ? new LinkedHashMap<>(newdata) : new LinkedHashMap<>();
  }

  @SuppressWarnings("unchecked")
  public void set(List<Object> newdata) {
    this.data = newdata;
  }

  @SuppressWarnings("unchecked")
  public void Merge(Map<String, Object> target) {
    if (target == null) {
      return;
    }
    if (!(this.data instanceof Map)) {
      this.data = new LinkedHashMap<String, Object>();
    }
    mergeMaps((Map<String, Object>) this.data, target);
  }

  @SuppressWarnings("unchecked")
  public void Merge(String path, Map<String, Object> target) {
    if (path == null || path.isEmpty()) {
      this.Merge(target);
      return;
    }
    if (target == null) {
      return;
    }
    Object obj = this.get(path);
    if (!(obj instanceof Map)) {
      obj = new LinkedHashMap<String, Object>();
      this.set(path, obj);
    }
    mergeMaps((Map<String, Object>) obj, target);
  }

  public void Merge(Storage target) {
    if (target != null) {
      this.Merge(target.get());
    }
  }

  public void Merge(String path, Storage target) {
    if (target != null) {
      this.Merge(path, target.get());
    }
  }

  @SuppressWarnings("unchecked")
  private void mergeMaps(Map<String, Object> original, Map<String, Object> target) {
    if (target == null || original == null) {
      return;
    }
    for (Map.Entry<String, Object> entry : target.entrySet()) {
      String key = entry.getKey();
      Object targetValue = entry.getValue();
      Object originalValue = original.get(key);

      if (originalValue instanceof Map && targetValue instanceof Map) {
        mergeMaps((Map<String, Object>) originalValue, (Map<String, Object>) targetValue);
      } else {
        original.put(key, targetValue);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> get() {
    if (this.data instanceof Map) {
      return (Map<String, Object>) this.data;
    }
    return new LinkedHashMap<>();
  }

  public Object getRaw() {
    return this.data;
  }

  public Boolean isEmpty() {
    if (this.data == null) {
      return true;
    }
    if (this.data instanceof Map) {
      return ((Map<?, ?>) this.data).isEmpty();
    }
    if (this.data instanceof List) {
      return ((List<?>) this.data).isEmpty();
    }
    if (this.data instanceof String) {
      return ((String) this.data).isEmpty();
    }
    return false;
  }

  public Storage() {
    this.data = new LinkedHashMap<String, Object>();
  }

  public Storage(Object data) {
    this.data = data != null ? data : new LinkedHashMap<String, Object>();
  }

  public static Storage fromJson(String json) {
    try {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      Object parsed = gson.fromJson(json, Object.class);
      return new Storage(parsed);
    } catch (Exception e) {
      return new Storage();
    }
  }

  public String toJson() {
    Gson gson = new GsonBuilder()
            .create();
    return gson.toJson(this.data);
  }

  public String toFmtJson() {
    Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    return gson.toJson(this.data);
  }

  public boolean Exists(String path) {
    return get(path) != null;
  }

  public String typeOf(String path) {
    Object val = get(path);
    if (val == null) {
      return "null";
    }
    if (val instanceof Map) {
      return "object";
    }
    if (val instanceof List) {
      return "array";
    }
    if (val instanceof String) {
      return "string";
    }
    if (val instanceof Number) {
      return "number";
    }
    if (val instanceof Boolean) {
      return "boolean";
    }
    return val.getClass().getSimpleName().toLowerCase();
  }

  public void set(String path, Object value) {
    if (path == null || path.isEmpty()) {
      return;
    }

    if (value instanceof Storage) {
      value = ((Storage) value).getRaw();
    }

    String[] keys = path.split("/");

    if (keys.length > 0 && ("[]".equals(keys[0]) || keys[0].matches("\\d+"))) {
      if (!(data instanceof List)) {
        if (data == null || (data instanceof Map && ((Map<?, ?>) data).isEmpty())) {
          data = new java.util.ArrayList<>();
        }
      }
    }

    Object current = data;

    //AppHandler.toConsole("set() " + path);
    for (int i = 0; i < keys.length - 1; i++) {
      String key = keys[i];
      Object next = getStep(current, key);
      //AppHandler.toConsole("key: " + key);

      if (!(next instanceof Map) && !(next instanceof List)) {
        String nextKey = keys[i + 1];
        if ("[]".equals(nextKey) || nextKey.matches("\\d+")) {
          List<Object> newList = new ArrayList<>();
          setStep(current, key, newList);
          next = newList;

        } else {
          Map<String, Object> newMap = new LinkedHashMap<>();
          setStep(current, key, newMap);
          next = newMap;
        }
      }
      current = next;
    }
    setStep(current, keys[keys.length - 1], value);
  }

  public void Remove(String path) {
    if (path == null || path.isEmpty()) {
      return;
    }
    if (!this.Exists(path)) {
      return;
    }

    String[] keys = path.split("/");
    if (keys.length == 1) {
      if (data instanceof Map) {
        ((Map<?, ?>) data).remove(keys[0]);
      } else if (data instanceof List && keys[0].matches("\\d+")) {
        int index = Integer.parseInt(keys[0]);
        List<?> list = (List<?>) data;
        if (index >= 0 && index < list.size()) {
          list.remove(index);
        }
      }
      return;
    }

    // Ищем родителя последнего элемента
    StringBuilder parentPath = new StringBuilder();
    for (int i = 0; i < keys.length - 1; i++) {
      parentPath.append(keys[i]).append(i == keys.length - 2 ? "" : "/");
    }

    Object parent = get(parentPath.toString());
    String lastKey = keys[keys.length - 1];

    if (parent instanceof Map) {
      ((Map<?, ?>) parent).remove(lastKey);
    } else if (parent instanceof List && lastKey.matches("\\d+")) {
      int index = Integer.parseInt(lastKey);
      List<?> list = (List<?>) parent;
      if (index >= 0 && index < list.size()) {
        list.remove(index);
      }
    }
  }

  // Вспомогательный метод для навигации (используется в оригинальном get)
  public Object get(String path) {
    if (path == null || path.isEmpty()) {
      return data;
    }
    String[] keys = path.split("/");
    Object current = data;
    for (String key : keys) {
      current = getStep(current, key);
      if (current == null) {
        return null;
      }
    }
    return current;
  }

  private Object getStep(Object container, String key) {
    if (container instanceof Map) {
      return ((Map<?, ?>) container).get(key);
    } else if (container instanceof List && key.matches("\\d+")) {
      int index = Integer.parseInt(key);
      List<?> list = (List<?>) container;
      return (index >= 0 && index < list.size()) ? list.get(index) : null;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private void setStep(Object container, String key, Object value) {
    if (container instanceof Map) {
      ((Map<String, Object>) container).put(key, value);
    } else if (container instanceof List) {
      List<Object> list = (List<Object>) container;
      if ("[]".equals(key)) {
        list.add(value);
      } else if (key.matches("\\d+")) {
        int index = Integer.parseInt(key);
        if (index >= 0 && index < list.size()) {
          list.set(index, value);
        } else if (index == list.size()) {
          list.add(value);
        }
      }
    }
  }

  // --- Типизированные геттеры (оставляем как были) ---
  public String getStr(String path) {
    Object val = get(path);
    return (val != null) ? String.valueOf(val) : null;
  }

  public boolean getBool(String path) {
    Object val = get(path);
    if (val instanceof Boolean) {
      return (Boolean) val;
    }
    if (val instanceof String) {
      return Boolean.parseBoolean((String) val);
    }
    return false;
  }

  public int getInt(String path) {
    Object val = get(path);
    if (val instanceof Number) {
      return ((Number) val).intValue();
    }
    if (val instanceof String) {
      try {
        return Integer.parseInt((String) val);
      } catch (NumberFormatException e) {
      }
    }
    return 0;
  }

  public float getFloat(String path) {
    Object val = get(path);
    if (val instanceof Number) {
      return ((Number) val).floatValue();
    }
    if (val instanceof String) {
      try {
        return Float.parseFloat((String) val);
      } catch (NumberFormatException e) {
      }
    }
    return 0.0f;
  }

  @SuppressWarnings("unchecked")
  public List<Object> getList(String path) {
    Object val = get(path);
    if (val instanceof List) {
      return (List<Object>) val;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public List<String> getStrList(String path) {
    Object val = this.get(path); // или как у тебя устроен поиск по path
    if (val instanceof List) {
      return (List<String>) val;
    }
    return new ArrayList<>();
  }

  /**
   * Возвращает список ключей (или индексов) для указанного пути
   */
  public List<String> childrenKeys(String path) {
    List<String> result = new ArrayList<>();
    Object rawData = this.get(path);

    if (rawData == null) {
      return result;
    }

    // Если это Мапа (JSON-объект) — забираем её ключи
    if (rawData instanceof Map) {
      Map<String, Object> data = (Map<String, Object>) rawData;
      for (String key : data.keySet()) {
        result.add(key);
      }
    } // Если это Лист (JSON-массив) — отдаем индексы "0", "1", "2"...
    else if (rawData instanceof List) {
      List<?> list = (List<?>) rawData;
      for (int i = 0; i < list.size(); i++) {
        result.add(String.valueOf(i));
      }
    }

    return result;
  }

  public List<String> childrenKeys() {
    List<String> result = new ArrayList<>();
    if (this.data instanceof Map) {
      for (Object key : ((Map<?, ?>) this.data).keySet()) {
        result.add(String.valueOf(key));
      }
    } else if (this.data instanceof List) {
      List<?> list = (List<?>) this.data;
      for (int i = 0; i < list.size(); i++) {
        result.add(String.valueOf(i));
      }
    }
    return result;
  }

  // ==========================================
  // ИНТЕГРАЦИЯ STORAGENODE (ВИРТУАЛЬНОЕ ПРЕДСТАВЛЕНИЕ)
  // ==========================================
  /**
   * Получить виртуальный узел дерева по пути. Если по пути ничего нет или лежит
   * примитив, метод бережно создаст там Map, сохраняя возможность древовидного
   * обхода, и вернет обертку.
   */
  @SuppressWarnings("unchecked")
  public StorageNode getNode(String path) {
    Object obj = get(path);
    if (!(obj instanceof Map)) {
      Map<String, Object> newMap = new LinkedHashMap<>();
      // Если там лежал примитив, мы можем сохранить его в скрытое поле _value создаваемого узла
      if (obj != null) {
        newMap.put("_value", obj);
      }
      set(path, newMap);
      return new StorageNode(newMap);
    }
    return new StorageNode((Map<String, Object>) obj);
  }

  /**
   * Легковесный виртуальный узел, работающий напрямую с картами внутри
   * основного Storage. Идеален для построения MindMap и кастомных осей Канбана.
   */
  public static class StorageNode {

    private final Map<String, Object> innerMap;

    public StorageNode(Map<String, Object> map) {
      this.innerMap = map != null ? map : new LinkedHashMap<>();
    }

    // Получить атомарное значение этого узла (например, описание таски или текст)
    public Object getValue() {
      return innerMap.get("_value");
    }

    public void setValue(Object val) {
      innerMap.put("_value", val);
    }

    // Легкий доступ к метаданным узла (теги, статусы канбана, координаты)
    @SuppressWarnings("unchecked")
    public Map<String, String> getMeta() {
      Object meta = innerMap.get("_meta");
      if (!(meta instanceof Map)) {
        Map<String, String> newMeta = new LinkedHashMap<>();
        innerMap.put("_meta", newMeta);
        return newMeta;
      }
      return (Map<String, String>) meta;
    }

    // Переход к дочернему узлу по имени ключа
    @SuppressWarnings("unchecked")
    public StorageNode child(String key) {
      Object child = innerMap.get(key);
      if (!(child instanceof Map)) {
        Map<String, Object> newMap = new LinkedHashMap<>();
        innerMap.put(key, newMap);
        return new StorageNode(newMap);
      }
      return new StorageNode((Map<String, Object>) child);
    }
    // Получить список имен всех дочерних узлов на этом уровне (минуя служебные поля)

    public Set<String> getChildrenKeys() {
      Set<String> keys = new HashSet<>(innerMap.keySet());
      keys.remove("_value");
      keys.remove("_meta");
      return keys;
    }

    // Прямой доступ к нижележащей мапе, если нужен сырой экспорт
    public Map<String, Object> asMap() {
      return this.innerMap;
    }
  }

// ==========================================
  // РАБОТА С MARKDOWN (Парсинг и Генерация)
  // ==========================================
  public static Storage fromMarkdown(String mdText) {
    Storage storage = new Storage();
    if (mdText == null || mdText.strip().isEmpty()) {
      return storage;
    }

    String[] lines = mdText.split("\\r?\\n");

    List<String> currentPath = new ArrayList<>();
    StringBuilder textBuffer = new StringBuilder();
    Map<String, String> metaBuffer = new LinkedHashMap<>();
    for (String line : lines) {
      String lineTrimmed = line.trim();
      int headerLevel = 0;
      while (headerLevel < line.length() && line.charAt(headerLevel) == '#') {
        headerLevel++;
      }

      // Проверяем, заголовок ли это (после решеток должен быть пробел)
      if (headerLevel > 0 && headerLevel < line.length() && line.charAt(headerLevel) == ' ') {

        // Перед тем как зайти в новый заголовок, сохраняем данные старого
        saveNodeData(storage, currentPath, textBuffer, metaBuffer);

        String headingName = line.substring(headerLevel).trim();

        // Схлопываем путь до текущего уровня вложенности
        while (currentPath.size() >= headerLevel) {
          currentPath.remove(currentPath.size() - 1);
        }
        currentPath.add(headingName);

      } else {
        // Проверяем, не метаданные ли это в формате HTML-комментариев
        if (lineTrimmed.startsWith("<!--") && lineTrimmed.endsWith("-->")) {
          String content = lineTrimmed.substring(4, lineTrimmed.length() - 3).trim();
          int eqIdx = content.indexOf('=');
          if (eqIdx > 0) {
            String mKey = content.substring(0, eqIdx).trim();
            String mVal = content.substring(eqIdx + 1).trim();
            metaBuffer.put(mKey, mVal);
          }
        } else {
          // Обычный текст (контент узла)
          textBuffer.append(line).append("\n");
        }
      }
    }

    // Сохраняем хвосты для самого последнего узла
    saveNodeData(storage, currentPath, textBuffer, metaBuffer);
    return storage;
  }

  private static void saveNodeData(Storage storage, List<String> path, StringBuilder textBuf, Map<String, String> metaBuf) {
    String text = textBuf.toString().trim();
    if (path.isEmpty() && text.isEmpty() && metaBuf.isEmpty()) {
      textBuf.setLength(0);
      metaBuf.clear();
      return;
    }

    // Строим строковый путь для метода getNode()
    StringBuilder fullPath = new StringBuilder();
    for (int i = 0; i < path.size(); i++) {
      fullPath.append(path.get(i)).append(i == path.size() - 1 ? "" : "/");
    }

    // Если путь пустой (текст до первого заголовка), сохраняем в корень
    StorageNode node = storage.getNode(fullPath.toString());

    if (!text.isEmpty()) {
      node.setValue(text);
    }
    if (!metaBuf.isEmpty()) {
      node.getMeta().putAll(metaBuf);
    }

    textBuf.setLength(0);
    metaBuf.clear();
  }

  public String toMarkdown() {
    StringBuilder sb = new StringBuilder();
    if (this.data instanceof Map) {
      buildMarkdownRecursive((Map<String, Object>) this.data, sb, 1);
    }
    if (this.data instanceof List) {
      for (Object item : (List<Object>) this.data) {
        if (item instanceof Map) {
          buildMarkdownRecursive((Map<String, Object>) item, sb, 1);
        }
      }
    }
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  private void buildMarkdownRecursive(Map<String, Object> currentMap, StringBuilder sb, int level) {
    if (currentMap == null) {
      return;
    }

    // Сначала выгребаем служебные поля текущего уровня, если они есть
    if (currentMap.containsKey("_value")) {
      Object val = currentMap.get("_value");
      if (val != null) {
        sb.append(val.toString().trim()).append("\n\n");
      }
    }

    if (currentMap.containsKey("_meta")) {
      Object metaObj = currentMap.get("_meta");
      if (metaObj instanceof Map) {
        Map<?, ?> meta = (Map<?, ?>) metaObj;
        for (Map.Entry<?, ?> mEntry : meta.entrySet()) {
          Object key = mEntry.getKey();
          Object val = mEntry.getValue();
          if (key != null) {
            String valStr;
            if (val instanceof Map || val instanceof List) {
              valStr = new com.google.gson.Gson().toJson(val);
            } else {
              valStr = val != null ? String.valueOf(val) : "";
            }
            sb.append("<!-- ").append(key).append("=").append(valStr).append(" -->\n");
          }
        }
        if (!meta.isEmpty()) {
          sb.append("\n");
        }
      }
    }
    // Теперь перебираем все остальные ключи (это дочерние ветки)
    for (Map.Entry<String, Object> entry : currentMap.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      // Пропускаем служебные маркеры
      if ("_value".equals(key) || "_meta".equals(key)) {
        continue;
      }

      // Каждая вложенная Map — это новый подзаголовок
      if (value instanceof Map) {
        sb.append("#".repeat(Math.min(level, 6))).append(" ").append(key).append("\n\n");
        buildMarkdownRecursive((Map<String, Object>) value, sb, level + 1);
      }
    }
  }
}
