package fr.robie.craftengineconverter.common.configuration;

import fr.robie.craftengineconverter.common.enums.ArmorConverter;
import fr.robie.craftengineconverter.common.enums.ConverterOptions;
import fr.robie.craftengineconverter.common.logger.LogType;
import fr.robie.craftengineconverter.common.logger.Logger;
import fr.robie.craftengineconverter.common.progress.BukkitProgressBar;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Configuration {
    public static boolean enableDebug = false;
    public static boolean autoConvertOnStartup = false;
    public static Material defaultMaterial = Material.PAPER;
    public static boolean disableDefaultItalic = true;
    public static ArmorConverter armorConverterType = ArmorConverter.COMPONENT;
    public static List<String> blacklistedPaths = new ArrayList<>();

    // Formatting options
    public static boolean packetEventsFormatting = true;

    public static boolean bossBarFormatting = true;
    public static boolean actionBarFormatting = true;
    public static boolean pluginMessageFormatting = true;
    public static boolean titleFormatting = true;

    // Tags options
    public static boolean glyphTagEnabled = true;
    public static boolean placeholderAPITagEnabled = true;

    private static volatile Configuration instance;
    private boolean isUpdated = false;

    private Configuration() {
    }

    public static Configuration getInstance(){
        if (instance == null){
            synchronized (Configuration.class){
                if (instance == null){
                    instance = new Configuration();
                }
            }
        }
        return instance;
    }

    /**
     * Checks if a given namespaced path is blacklisted.
     * Supports wildcard patterns using *.
     *
     * @param namespacedPath The path to check (e.g., "minecraft:textures/block/stone.png")
     * @return true if the path matches any blacklisted pattern
     */
    public static boolean isPathBlacklisted(String namespacedPath) {
        if (namespacedPath == null || blacklistedPaths.isEmpty()) {
            return false;
        }

        for (String pattern : blacklistedPaths) {
            if (matchesPattern(namespacedPath, pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a path matches a pattern with wildcard support.
     * Supports:
     * - Exact match: "minecraft:textures/block/stone.png"
     * - Wildcard: "minecraft:textures/*" matches everything under minecraft:textures/
     * - Without namespace: "textures/*" matches "namespace:textures/*" for any namespace
     *
     * @param path The path to check
     * @param pattern The pattern to match against
     * @return true if the path matches the pattern
     */
    private static boolean matchesPattern(String path, String pattern) {
        if (pattern.contains("*")) {
            String regex = pattern.replace(".", "\\.")
                                  .replace("*", ".*");

            if (path.matches(regex)) {
                return true;
            }

            if (!pattern.contains(":") && path.contains(":")) {
                String pathWithoutNamespace = path.substring(path.indexOf(":") + 1);
                String patternRegex = pattern.replace(".", "\\.")
                                            .replace("*", ".*");
                return pathWithoutNamespace.matches(patternRegex);
            }
        } else {
            if (path.equals(pattern)) {
                return true;
            }

            if (!pattern.contains(":") && path.contains(":")) {
                String pathWithoutNamespace = path.substring(path.indexOf(":") + 1);
                return pathWithoutNamespace.equals(pattern);
            }
        }

        return false;
    }

    public void load(YamlConfiguration config, File file) {
        for (ConfigPath configPath : ConfigPath.values()) {
            Object value;
            switch (configPath.getDefaultValue()) {
                case Boolean b -> value = getOrAddBoolean(config, configPath.getPath(), b);
                case Integer i -> value = getOrAddInt(config, configPath.getPath(), i);
                case String s -> value = getOrAddString(config, configPath.getPath(), s);
                case Long l -> value = getOrAddLong(config, configPath.getPath(), l);
                case List<?> list -> value = getOrAddList(config, configPath.getPath(), list);
                case null, default -> {
                    continue;
                }
            }
            configPath.assign(value);
        }
        for (ConverterOptions options : ConverterOptions.values()){
            if (options == ConverterOptions.ALL) continue;
            String path = "progress-bar-options." + options.name().toLowerCase().replace("_", "-");
            String progressColor = getOrAddString(config, path + ".progress-color", options.getProgressColor().name());
            String emptyColor = getOrAddString(config, path + ".empty-color", options.getEmptyColor().name());
            String percentColor = getOrAddString(config, path + ".percent-color", options.getPercentColor().name());
            char progressChar = getOrAddString(config, path + ".progress-char", String.valueOf(options.getProgressChar())).charAt(0);
            char emptyChar = getOrAddString(config, path + ".empty-char", String.valueOf(options.getEmptyChar())).charAt(0);
            int barWidth = getOrAddInt(config, path + ".bar-width", options.getBarWidth());
            try {
                options.setProgressColor(BukkitProgressBar.ProgressColor.valueOf(progressColor.toUpperCase()));
            } catch (Exception e) {
                Logger.debug("Invalid progress color for " + options.name() + " in configuration, valid values are: "+ String.join(",", getAvailableColors()), LogType.WARNING);
            }
            try {
                options.setEmptyColor(BukkitProgressBar.ProgressColor.valueOf(emptyColor.toUpperCase()));
            } catch (Exception e) {
                Logger.debug("Invalid empty color for " + options.name() + " in configuration, valid values are: "+ String.join(",",getAvailableColors()), LogType.WARNING);
            }
            try {
                options.setPercentColor(BukkitProgressBar.ProgressColor.valueOf(percentColor.toUpperCase()));
            } catch (Exception e) {
                Logger.debug("Invalid percent color for " + options.name() + " in configuration, valid values are: "+ String.join(",",getAvailableColors()), LogType.WARNING);
            }
            options.setProgressChar(progressChar);
            options.setEmptyChar(emptyChar);
            options.setBarWidth(barWidth);
        }
        if (isUpdated){
            try {
                config.save(file);
                isUpdated = false;
            } catch (Exception e) {
                Logger.info("Could not save the configuration file: " + e.getMessage(), LogType.ERROR);
            }
        }
    }

    private List<String> getAvailableColors(){
        List<String> colors = new ArrayList<>();
        for (BukkitProgressBar.ProgressColor color : BukkitProgressBar.ProgressColor.values()){
            colors.add(color.name());
        }
        return colors;
    }

    private boolean getOrAddBoolean(YamlConfiguration config, String path, boolean defaultValue) {
        if (!config.contains(path)) {
            config.set(path, defaultValue);
            this.isUpdated = true;
            return defaultValue;
        }
        return config.getBoolean(path);
    }
    private int getOrAddInt(YamlConfiguration config, String path, int defaultValue) {
        if (!config.contains(path)) {
            config.set(path, defaultValue);
            this.isUpdated = true;
            return defaultValue;
        }
        return config.getInt(path);
    }
    private String getOrAddString(YamlConfiguration config, String path, String defaultValue) {
        if (!config.contains(path)) {
            config.set(path, defaultValue);
            this.isUpdated = true;
            return defaultValue;
        }
        return config.getString(path, defaultValue);
    }
    private long getOrAddLong(YamlConfiguration config, String path, long defaultValue) {
        if (!config.contains(path)) {
            config.set(path, defaultValue);
            this.isUpdated = true;
            return defaultValue;
        }
        return config.getLong(path, defaultValue);
    }

    @SuppressWarnings("unchecked")
    private List<String> getOrAddList(YamlConfiguration config, String path, List<?> defaultValue) {
        if (!config.contains(path)) {
            config.set(path, defaultValue);
            this.isUpdated = true;
            return (List<String>) defaultValue;
        }
        return config.getStringList(path);
    }

    public enum ConfigPath {
        ENABLE_DEBUG("enable-debug", false, v -> enableDebug = (Boolean) v),
        AUTO_CONVERT_ON_STARTUP("auto-convert-on-startup", false, v -> autoConvertOnStartup = (Boolean) v),
        DEFAULT_MATERIAL("default-material", "PAPER", v -> {
            try {
                String string = (String) v;
                defaultMaterial = Material.valueOf(string.toUpperCase());
            } catch (Exception e) {
                Logger.debug("Invalid default material in configuration, using PAPER as default.", LogType.WARNING);
                defaultMaterial = Material.PAPER;
            }
        }),
        DISABLE_DEFAULT_ITALIC("disable-default-italic", true, v -> disableDefaultItalic = (Boolean) v),
        ARMOR_CONVERTER_TYPE("armor-converter-type", "COMPONENT", v -> {
            try {
                String string = (String) v;
                armorConverterType = ArmorConverter.valueOf(string.toUpperCase());
            } catch (Exception e) {
                Logger.debug("Invalid armor converter type in configuration, using COMPONENT as default.", LogType.WARNING);
                armorConverterType = ArmorConverter.COMPONENT;
            }
        }),
        BLACKLISTED_PATHS("blacklisted-paths", new ArrayList<>(), v -> {
            blacklistedPaths.clear();
            if (v instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<String> paths = (List<String>) v;
                blacklistedPaths.addAll(paths);
            }
        }),
        PACKET_EVENTS_FORMATTING("formatting.packet-events", true, v -> packetEventsFormatting = (Boolean) v),
        BOSS_BAR_FORMATTING("formatting.boss-bar", true, v -> bossBarFormatting = (Boolean) v),
        ACTION_BAR_FORMATTING("formatting.action-bar", true, v -> actionBarFormatting = (Boolean) v),
        PLUGIN_MESSAGE_FORMATTING("formatting.plugin-message", true, v -> pluginMessageFormatting = (Boolean) v),
        TITLE_FORMATTING("formatting.title", true, v -> titleFormatting = (Boolean) v),
        GLYPH_TAG_ENABLED("tags.glyph.enabled", true, v -> glyphTagEnabled = (Boolean) v),
        PLACEHOLDER_API_TAG_ENABLED("tags.placeholder-api.enabled", true, v -> placeholderAPITagEnabled = (Boolean) v)
        ;

        private final String path;
        private final Object defaultValue;
        private final Consumer<Object> setter;

        ConfigPath(String path, Object defaultValue, Consumer<Object> setter) {
            this.path = path;
            this.defaultValue = defaultValue;
            this.setter = setter;
        }

        public String getPath() {
            return path;
        }
        public Object getDefaultValue() {
            return defaultValue;
        }
        public void assign(Object value) {
            setter.accept(value);
        }
    }
}
