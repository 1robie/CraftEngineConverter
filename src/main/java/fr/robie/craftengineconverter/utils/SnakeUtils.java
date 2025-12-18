package fr.robie.craftengineconverter.utils;

import fr.robie.craftengineconverter.utils.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * SnakeUtils - A utility class for managing YAML files with convenient access methods.
 *
 * This class provides a fluent API for reading, writing, and manipulating YAML data
 * with support for nested keys using dot notation (e.g., "section.subsection.key").
 *
 * Features:
 * - Load and save YAML files
 * - Navigate nested structures with dot notation
 * - Type-safe getters for common data types (String, int, double, boolean)
 * - Section management (get, create, update sections)
 * - Data manipulation (add, remove, merge, clone)
 * - Auto-save capability
 * - Implements AutoCloseable for try-with-resources support
 *
 * Example usage:
 * <pre>
 * try (SnakeUtils yaml = new SnakeUtils(new File("config.yml"), true)) {
 *     yaml.addData("server.port", 8080);
 *     String host = yaml.getString("server.host", "localhost");
 *     SnakeUtils dbSection = yaml.getOrCreateSection("database");
 *     // Auto-saved on close
 * }
 * </pre>
 *
 * @author Robie
 * @version 1.0
 */
public class SnakeUtils implements AutoCloseable {
    private final File targetFile;
    private final Map<String, Object> data;
    private final boolean autoSave;

    /**
     * Creates a new SnakeUtils instance from a YAML file.
     *
     * @param targetFile The YAML file to load. If the file doesn't exist or is empty,
     *                   an empty data structure will be initialized
     * @param autoSave If true, changes will be automatically saved when close() is called.
     *                 Useful with try-with-resources pattern
     * @throws IOException If the file cannot be read
     */
    public SnakeUtils(@NotNull File targetFile, boolean autoSave) throws IOException {
        this.targetFile = targetFile;
        Yaml yaml = new Yaml();
        Map<String, Object> loadedData;
        try (FileInputStream fis = new FileInputStream(targetFile)){
            loadedData = yaml.load(fis);
        }
        this.data = (loadedData != null) ? loadedData : new LinkedHashMap<>();
        this.autoSave = autoSave;
    }

    /**
     * Creates a new SnakeUtils instance from a YAML file with auto-save disabled.
     *
     * @param targetFile The YAML file to load. If the file doesn't exist or is empty,
     *                   an empty data structure will be initialized
     * @throws IOException If the file cannot be read
     */
    public SnakeUtils(@NotNull File targetFile) throws IOException {
        this(targetFile, false);
    }

    /**
     * Adds or updates data at the specified key path using dot notation.
     * Creates nested sections as needed.
     *
     * Example:
     * <pre>
     * addData("server.database.host", "localhost");
     * // Creates: server -> database -> host: "localhost"
     * </pre>
     *
     * @param key The key path with dot notation (e.g., "section.subsection.key")
     * @param value The value to store (can be any object, Map, List, etc.)
     */
    public void addData(@NotNull String key, @NotNull Object value){
        this.addData(key, value, ".");
    }

    /**
     * Adds or updates data at the specified key path using a custom delimiter.
     * Creates nested sections as needed.
     *
     * @param key The key path with custom delimiter
     * @param value The value to store (can be any object, Map, List, etc.)
     * @param delimiter The delimiter to use for splitting the key path (e.g., ".", "/", ":")
     */
    public void addData(@NotNull String key, @NotNull Object value, @NotNull String delimiter){
        if (key.isEmpty()){
            return;
        }

        String[] keys = key.split(Pattern.quote(delimiter));

        if (keys.length == 0){
            return;
        }

        Map<String, Object> currentMap = this.data;
        for (int i = 0; i < keys.length - 1; i++){
            String k = keys[i];
            if (!currentMap.containsKey(k) || !(currentMap.get(k) instanceof Map)){
                currentMap.put(k, new java.util.LinkedHashMap<String, Object>());
            }
            //noinspection unchecked
            currentMap = (Map<String, Object>) currentMap.get(k);
        }
        currentMap.put(keys[keys.length - 1], value);
    }

