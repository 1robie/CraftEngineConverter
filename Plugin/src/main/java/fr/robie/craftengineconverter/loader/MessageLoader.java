package fr.robie.craftengineconverter.loader;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import fr.robie.craftengineconverter.api.enums.Languages;
import fr.robie.craftengineconverter.api.format.CraftEngineConverterMessage;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.format.MessageType;
import fr.robie.craftengineconverter.api.format.message.BossBarMessage;
import fr.robie.craftengineconverter.api.format.message.ClassicMessage;
import fr.robie.craftengineconverter.api.format.message.TitleMessage;
import fr.robie.craftengineconverter.api.logger.LogType;
import fr.robie.craftengineconverter.api.logger.Logger;
import fr.robie.craftengineconverter.api.utils.ObjectUtils;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.manager.Manageable;
import fr.robie.craftengineconverter.common.utils.SnakeUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MessageLoader extends ObjectUtils implements Manageable {
    private static final String TRANSLATIONS_PATH = "translations/";
    private static final String MESSAGES_FILE = "/messages.yml";
    private static final String BACKUP_FOLDER = "translations/backup/";
    private static final String VERSION_KEY = "version";
    private static final DateTimeFormatter BACKUP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final CraftEngineConverterPlugin plugin;
    private final int version = 2;

    public MessageLoader(CraftEngineConverterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        for (Languages lang : Languages.values()) {
            String path = this.getLanguagePath(lang);
            File file = new File(this.plugin.getDataFolder(), path);
            try {
                this.ensureFileExists(file, path);
                this.updateLanguageFile(file, path, lang);
            } catch (Exception e) {
                Logger.showException("Failed to process language file: " + path, e);
            }
        }
        this.loadLanguage(Configuration.get(ConfigurationKey.LANGUAGE));
    }

    private void ensureFileExists(File file, String path) {
        if (!file.exists()) {
            this.plugin.saveResource(path, false);
        }
    }

    private void updateLanguageFile(File file, String path, Languages lang) throws Exception {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Object fileVersion = config.get(VERSION_KEY);

        boolean needsSave = this.removeObsoleteKeys(config, file, lang);

        if (!(fileVersion instanceof Integer intVersion) || intVersion < this.version) {
            config.set(VERSION_KEY, this.version);
            if (this.updateMissingKeys(config, path, lang)) {
                needsSave = true;
            }
        }

        if (needsSave) {
            config.save(file);
        }
    }

    private boolean removeObsoleteKeys(YamlConfiguration config, File file, Languages lang) {
        Set<String> validKeys = this.buildValidKeySet();
        List<String> obsoleteKeys = new ArrayList<>();

        for (String key : config.getKeys(true)) {
            if (VERSION_KEY.equals(key) || config.isConfigurationSection(key)) {
                continue;
            }
            if (this.resolveRootKey(key, validKeys) == null) {
                obsoleteKeys.add(key);
            }
        }

        if (obsoleteKeys.isEmpty()) {
            return false;
        }

        this.backupFile(file, lang);
        obsoleteKeys.forEach(key -> config.set(key, null));
        this.removeEmptySections(config);

        Logger.info("Removed " + obsoleteKeys.size() + " obsolete key(s) from language file '" + lang.name() + "': " + obsoleteKeys, LogType.WARNING);
        return true;
    }

    private void removeEmptySections(YamlConfiguration config) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String key : new ArrayList<>(config.getKeys(true))) {
                if (config.isConfigurationSection(key) && config.getConfigurationSection(key).getKeys(false).isEmpty()) {
                    config.set(key, null);
                    changed = true;
                }
            }
        }
    }

    private Set<String> buildValidKeySet() {
        Set<String> keys = new HashSet<>();
        keys.add(VERSION_KEY);
        for (Message message : Message.values()) {
            keys.add(this.enumNameToKey(message.name()));
        }
        return keys;
    }

    private @Nullable String resolveRootKey(String key, Set<String> validKeys) {
        if (validKeys.contains(key)) {
            return key;
        }
        int lastDot = key.lastIndexOf('.');
        while (lastDot > 0) {
            String parent = key.substring(0, lastDot);
            if (validKeys.contains(parent)) {
                return parent;
            }
            lastDot = parent.lastIndexOf('.');
        }
        return null;
    }

    private void backupFile(File file, Languages lang) {
        File backupDir = new File(this.plugin.getDataFolder(), BACKUP_FOLDER);
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            Logger.info("Failed to create backup directory: " + backupDir.getPath(), LogType.WARNING);
            return;
        }
        String backupName = lang.name().toLowerCase() + "_messages_" + LocalDateTime.now().format(BACKUP_DATE_FORMAT) + ".yml";
        try {
            Files.copy(file.toPath(), new File(backupDir, backupName).toPath(), StandardCopyOption.REPLACE_EXISTING);
            Logger.info("Backed up language file '" + lang.name() + "' to: " + backupName, LogType.INFO);
        } catch (IOException e) {
            Logger.showException("Failed to back up language file: " + file.getPath(), e);
        }
    }

    private boolean updateMissingKeys(YamlConfiguration config, String path, Languages lang) throws Exception {
        boolean updated = false;
        try (InputStream inputStream = this.plugin.getResource(path)) {
            if (inputStream == null) {
                Logger.info("Language file not found in resources: " + path);
                return false;
            }
            try (SnakeUtils reader = new SnakeUtils(inputStream)) {
                for (Message message : Message.values()) {
                    String key = this.enumNameToKey(message.name());
                    if (config.contains(key)) {
                        continue;
                    }

                    if (reader.contains(key)) {
                        config.set(key, reader.getObject(key));
                    } else {
                        Logger.info(
                                "Missing key for language " + lang.name() + ": " + key + ". Please report this.",
                                LogType.WARNING
                        );
                        config.set(key, this.buildDefaultEntry(message));
                    }
                    updated = true;
                }
            }
        }
        return updated;
    }

    private Object buildDefaultEntry(Message message) {
        List<CraftEngineConverterMessage> defaults = message.getDefaults();

        if (defaults.size() == 1 && defaults.getFirst() instanceof ClassicMessage(
                MessageType messageType, List<String> messages
        ) && messages != null && messageType == MessageType.TCHAT) {
            return messages.size() == 1
                    ? messages.getFirst()
                    : messages;
        }

        List<Map<String, Object>> entries = new ArrayList<>();
        for (CraftEngineConverterMessage craftMessage : defaults) {
            Map<String, Object> entry = new LinkedHashMap<>(craftMessage.serialize());
            entry.put("type", craftMessage.messageType().name());
            entries.add(entry);
        }
        return entries.size() == 1 ? entries.getFirst() : entries;
    }

    public void loadLanguage(Languages language) {
        File file = new File(this.plugin.getDataFolder(), this.getLanguagePath(language));
        if (!file.exists()) {
            Logger.info("Language file not found: " + file.getPath(), LogType.WARNING);
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Message> loadedMessages = new ArrayList<>();

        for (Message message : Message.values()) {
            try {
                String key = this.enumNameToKey(message.name());
                if (config.contains(key)) {
                    message.setCraftMessages(this.parseMessageList(config, key));
                    loadedMessages.add(message);
                } else {
                    Logger.info("Missing message key in config: " + key, LogType.WARNING);
                }
            } catch (Exception e) {
                Logger.showException("Failed to load message: " + message.name(), e);
            }
        }

        this.validateLoadedMessages(loadedMessages, language);
    }

    private List<CraftEngineConverterMessage> parseMessageList(YamlConfiguration config, String key) {
        Object raw = config.get(key);
        switch (raw) {
            case null -> {
                Logger.info("Message key '" + key + "' is null — skipping.", LogType.WARNING);
                return List.of();
            }
            case String str -> {
                return List.of(new ClassicMessage(MessageType.TCHAT, List.of(str)));
            }
            case List<?> list -> {
                if (list.isEmpty()) {
                    return List.of();
                }

                if (list.getFirst() instanceof String) {
                    List<String> lines = list.stream()
                            .filter(e -> e instanceof String)
                            .map(e -> (String) e)
                            .toList();
                    return List.of(new ClassicMessage(MessageType.TCHAT, lines));
                }

                if (list.getFirst() instanceof Map<?, ?>) {
                    List<CraftEngineConverterMessage> result = new ArrayList<>();
                    for (Object entry : list) {
                        if (entry instanceof Map<?, ?> map) {
                            YamlConfiguration section = new YamlConfiguration();
                            for (Map.Entry<?, ?> e : map.entrySet()) {
                                section.set(String.valueOf(e.getKey()), e.getValue());
                            }
                            CraftEngineConverterMessage craftMessage = this.parseCraftMessage(section, key);
                            if (craftMessage != null) {
                                result.add(craftMessage);
                            }
                        }
                    }
                    return result;
                }
            }
            default -> {
            }
        }

        if (raw instanceof ConfigurationSection section) {
            CraftEngineConverterMessage craftMessage = this.parseCraftMessage(section, key);
            return craftMessage != null ? List.of(craftMessage) : List.of();
        }

        if (raw instanceof Map<?, ?> map) {
            YamlConfiguration section = new YamlConfiguration();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                section.set(String.valueOf(e.getKey()), e.getValue());
            }
            CraftEngineConverterMessage craftMessage = this.parseCraftMessage(section, key);
            return craftMessage != null ? List.of(craftMessage) : List.of();
        }

        Logger.info("Message key '" + key + "' has unsupported format: " + raw.getClass().getSimpleName() + " — skipping.", LogType.WARNING);
        return List.of();
    }

    @Nullable
    private CraftEngineConverterMessage parseCraftMessage(@NotNull ConfigurationSection section, String debugKey) {
        MessageType messageType = MessageType.TCHAT;
        if (section.contains("type")) {
            try {
                messageType = MessageType.valueOf(section.getString("type", "TCHAT").toUpperCase());
            } catch (IllegalArgumentException e) {
                Logger.info("Unknown message type '" + section.getString("type") + "' in: " + debugKey, LogType.WARNING);
                return null;
            }
        }

        return switch (messageType) {
            case TITLE -> TitleMessage.deserialize(section.getValues(false));
            case BOSS_BAR -> BossBarMessage.deserialize(section.getValues(false));
            case TCHAT, ACTION_BAR, TCHAT_AND_ACTION_BAR, WITHOUT_PREFIX, NONE ->
                    ClassicMessage.deserialize(messageType, section.getValues(false));
        };
    }

    private void validateLoadedMessages(List<Message> loadedMessages, Languages language) {
        if (loadedMessages.size() == Message.values().length) {
            return;
        }

        Set<Message> loaded = new HashSet<>(loadedMessages);
        List<String> missing = Arrays.stream(Message.values())
                .filter(m -> !loaded.contains(m))
                .map(m -> this.enumNameToKey(m.name()))
                .toList();

        Logger.info(String.format(
                "Loaded messages (%d) do not match expected count (%d) for language %s. Missing keys: %s",
                loadedMessages.size(), Message.values().length, language.name(), missing
        ), LogType.WARNING);
    }

    private String getLanguagePath(Languages language) {
        return TRANSLATIONS_PATH + language.name().toLowerCase() + MESSAGES_FILE;
    }

    private String keyToEnumName(String key) {
        return key.toUpperCase().replace(".", "__").replace("-", "_");
    }

    private String enumNameToKey(String enumName) {
        return enumName.toLowerCase().replace("__", ".").replace("_", "-");
    }
}