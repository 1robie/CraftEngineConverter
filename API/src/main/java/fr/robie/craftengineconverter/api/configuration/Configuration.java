package fr.robie.craftengineconverter.api.configuration;

import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.enums.ConverterOption;
import fr.robie.craftengineconverter.api.enums.CraftEngineBlockState;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.logger.LogType;
import fr.robie.craftengineconverter.api.logger.Logger;
import fr.robie.craftengineconverter.api.progress.BukkitProgressBar;
import fr.robie.craftengineconverter.api.progress.ProgressBarOption;
import fr.robie.craftengineconverter.api.progress.ProgressBarUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class Configuration {
    private static final Map<ConfigurationKey, Object> configValues = new EnumMap<>(ConfigurationKey.class);

    public static ProgressBarUtils worldConverterProgressBarOptions = ProgressBarOption.of(BukkitProgressBar.ProgressColor.GOLD);

    private static volatile Configuration instance;
    private boolean isUpdated = false;

    private Configuration() {
    }

    public static Configuration getInstance() {
        if (instance == null) {
            synchronized (Configuration.class) {
                if (instance == null) {
                    instance = new Configuration();
                }
            }
        }
        return instance;
    }

    /**
     * Gets the auto-convert on startup options for a specific converter.
     *
     * @param converterName The name of the converter (e.g., "nexo", "ia")
     * @return A list of converter options, or an empty list if none are specified
     */
    public static List<ConverterOption> getAutoConvertOnStartupOptions(String converterName) {
        Map<String, List<ConverterOption>> options = get(ConfigurationKey.AUTO_CONVERT_ON_STARTUP_TYPES);
        return options.getOrDefault(converterName.toLowerCase(), Collections.emptyList());
    }

    /**
     * Checks if a given namespaced path is blacklisted.
     * Supports wildcard patterns using *.
     *
     * @param namespacedPath The path to check (e.g., "minecraft:textures/block/stone.png")
     * @return true if the path matches any blacklisted pattern
     */
    public static boolean isPathBlacklisted(String namespacedPath) {
        if (namespacedPath == null || (Configuration.<List<String>>get(ConfigurationKey.BLACKLISTED_PATHS)).isEmpty()) {
            return false;
        }

        for (String pattern : Configuration.<List<String>>get(ConfigurationKey.BLACKLISTED_PATHS)) {
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
     * @param path    The path to check
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
        long startTime = System.currentTimeMillis();
        for (ConfigurationKey key : ConfigurationKey.values()) {
            Object defaultValue = key.getDefaultValue();
            Object o = config.get(key.getPath());
            if (o == null) {
                config.set(key.getPath(), defaultValue);
                this.isUpdated = true;
                if (key.getRawType().isInstance(defaultValue)) {
                    configValues.put(key, defaultValue);
                } else {
                    Logger.debug(Message.ERROR__PLUGIN__CONFIGURATION__TYPE_MISMATCH, LogType.ERROR, "path", key.getPath(), "expected", key.getRawType().getSimpleName(), "got", defaultValue.getClass().getSimpleName(), "default", defaultValue);
                }
                continue;
            }
            Object value;
            try {
                value = key.deserialize(o);
            } catch (Exception e) {
                Logger.info("Invalid value for " + key.getPath() + " in configuration, using default value: " + defaultValue, LogType.WARNING);
                value = defaultValue;
            }
            if (key.getRawType().isInstance(value)) {
                configValues.put(key, value);
            } else {
                Logger.debug(Message.ERROR__PLUGIN__CONFIGURATION__TYPE_MISMATCH, LogType.ERROR, "path", key.getPath(), "expected", key.getRawType().getSimpleName(), "got", defaultValue.getClass().getSimpleName(), "default", defaultValue);
            }
        }
        for (ConverterOption options : ConverterOption.values()) {
            if (options == ConverterOption.ALL) {
                continue;
            }
            String path = "progress-bar-options." + options.name().toLowerCase().replace("_", "-");
            this.loadProgressBarOption(config, options, path);
        }
        ConfigurationSection worldConverterProgressBarSection = config.getConfigurationSection("world-converter.progress-bar-options");
        if (worldConverterProgressBarSection != null) {
            this.loadProgressBarOption(config, worldConverterProgressBarOptions, "world-converter.progress-bar-options");
        }
        for (CraftEngineBlockState blockStateLimit : CraftEngineBlockState.values()) {
            String path = "block-state-limit." + blockStateLimit.name().toLowerCase().replace("_", "-");
            int startLimit = this.getOrAddInt(config, path + ".start-limit", blockStateLimit.getStart());
            try {
                blockStateLimit.setStart(startLimit);
            } catch (Exception e) {
                Logger.debug("Invalid start limit for " + blockStateLimit.name() + " in configuration.", LogType.WARNING);
            }
        }
        if (this.isUpdated) {
            try {
                config.save(file);
                this.isUpdated = false;
            } catch (Exception e) {
                Logger.info("Could not save the configuration file: " + e.getMessage(), LogType.ERROR);
            }
        }
        long endTime = System.currentTimeMillis();
        Logger.info(Message.MESSAGE__PLUGIN__CONFIGURATION__LOADED, LogType.SUCCESS, "time", TimerBuilder.formatTimeAuto(endTime - startTime));
    }

    private void loadProgressBarOption(YamlConfiguration config, ProgressBarUtils options, String path) {
        String progressColor = this.getOrAddString(config, path + ".progress-color", options.getProgressColor().name());
        String emptyColor = this.getOrAddString(config, path + ".empty-color", options.getEmptyColor().name());
        String percentColor = this.getOrAddString(config, path + ".percent-color", options.getPercentColor().name());
        char progressChar = this.getOrAddString(config, path + ".progress-char", String.valueOf(options.getProgressChar())).charAt(0);
        char emptyChar = this.getOrAddString(config, path + ".empty-char", String.valueOf(options.getEmptyChar())).charAt(0);
        int barWidth = this.getOrAddInt(config, path + ".bar-width", options.getBarWidth());
        try {
            options.setProgressColor(BukkitProgressBar.ProgressColor.valueOf(progressColor.toUpperCase()));
        } catch (Exception e) {
            Logger.debug("Invalid progress color for " + options + " in configuration, valid values are: " + String.join(",", this.getAvailableColors()), LogType.WARNING);
        }
        try {
            options.setEmptyColor(BukkitProgressBar.ProgressColor.valueOf(emptyColor.toUpperCase()));
        } catch (Exception e) {
            Logger.debug("Invalid empty color for " + options + " in configuration, valid values are: " + String.join(",", this.getAvailableColors()), LogType.WARNING);
        }
        try {
            options.setPercentColor(BukkitProgressBar.ProgressColor.valueOf(percentColor.toUpperCase()));
        } catch (Exception e) {
            Logger.debug("Invalid percent color for " + options + " in configuration, valid values are: " + String.join(",", this.getAvailableColors()), LogType.WARNING);
        }
        options.setProgressChar(progressChar);
        options.setEmptyChar(emptyChar);
        options.setBarWidth(barWidth);
    }

    private List<String> getAvailableColors() {
        List<String> colors = new ArrayList<>();
        for (BukkitProgressBar.ProgressColor color : BukkitProgressBar.ProgressColor.values()) {
            colors.add(color.name());
        }
        return colors;
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

    public static <T> T get(@NotNull ConfigurationKey key) {
        //noinspection unchecked
        return (T) configValues.getOrDefault(key, key.getDefaultValue());
    }
}