    /**
     * Gets the raw data Map containing all YAML data.
     * Modifications to this map will affect the underlying data.
     *
     * @return The data Map (never null, but may be empty)
     */
    public Map<String, Object> getData(){
        return this.data;
    }

    /**
     * Gets the target file associated with this SnakeUtils instance.
     *
     * @return The target file for save operations
     */
    public File getTargetFile(){
        return this.targetFile;
    }

    /**
     * Retrieves a value from the data using a delimited key path.
     * Internal method supporting custom delimiters.
     *
     * @param key The key path with delimiter (e.g., "section.subsection.key")
     * @param delimiter The delimiter to use for splitting the key path
     * @return The value found, or null if absent or path is invalid
     */
    @Nullable
    private Object getValue(@NotNull String key, @NotNull String delimiter){
        if (key.isEmpty() || this.data == null){
            return null;
        }

        String[] keys = key.split(Pattern.quote(delimiter));

        if (keys.length == 0){
            return null;
        }

        Map<String, Object> currentMap = this.data;

        for (int i = 0; i < keys.length - 1; i++){
            String k = keys[i];
            if (!currentMap.containsKey(k) || !(currentMap.get(k) instanceof Map)){
                return null;
            }
            //noinspection unchecked
            currentMap = (Map<String, Object>) currentMap.get(k);
        }

        return currentMap.get(keys[keys.length - 1]);
    }

    /**
     * Retrieves a value from the data using dot notation.
     *
     * @param key The key path with dot notation (e.g., "section.subsection.key")
     * @return The value found, or null if absent or path is invalid
     */
    @Nullable
    private Object getValue(@NotNull String key){
        return getValue(key, ".");
    }

    /**
     * Retrieves an integer value from the specified key path.
     * Returns 0 if the value is absent or cannot be parsed as an integer.
     *
     * @param key The key path with dot notation
     * @return The integer value, or 0 if absent/invalid
     */
    public int getInt(@NotNull String key){
        return getInt(key, 0);
    }

    /**
     * Retrieves an integer value from the specified key path with a default value.
     * Attempts to parse the value as an integer if it's not a Number type.
     *
     * @param key The key path with dot notation
     * @param defaultValue The value to return if the key doesn't exist or cannot be parsed
     * @return The integer value, or defaultValue if absent/invalid
     */
    public int getInt(@NotNull String key, int defaultValue){
        Object value = getValue(key);
        if (value == null) return defaultValue;

        if (value instanceof Number){
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e){
            return defaultValue;
        }
    }

    /**
     * Retrieves a double value from the specified key path.
     * Returns 0.0 if the value is absent or cannot be parsed as a double.
     *
     * @param key The key path with dot notation
     * @return The double value, or 0.0 if absent/invalid
     */
    public double getDouble(@NotNull String key){
        return getDouble(key, 0.0);
    }

    /**
     * Retrieves a double value from the specified key path with a default value.
     * Attempts to parse the value as a double if it's not a Number type.
     *
     * @param key The key path with dot notation
     * @param defaultValue The value to return if the key doesn't exist or cannot be parsed
     * @return The double value, or defaultValue if absent/invalid
     */
    public double getDouble(@NotNull String key, double defaultValue){
        Object value = getValue(key);
        if (value == null) return defaultValue;

        if (value instanceof Number){
            return ((Number) value).doubleValue();
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e){
            return defaultValue;
        }
    }

    /**
     * Retrieves a String value from the specified key path.
     * Returns null if the value is absent. Converts any non-null value to String.
     *
     * @param key The key path with dot notation
     * @return The String value, or null if absent
     */
    @Nullable
    public String getString(@NotNull String key){
        return getString(key, null);
    }

    /**
     * Retrieves a String value from the specified key path with a default value.
     * Converts any non-null value to String using toString().
     *
     * @param key The key path with dot notation
     * @param defaultValue The value to return if the key doesn't exist
     * @return The String value, or defaultValue if absent
     */
    @Nullable
    public String getString(@NotNull String key, @Nullable String defaultValue){
        Object value = getValue(key);
        return (value != null) ? value.toString() : defaultValue;
    }

