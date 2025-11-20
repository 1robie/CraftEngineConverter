package fr.robie.craftEngineConverter.core.utils;

import fr.robie.craftEngineConverter.core.utils.logger.LogType;
import fr.robie.craftEngineConverter.core.utils.logger.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.function.Consumer;

public class Configuration {
    public static boolean enableDebug = false;
    public static Material defaultMaterial = Material.PAPER;

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

    public void load(YamlConfiguration config, File file) {
        for (ConfigPath configPath : ConfigPath.values()) {
            Object value;
            switch (configPath.getDefaultValue()) {
                case Boolean b -> value = getOrAddBoolean(config, configPath.getPath(), b);
                case Integer i -> value = getOrAddInt(config, configPath.getPath(), i);
                case String s -> value = getOrAddString(config, configPath.getPath(), s);
                case Long l -> value = getOrAddLong(config, configPath.getPath(), l);
                case null, default -> {
                    continue;
                }
            }
            configPath.assign(value);
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

    public enum ConfigPath {
        ENABLE_DEBUG("enable-debug", false, v -> Configuration.enableDebug = (Boolean) v),
        DEFAULT_MATERIAL("default-material", "PAPER", v -> {
            try {
                String string = (String) v;
                Configuration.defaultMaterial = Material.valueOf(string.toUpperCase());
            } catch (Exception e) {
                Logger.debug("Invalid default material in configuration, using PAPER as default.", LogType.WARNING);
                Configuration.defaultMaterial = Material.PAPER;
            }
        }),
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
