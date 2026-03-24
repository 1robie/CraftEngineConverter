package fr.robie.craftengineconverter.loader;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import fr.robie.craftengineconverter.api.enums.Languages;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.format.MessageType;
import fr.robie.craftengineconverter.api.logger.LogType;
import fr.robie.craftengineconverter.api.logger.Logger;
import fr.robie.craftengineconverter.api.utils.ObjectUtils;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.manager.Manageable;
import fr.robie.craftengineconverter.common.utils.SnakeUtils;
import org.bukkit.configuration.file.YamlConfiguration;

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
            if (VERSION_KEY.equals(key)) {
                continue;
            }
            if (config.isConfigurationSection(key)) {
                continue;
            }
            String rootKey = this.resolveRootKey(key, validKeys);
            if (rootKey == null) {
                obsoleteKeys.add(key);
            }
        }

        if (obsoleteKeys.isEmpty()) {
            return false;
        }

        this.backupFile(file, lang);

        for (String key : obsoleteKeys) {
            config.set(key, null);
        }

        this.removeEmptySections(config);

        Logger.info(
                "Removed " + obsoleteKeys.size() + " obsolete key(s) from language file '" + lang.name() + "': " + obsoleteKeys,
                LogType.WARNING
        );
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

    private String resolveRootKey(String key, Set<String> validKeys) {
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

        String timestamp = LocalDateTime.now().format(BACKUP_DATE_FORMAT);
        String backupName = lang.name().toLowerCase() + "_messages_" + timestamp + ".yml";
        File backupFile = new File(backupDir, backupName);

        try {
            Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Logger.info("Backed up language file '" + lang.name() + "' to: " + backupFile.getPath(), LogType.INFO);
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

                    if (!config.contains(key)) {
                        if (reader.contains(key)) {
                            config.set(key, reader.getObject(key));
                        } else {
                            Logger.info(
                                    "Missing message key for language " + lang.name() + ": " + key
                                            + ". Please report this. Using default message.",
                                    LogType.WARNING
                            );
                            MessageType type = message.getType();
                            if (type != MessageType.TCHAT) {
                                config.set(key + ".type", type.name());
                            }
                            if (type == MessageType.TITLE) {
                                config.set(key + ".title", message.getTitle());
                                config.set(key + ".subtitle", message.getSubTitle());
                                config.set(key + ".fade-in", message.getStart());
                                config.set(key + ".show-time", message.getTime());
                                config.set(key + ".fade-out", message.getEnd());
                            } else {
                                if (message.isMessage()) {
                                    config.set(
                                            key + (type != MessageType.TCHAT ? ".messages" : ""),
                                            message.getMessages()
                                    );
                                } else {
                                    config.set(
                                            key + (type != MessageType.TCHAT ? ".message" : ""),
                                            message.getMessage()
                                    );
                                }
                            }
                        }
                        updated = true;
                    }
                }
            }
        }
        return updated;
    }

    public void loadLanguage(Languages language) {
        File file = new File(this.plugin.getDataFolder(), this.getLanguagePath(language));

        if (!file.exists()) {
            Logger.info("Language file not found: " + file.getPath(), LogType.WARNING);
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Set<String> keys = config.getKeys(true);
        List<Message> loadedMessages = new ArrayList<>();

        for (String key : keys) {
            if (VERSION_KEY.equals(key) || config.isConfigurationSection(key)) {
                continue;
            }

            try {
                Message message = this.parseMessage(key);
                this.loadMessageContent(message, config, key);
                loadedMessages.add(message);
            } catch (IllegalArgumentException e) {
                Logger.info("Unknown message key: " + key, LogType.WARNING);
            } catch (Exception e) {
                Logger.showException("Failed to load message: " + key, e);
            }
        }

        this.validateLoadedMessages(loadedMessages, language);
    }

    private Message parseMessage(String key) {
        return Message.valueOf(this.keyToEnumName(key));
    }

    private void loadMessageContent(Message message, YamlConfiguration config, String key) {
        if (config.contains(key + ".type")) {
            this.loadTypedMessage(message, config, key);
        } else {
            this.loadSimpleMessage(message, config, key);
        }
    }

    private void loadTypedMessage(Message message, YamlConfiguration config, String key) {
        MessageType messageType = MessageType.valueOf(
                config.getString(key + ".type", "TCHAT").toUpperCase()
        );
        message.setType(messageType);

        switch (messageType) {
            case ACTION, TCHAT_AND_ACTION -> this.loadActionMessage(message, config, key);
            case CENTER, TCHAT, WITHOUT_PREFIX -> this.loadTextMessage(message, config, key);
            case TITLE -> this.loadTitleMessage(message, config, key);
        }
    }

    private void loadActionMessage(Message message, YamlConfiguration config, String key) {
        message.setMessage(config.getString(key + ".message"));
    }

    private void loadTextMessage(Message message, YamlConfiguration config, String key) {
        List<String> messages = config.getStringList(key + ".messages");
        if (messages.isEmpty()) {
            message.setMessage(config.getString(key + ".message"));
        } else {
            message.setMessages(messages);
        }
    }

    private void loadTitleMessage(Message message, YamlConfiguration config, String key) {
        Map<String, Object> titles = new HashMap<>();
        titles.put("title", config.getString(key + ".title"));
        titles.put("subtitle", config.getString(key + ".subtitle"));
        titles.put("start", config.getInt(key + ".fade-in", 10));
        titles.put("time", config.getInt(key + ".show-time", 70));
        titles.put("end", config.getInt(key + ".fade-out", 20));
        titles.put("isUse", true);
        message.setTitles(titles);
    }

    private void loadSimpleMessage(Message message, YamlConfiguration config, String key) {
        message.setType(MessageType.TCHAT);
        List<String> messages = config.getStringList(key);
        if (messages.isEmpty()) {
            message.setMessage(config.getString(key));
        } else {
            message.setMessages(messages);
        }
    }

    private void validateLoadedMessages(List<Message> loadedMessages, Languages language) {
        if (loadedMessages.size() != Message.values().length) {
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