    /**
     * Retrieves a boolean value from the specified key path.
     * Returns false if the value is absent or cannot be parsed as a boolean.
     *
     * @param key The key path with dot notation
     * @return The boolean value, or false if absent/invalid
     */
    public boolean getBoolean(@NotNull String key){
        return getBoolean(key, false);
    }

    /**
     * Retrieves a boolean value from the specified key path with a default value.
     * Attempts to parse the value as a boolean using Boolean.parseBoolean() if not a Boolean type.
     *
     * @param key The key path with dot notation
     * @param defaultValue The value to return if the key doesn't exist
     * @return The boolean value, or defaultValue if absent
     */
    public boolean getBoolean(@NotNull String key, boolean defaultValue){
        Object value = getValue(key);
        if (value == null) return defaultValue;

        if (value instanceof Boolean){
            return (Boolean) value;
        }

        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Retrieves a Map value from the specified key path.
     * This is useful for accessing nested sections as raw Map objects.
     *
     * @param key The key path with dot notation
     * @return The Map value, or null if absent or not a Map type
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public Map<String, Object> getMap(@NotNull String key){
        Object value = getValue(key);
        return (value instanceof Map) ? (Map<String, Object>) value : null;
    }

    /**
     * Retrieves a List of Maps from the specified key path.
     * This is useful for accessing lists of complex objects stored as maps.
     *
     * Example YAML:
     * <pre>
     * players:
     *   - name: John
     *     score: 100
     *   - name: Jane
     *     score: 200
     * </pre>
     *
     * Usage:
     * <pre>
     * List&lt;Map&lt;String, Object&gt;&gt; players = config.getListMap("players");
     * if (players != null) {
     *     for (Map&lt;String, Object&gt; player : players) {
     *         String name = (String) player.get("name");
     *         Integer score = (Integer) player.get("score");
     *     }
     * }
     * </pre>
     *
     * @param key The key path with dot notation
     * @return The List of Maps, or null if absent, not a List, or contains non-Map elements
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public List<Map<String, Object>> getListMap(@NotNull String key){
        Object value = getValue(key);
        if (!(value instanceof List<?> list)) return new ArrayList<>();

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object item : list) {
            if (!(item instanceof Map)) {
                return new ArrayList<>();
            }
            result.add((Map<String, Object>) item);
        }

        return result;
    }

    /**
     * Retrieves a subsection as a new SnakeUtils instance.
     * This allows you to work directly with a nested section as if it were a separate YAML file.
     * The returned SnakeUtils is independent and backed by a temporary file.
     *
     * Example:
     * <pre>
     * SnakeUtils config = new SnakeUtils(new File("config.yml"));
     * SnakeUtils dbSection = config.getSection("database");
     * if (dbSection != null) {
     *     String host = dbSection.getString("host");
     * }
     * </pre>
     *
     * @param key The key path to the section (e.g., "database" or "server.database")
     * @return A new SnakeUtils instance for the section, or null if the section doesn't exist or isn't a Map
     */
    @Nullable
    public SnakeUtils getSection(@NotNull String key){
        Map<String, Object> sectionData = getMap(key);
        if (sectionData == null){
            return null;
        }

        try {
            File tempFile = File.createTempFile("snakeutils_section_" + key.replace(".", "_"), ".yml");

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);

            try (FileWriter writer = new FileWriter(tempFile)){
                yaml.dump(sectionData, writer);
            }

            SnakeUtils sectionUtils = new SnakeUtils(tempFile);

            tempFile.deleteOnExit();

            return sectionUtils;
        } catch (IOException e){
            Logger.showException("Failed to create section SnakeUtils for key: " + key, e);
            return null;
        }
    }

    /**
     * Retrieves a subsection as a SnakeUtils instance, creating it if it doesn't exist.
     * This is useful when you want to ensure a section exists before working with it.
     *
     * Example:
     * <pre>
     * SnakeUtils config = new SnakeUtils(new File("config.yml"));
     * SnakeUtils dbSection = config.getOrCreateSection("database");
     * dbSection.addData("host", "localhost");
     * dbSection.addData("port", 3306);
     * config.setSection("database", dbSection); // Sync changes back
     * </pre>
     *
     * @param key The key path to the section (e.g., "database" or "config.database")
     * @return A SnakeUtils instance for the section (never null)
     * @throws RuntimeException if the section cannot be created
     */
    @NotNull
    public SnakeUtils getOrCreateSection(@NotNull String key){
        SnakeUtils section = getSection(key);
        if (section != null){
            return section;
        }

        addData(key, new java.util.LinkedHashMap<String, Object>());

        section = getSection(key);
        if (section == null){
            throw new RuntimeException("Failed to create section: " + key);
        }

        return section;
    }

    /**
     * Updates a section with data from another SnakeUtils instance.
     * This completely replaces the existing section with the new data.
     *
     * Use this to sync changes made to a section obtained via getSection() or getOrCreateSection().
     *
     * Example:
     * <pre>
     * SnakeUtils section = config.getOrCreateSection("player");
     * section.addData("name", "John");
     * config.setSection("player", section); // Apply changes to parent
     * </pre>
     *
     * @param key The key path to the section (e.g., "database" or "config.database")
     * @param section The SnakeUtils instance containing the new data
     */
    public void setSection(@NotNull String key, @NotNull SnakeUtils section){
        addData(key, section.getData());
    }

    /**
     * Updates a section with data from a Map.
     * This completely replaces the existing section with the new data.
     *
     * @param key The key path to the section (e.g., "database" or "config.database")
     * @param sectionData The Map containing the new data
     */
    public void setSection(@NotNull String key, @NotNull Map<String, Object> sectionData){
        addData(key, sectionData);
    }

    /**
     * Retrieves a List value from the specified key path.
     * Useful for accessing YAML arrays/lists.
     *
     * Example:
     * <pre>
     * List&lt;Object&gt; items = yaml.getList("server.allowed-ips");
     * if (items != null) {
     *     for (Object item : items) {
     *         System.out.println(item);
     *     }
     * }
     * </pre>
     *
     * @param key The key path with dot notation
     * @return The List value, or null if absent or not a List type
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public List<Object> getList(@NotNull String key){
        Object value = getValue(key);
        return (value instanceof List) ? (List<Object>) value : null;
    }

    /**
     * Checks if a key exists in the data.
     * Supports nested keys with dot notation.
     *
     * @param key The key path with dot notation (e.g., "section.subsection.key")
     * @return true if the key exists, false otherwise
     */
    public boolean contains(@NotNull String key){
        return getValue(key) != null;
    }

    /**
     * Checks if a key exists in the data (alias of contains).
     * Supports nested keys with dot notation.
     *
     * @param key The key path with dot notation (e.g., "section.subsection.key")
     * @return true if the key exists, false otherwise
     */
    public boolean containsKey(@NotNull String key){
        return contains(key);
    }

    /**
     * Checks if the data structure is empty (no root-level keys).
     *
     * @return true if no data is present, false otherwise
     */
    public boolean isEmpty(){
        return this.data == null || this.data.isEmpty();
    }

    /**
     * Returns the number of keys at the root level.
     * Does not count nested keys.
     *
     * @return The number of root-level keys
     */
    public int size(){
        return (this.data != null) ? this.data.size() : 0;
    }

    /**
     * Clears all data from this SnakeUtils instance.
     * After calling this, the data structure will be empty.
     */
    public void clear(){
        if (this.data != null){
            this.data.clear();
        }
    }

    /**
     * Retrieves all keys at the root level.
     *
     * Example:
     * <pre>
     * Set&lt;String&gt; keys = yaml.getKeys();
     * for (String key : keys) {
     *     System.out.println(key);
     * }
     * </pre>
     *
     * @return Set of root-level keys (never null, but may be empty)
     */
    @NotNull
    public Set<String> getKeys(){
        return (this.data != null) ? this.data.keySet() : Collections.emptySet();
    }

    /**
     * Retrieves all keys from a specific section.
     * Useful for iterating over nested sections.
     *
     * Example:
     * <pre>
     * Set&lt;String&gt; dbKeys = yaml.getKeys("database");
     * for (String key : dbKeys) {
     *     System.out.println(key + ": " + yaml.getString("database." + key));
     * }
     * </pre>
     *
     * @param key The key path to the section (e.g., "database" or "config.database")
     * @return Set of keys in the section (never null, but may be empty if section doesn't exist)
     */
    @NotNull
    public Set<String> getKeys(@NotNull String key){
        Map<String, Object> section = getMap(key);
        return (section != null) ? section.keySet() : Collections.emptySet();
    }

    /**
     * Removes a key and its value from the data.
     * Supports nested keys with dot notation.
     *
     * Example:
     * <pre>
     * yaml.removeData("database.password"); // Removes only the password key
     * yaml.removeData("database"); // Removes the entire database section
     * </pre>
     *
     * @param key The key path with dot notation (e.g., "section.subsection.key")
     * @return true if the key was removed, false if it didn't exist
     */
    public boolean removeData(@NotNull String key){
        return removeData(key, ".");
    }

    /**
     * Removes a key and its value from the data using a custom delimiter.
     *
     * @param key The key path with custom delimiter
     * @param delimiter The delimiter to use for splitting the key path
     * @return true if the key was removed, false if it didn't exist
     */
    public boolean removeData(@NotNull String key, @NotNull String delimiter){
        if (key.isEmpty() || this.data == null){
            return false;
        }

        String[] keys = key.split(Pattern.quote(delimiter));

        if (keys.length == 0){
            return false;
        }

        Map<String, Object> currentMap = this.data;

        for (int i = 0; i < keys.length - 1; i++){
            String k = keys[i];
            if (!currentMap.containsKey(k) || !(currentMap.get(k) instanceof Map)){
                return false;
            }
            //noinspection unchecked
            currentMap = (Map<String, Object>) currentMap.get(k);
        }

        return currentMap.remove(keys[keys.length - 1]) != null;
    }

    /**
     * Saves the current data to the target file.
     * Creates parent directories if they don't exist.
     *
     * @throws IOException If the file cannot be written
     */
    public void save() throws IOException {
        save(this.targetFile);
    }

    /**
     * Saves the current data to a specific file.
     * Creates parent directories if they don't exist.
     * Useful for saving to a different location than the original file.
     *
     * @param file The destination file for saving
     * @throws IOException If the file cannot be written
     */
    public void save(@NotNull File file) throws IOException {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()){
            parentDir.mkdirs();
        }

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);

        try (FileWriter writer = new FileWriter(file)){
            yaml.dump(this.data, writer);
        }
    }

    /**
     * Reloads the data from the target file.
     * Discards any unsaved changes and reloads from disk.
     * Useful for detecting external changes to the file.
     *
     * @throws IOException If the file cannot be read
     */
    public void reload() throws IOException {
        Yaml yaml = new Yaml();
        try (FileInputStream fis = new FileInputStream(this.targetFile)){
            Map<String, Object> newData = yaml.load(fis);
            this.data.clear();
            if (newData != null){
                this.data.putAll(newData);
            }
        }
    }

    /**
     * Merges data from another SnakeUtils instance into this one.
     * Existing values are overwritten by the new values.
     * Nested Maps are merged recursively.
     *
     * Example:
     * <pre>
     * SnakeUtils defaults = SnakeUtils.load("defaults.yml");
     * SnakeUtils user = SnakeUtils.load("user.yml");
     * defaults.merge(user); // User settings override defaults
     * </pre>
     *
     * @param other The SnakeUtils instance to merge from
     */
    public void merge(@NotNull SnakeUtils other){
        if (other.data != null){
            mergeMap(this.data, other.data);
        }
    }

    /**
     * Merges a Map into the current data.
     * Existing values are overwritten by the new values.
     * Nested Maps are merged recursively.
     *
     * @param dataToMerge The Map to merge into the current data
     */
    public void merge(@NotNull Map<String, Object> dataToMerge){
        mergeMap(this.data, dataToMerge);
    }

    /**
     * Recursively merges two Maps.
     * When both target and source contain a Map at the same key, they are merged recursively.
     * Otherwise, the source value overwrites the target value.
     *
     * @param target The target Map to merge into
     * @param source The source Map to merge from
     */
    @SuppressWarnings("unchecked")
    private void mergeMap(Map<String, Object> target, Map<String, Object> source){
        for (Map.Entry<String, Object> entry : source.entrySet()){
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map && target.get(key) instanceof Map){
                mergeMap((Map<String, Object>) target.get(key), (Map<String, Object>) value);
            } else {
                target.put(key, value);
            }
        }
    }

    /**
     * Copies all data to another SnakeUtils instance.
     * Clears the target's data first, then copies all data from this instance.
     *
     * @param target The destination SnakeUtils instance
     */
    public void copyTo(@NotNull SnakeUtils target){
        target.data.clear();
        target.data.putAll(this.data);
    }

    /**
     * Creates a deep clone of the data Map.
     * All nested Maps and Lists are recursively cloned.
     * Modifications to the clone will not affect the original data.
     *
     * @return A new Map containing a deep copy of all data
     */
    @NotNull
    public Map<String, Object> clone(){
        return deepClone(this.data);
    }

    /**
     * Recursively clones a Map and all its nested structures.
     * Creates new instances of Maps and Lists to ensure deep copying.
     *
     * @param original The original Map to clone
     * @return A deep copy of the original Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> deepClone(Map<String, Object> original){
        Map<String, Object> copy = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()){
            Object value = entry.getValue();
            if (value instanceof Map){
                copy.put(entry.getKey(), deepClone((Map<String, Object>) value));
            } else if (value instanceof List){
                copy.put(entry.getKey(), new java.util.ArrayList<>((List<?>) value));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }

    /**
     * Converts the current data to a YAML-formatted string.
     * Useful for debugging or displaying the data without writing to a file.
     *
     * Example:
     * <pre>
     * SnakeUtils yaml = new SnakeUtils(new File("config.yml"));
     * System.out.println(yaml.toYamlString());
     * </pre>
     *
     * @return A YAML-formatted string representation of the data
     */
    @NotNull
    public String toYamlString(){
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        return yaml.dump(this.data);
    }

    // =============== STATIC METHODS ===============

    /**
     * Creates a new SnakeUtils instance from a YAML file.
     * This is a convenient static factory method that handles IOExceptions gracefully.
     *
     * Example:
     * <pre>
     * SnakeUtils yaml = SnakeUtils.load(new File("config.yml"));
     * if (yaml != null) {
     *     // File loaded successfully
     * }
     * </pre>
     *
     * @param file The YAML file to load
     * @return A new SnakeUtils instance, or null if an IOException occurs
     */
    @Nullable
    public static SnakeUtils load(@NotNull File file){
        try {
            return new SnakeUtils(file);
        } catch (IOException e){
            Logger.showException("Failed to load YAML file: " + file, e);
            return null;
        }
    }

    /**
     * Creates a new SnakeUtils instance from a file path string.
     * Convenient overload that accepts a String path instead of a File object.
     *
     * Example:
     * <pre>
     * SnakeUtils yaml = SnakeUtils.load("config.yml");
     * SnakeUtils yaml2 = SnakeUtils.load("C:/configs/database.yml");
     * </pre>
     *
     * @param filePath The path to the YAML file to load
     * @return A new SnakeUtils instance, or null if an IOException occurs
     */
    @Nullable
    public static SnakeUtils load(@NotNull String filePath){
        return load(new File(filePath));
    }

    @NotNull
    public static SnakeUtils createEmpty(){
        try {
            File tempFile = File.createTempFile("snakeutils_empty_", ".yml");
            try (FileWriter writer = new FileWriter(tempFile)){
                writer.write("{}");
            }
            SnakeUtils utils = new SnakeUtils(tempFile);
            utils.clear();
            tempFile.deleteOnExit();
            return utils;
        } catch (IOException e){
            throw new RuntimeException("Failed to create empty SnakeUtils", e);
        }
    }

    /**
     * Creates an empty SnakeUtils instance with a target file.
     * The instance starts with no data, but has a file associated for future saves.
     *
     * Example:
     * <pre>
     * SnakeUtils yaml = SnakeUtils.createEmpty(new File("new-config.yml"));
     * yaml.addData("version", "1.0");
     * yaml.save(); // Saves to new-config.yml
     * </pre>
     *
     * @param targetFile The target file for save operations
     * @return A new empty SnakeUtils instance (never null)
     * @throws RuntimeException if the empty instance cannot be created
     */
    @NotNull
    public static SnakeUtils createEmpty(@NotNull File targetFile){
        try {
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()){
                if (!parentDir.mkdirs()) {
                    throw new IOException("Failed to create parent directories for: " + targetFile);
                }
            }

            if (!targetFile.exists()) {
                try (FileWriter writer = new FileWriter(targetFile)){
                    writer.write("{}");
                }
            }

            SnakeUtils utils = new SnakeUtils(targetFile);
            utils.clear();
            return utils;
        } catch (IOException e){
            throw new RuntimeException("Failed to create empty SnakeUtils for file: " + targetFile, e);
        }
    }

    /**
     * Saves a Map directly to a YAML file without creating a SnakeUtils instance.
     * Useful for one-off saves when you already have a Map of data.
     * Creates parent directories if they don't exist.
     *
     * Example:
     * <pre>
     * Map&lt;String, Object&gt; data = new LinkedHashMap&lt;&gt;();
     * data.put("key", "value");
     * boolean success = SnakeUtils.saveToFile(data, new File("output.yml"));
     * </pre>
     *
     * @param data The data Map to save
     * @param file The destination file
     * @return true if the save was successful, false if an IOException occurred
     */
    public static boolean saveToFile(@NotNull Map<String, Object> data, @NotNull File file){
        try {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()){
                parentDir.mkdirs();
            }

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);

            try (FileWriter writer = new FileWriter(file)){
                yaml.dump(data, writer);
            }
            return true;
        } catch (IOException e){
            Logger.showException("Failed to save YAML file: " + file, e);
            return false;
        }
    }

    /**
     * Loads a Map directly from a YAML file without creating a SnakeUtils instance.
     * Useful for quick reads when you don't need the full SnakeUtils functionality.
     *
     * Example:
     * <pre>
     * Map&lt;String, Object&gt; data = SnakeUtils.loadFromFile(new File("config.yml"));
     * if (data != null) {
     *     String value = (String) data.get("key");
     * }
     * </pre>
     *
     * @param file The YAML file to load
     * @return A Map containing the YAML data, or null if the file doesn't exist or an error occurs
     */
    @Nullable
    public static Map<String, Object> loadFromFile(@NotNull File file){
        if (!file.exists() || !file.isFile()){
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file)){
            Yaml yaml = new Yaml();
            return yaml.load(fis);
        } catch (IOException e){
            Logger.showException("Failed to load YAML file: " + file, e);
            return null;
        }
    }

    /**
     * Closes this SnakeUtils instance and automatically saves if auto-save is enabled.
     * This method is called automatically when used with try-with-resources.
     *
     * Example:
     * <pre>
     * try (SnakeUtils yaml = new SnakeUtils(new File("config.yml"), true)) {
     *     yaml.addData("key", "value");
     *     // Auto-saved on close
     * } catch (Exception e) {
     *     e.printStackTrace();
     * }
     * </pre>
     *
     * @throws Exception if save() fails when auto-save is enabled
     */
    @Override
    public void close() throws Exception {
        if (this.autoSave){
            save();
        }
    }
}
