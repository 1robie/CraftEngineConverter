package fr.robie.craftengineconverter.converter.nexo;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.common.CraftEngineImageUtils;
import fr.robie.craftengineconverter.common.ImageConversion;
import fr.robie.craftengineconverter.common.configuration.Configuration;
import fr.robie.craftengineconverter.common.enums.ConverterOptions;
import fr.robie.craftengineconverter.common.logger.LogType;
import fr.robie.craftengineconverter.common.logger.Logger;
import fr.robie.craftengineconverter.common.progress.BukkitProgressBar;
import fr.robie.craftengineconverter.converter.Converter;
import fr.robie.craftengineconverter.utils.ConfigFile;
import fr.robie.craftengineconverter.utils.SnakeUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NexoConverter extends Converter {
    public NexoConverter(CraftEngineConverter plugin) {
        super(plugin,"Nexo");
    }

    @Override
    public CompletableFuture<Void> convertItems(boolean async, Optional<Player> player){
        return executeTask(async, ()-> convertItemsSync(player));
    }

    private void convertItemsSync(Optional<Player> player) {
        File inputBase = new File("plugins/" + converterName + "/items");
        File outputBase = new File(this.plugin.getDataFolder(), "converted/"+converterName+"/CraftEngine/resources/craftengineconverter/configuration/items");

        if (!inputBase.exists() || !inputBase.isDirectory()) {
            Logger.info("Nexo items directory not found at: " + inputBase.getAbsolutePath());
            return;
        }

        if (outputBase.exists()) {
            deleteDirectory(outputBase);
        }

        if (!outputBase.mkdirs()) {
            Logger.debug("Failed to create Nexo items output directory", LogType.ERROR);
            return;
        }

        Queue<ConfigFile> toConvert = new LinkedList<>();
        populateQueue(inputBase, inputBase, toConvert);

        if (toConvert.isEmpty()) {
            return;
        }

        int totalItems = 0;
        for (ConfigFile configFile : toConvert) {
            totalItems += countItemsInConfig(configFile.config());
        }
        BukkitProgressBar.Builder progressBarBuilder;
        progressBarBuilder = new BukkitProgressBar.Builder(totalItems);

        if (player.isPresent()){
            progressBarBuilder.player(player.get());
            progressBarBuilder.showBar(false);
        }

        BukkitProgressBar progress = progressBarBuilder.prefix("Converting Nexo items")
                .suffix("items")
                .options(ConverterOptions.ITEMS)
                .updateInterval(5000)
                .build(plugin);

        progress.start();

        try {
            processConfigs(toConvert, outputBase, progress);
            toConvert.clear();
        } catch (Exception e) {
            Logger.showException("Error during Nexo items conversion", e);
        } finally {
            progress.stop();
        }
    }

    private int countItemsInConfig(YamlConfiguration config) {
        Set<String> keys = config.getKeys(false);
        return keys.size();
    }

    private void processConfigs(Queue<ConfigFile> toConvert, File outputBase, BukkitProgressBar progress) {
        for (ConfigFile configFile : toConvert) {
            processConfigFile(configFile, outputBase, progress);
        }
    }

    private void processConfigFile(ConfigFile configFile, File outputBase, BukkitProgressBar progress) {
        String fileName = configFile.sourceFile().getName();
        YamlConfiguration config = configFile.config();

        YamlConfiguration convertedConfig = new YamlConfiguration();
        ConfigurationSection items = convertedConfig.createSection("items");
        Set<String> keys = config.getKeys(false);
        List<String> itemsIds = new ArrayList<>();
        String finalFileName = fileName.substring(0, fileName.length() - 4);

        for (String itemId : keys) {
            ConfigurationSection section = config.getConfigurationSection(itemId);

            if (section == null) {
                progress.increment();
                continue;
            }

            String finalItemId = finalFileName + ":" + itemId;

            try {
                NexoItemConverter nexoItemConverter = new NexoItemConverter(
                        this,
                        section,
                        finalItemId,
                        items.createSection(finalItemId),
                        convertedConfig
                );

                nexoItemConverter.convertItem();

                if (!nexoItemConverter.isExcludeFromInventory()) {
                    itemsIds.add(finalItemId);
                }
            } catch (Exception e) {
                Logger.debug("Failed to convert item: " + finalItemId, LogType.ERROR);
            }

            progress.increment();
        }

        generateCategorie(itemsIds, convertedConfig, finalFileName);
        if (this.settings.dryRunEnabled()) return;
        try {
            Path relative = configFile.baseDir().toPath().relativize(configFile.sourceFile().toPath());
            File output = new File(outputBase, relative.toString());

            if (!output.getParentFile().exists()) {
                if (!output.getParentFile().mkdirs()) {
                    Logger.debug("Failed to create output directory for converted item file: " +
                            output.getParentFile().getAbsolutePath(), LogType.ERROR);
                }
            }

            convertedConfig.save(output);
        } catch (IOException e) {
            Logger.showException("Failed to save converted item file: " + fileName, e);
        } catch (IllegalArgumentException e) {
            Logger.showException("Failed to compute relative path for: " + configFile.sourceFile().getPath(), e);
        }
    }

    @Override
    public CompletableFuture<Void> convertEmojis(boolean async, Optional<Player> player){
        return executeTask(async, ()-> convertEmojisSync(player));
    }

    private void convertEmojisSync(Optional<Player> player) {
        File inputEmojisFolder = new File("plugins/" + converterName + "/glyphs");
        File outputEmojisFolder = new File(this.plugin.getDataFolder(), "converted/" + converterName + "/CraftEngine/resources/craftengineconverter/configuration/emojis");

        if (!inputEmojisFolder.exists() || !inputEmojisFolder.isDirectory()) {
            Logger.debug("Nexo emojis directory not found at: " + inputEmojisFolder.getAbsolutePath());
            return;
        }

        if (outputEmojisFolder.exists()) {
            deleteDirectory(outputEmojisFolder);
        }

        if (!outputEmojisFolder.mkdirs()) {
            Logger.debug("Failed to create Nexo emojis output directory", LogType.ERROR);
            return;
        }

        Queue<ConfigFile> toConvert = new LinkedList<>();
        populateQueue(inputEmojisFolder, inputEmojisFolder, toConvert);

        if (toConvert.isEmpty()) {
            Logger.info("No emojis found to convert.");
            return;
        }

        int totalEmojis = 0;
        for (ConfigFile configFile : toConvert) {
            totalEmojis += countItemsInConfig(configFile.config());
        }

        BukkitProgressBar.Builder progressBarBuilder = new BukkitProgressBar.Builder(totalEmojis);
        if (player.isPresent()){
            progressBarBuilder.player(player.get());
            progressBarBuilder.showBar(false);
        }

        BukkitProgressBar progress = progressBarBuilder
                .prefix("Converting Nexo emojis")
                .suffix("emojis")
                .options(ConverterOptions.EMOJIS)
                .updateInterval(5000)
                .build(plugin);

        progress.start();

        try {
            processEmojisConfigs(toConvert, outputEmojisFolder, progress);
            toConvert.clear();
        } catch (Exception e) {
            Logger.showException("Error during Nexo emojis conversion", e);
        } finally {
            progress.stop();
        }
    }

    private void processEmojisConfigs(Queue<ConfigFile> toConvert, File outputBaseDir, BukkitProgressBar progress) {
        for (ConfigFile configFile : toConvert) {
            convertEmojiFile(configFile, outputBaseDir, progress);
        }
    }

    private void convertEmojiFile(ConfigFile configFile, File outputBaseDir, BukkitProgressBar progress) {
        File emojiFile = configFile.sourceFile();
        YamlConfiguration config = configFile.config();

        Set<String> keys = config.getKeys(false);
        YamlConfiguration convertedConfig = new YamlConfiguration();
        ConfigurationSection convertedEmojiSection = convertedConfig.createSection("emoji");

        int convertedCount = 0;

        for (String key : keys) {
            ConfigurationSection emojiSection = config.getConfigurationSection(key);

            if (emojiSection == null) {
                progress.increment();
                continue;
            }

            String finalKey = "default:" + key;
            String permission = emojiSection.getString("permission");
            List<String> placeholders = emojiSection.getStringList("placeholders");

            if (placeholders.isEmpty()) {
                progress.increment();
                continue;
            }

            try {
                ConfigurationSection ceEmojiSection = convertedEmojiSection.createSection(finalKey);

                if (permission != null) {
                    ceEmojiSection.set("permission", permission);
                }

                ceEmojiSection.set("keywords", placeholders);

                int index = emojiSection.getInt("index", -1);
                int rows = emojiSection.getInt("rows", 0);
                int columns = emojiSection.getInt("columns", 0);

                if (index != -1 && rows != 0 && columns != 0) {
                    ceEmojiSection.set("image", finalKey + ":" + rows + ":" + columns);
                } else {
                    ceEmojiSection.set("image", finalKey + ":0:0");
                }

                CraftEngineImageUtils.register(key, new ImageConversion(finalKey, rows, columns));
                convertedCount++;
            } catch (Exception e) {
                Logger.debug("Failed to convert emoji: " + finalKey, LogType.ERROR);
            }

            progress.increment();
        }
        if (this.settings.dryRunEnabled()) return;
        if (convertedCount > 0) {
            try {
                Path relativePath = configFile.baseDir().toPath().relativize(emojiFile.toPath());
                File outputFile = new File(outputBaseDir, relativePath.toString());

                if (!outputFile.getParentFile().exists()) {
                    if (!outputFile.getParentFile().mkdirs()) {
                        Logger.debug("Failed to create output directory for emoji file: " +
                                outputFile.getParentFile().getAbsolutePath(), LogType.ERROR);
                    }
                }

                convertedConfig.save(outputFile);
            } catch (IOException e) {
                Logger.showException("Failed to save converted emoji file: " + emojiFile.getName(), e);
            }
        }
    }

    @Override
    public CompletableFuture<Void> convertImages(boolean async, Optional<Player> player) {
        return executeTask(async, ()-> this.convertImagesSync(player));
    }

    @Override
    public CompletableFuture<Void> convertLanguages(boolean async, Optional<Player> player) {
        return executeTask(async, ()->this.convertLanguagesSync(player));
    }

    @Override
    public CompletableFuture<Void> convertSounds(boolean async, Optional<Player> player) {
        return executeTask(async, ()->this.convertSoundsSync(player));
    }

    private void convertSoundsSync(Optional<Player> player) {
        File inputSoundFile = new File("plugins/" + converterName + "/sounds.yml");
        File outputSoundFile = new File(this.plugin.getDataFolder(), "converted/" + converterName + "/CraftEngine/resources/craftengineconverter/configuration/sounds/sounds.yml");

        if (!inputSoundFile.exists() || !inputSoundFile.isFile()) {
            Logger.debug("Nexo sounds file not found at: " + inputSoundFile.getAbsolutePath());
            return;
        }

        if (!outputSoundFile.getParentFile().exists()) {
            if (!outputSoundFile.getParentFile().mkdirs()) {
                Logger.info("Failed to create sounds output directory", LogType.ERROR);
                return;
            }
        }

        try (SnakeUtils nexoSounds = new SnakeUtils(inputSoundFile)) {
            if (nexoSounds.isEmpty()) {
                Logger.debug("Sounds file is empty: " + inputSoundFile.getAbsolutePath());
                return;
            }

            List<Map<String, Object>> nexoSoundsList = nexoSounds.getListMap("sounds");
            if (nexoSoundsList.isEmpty()) { // No sounds to convert
                nexoSounds.close();
                return;
            }

            int totalSounds = nexoSoundsList.size();

            BukkitProgressBar.Builder progressBarBuilder = new BukkitProgressBar.Builder(totalSounds);
            if (player.isPresent()){
                progressBarBuilder.player(player.get());
                progressBarBuilder.showBar(false);
            }

            BukkitProgressBar progress = progressBarBuilder
                    .prefix("Converting Nexo sounds")
                    .suffix("sounds")
                    .options(ConverterOptions.SOUNDS)
                    .updateInterval(5000)
                    .build(plugin);

            progress.start();

            try {
                File tempOutputFile = File.createTempFile("craftengine_sounds", ".yml");
                tempOutputFile.deleteOnExit();

                try (SnakeUtils craftEngineSounds = SnakeUtils.createEmpty(tempOutputFile)) {
                    SnakeUtils soundsSection = craftEngineSounds.getOrCreateSection("sounds");

                    for (Map<String, Object> soundEntry : nexoSoundsList) {
                        try {
                            convertSoundEntry(soundEntry, soundsSection, craftEngineSounds, progress);
                        } catch (Exception e) {
                            Object idObj = soundEntry.get("id");
                            String soundId = idObj != null ? idObj.toString() : "unknown";
                            Logger.debug("Failed to convert sound: " + soundId, LogType.ERROR);
                            progress.increment();
                        }
                    }
                    if (!this.settings.dryRunEnabled())
                        craftEngineSounds.save(outputSoundFile);
                }
            } catch (Exception e) {
                Logger.showException("Failed to copyFileWithProgress sounds file: " + inputSoundFile.getName(), e);
            } finally {
                nexoSounds.close();
                progress.stop();
            }
        } catch (Exception e) {
            Logger.showException("Failed to convert sounds file: " + inputSoundFile.getName(), e);
        }
    }

    private void convertSoundEntry(Map<String, Object> soundEntry, SnakeUtils soundsSection,
                                   SnakeUtils craftEngineSounds, BukkitProgressBar progress) {
        Object idObj = soundEntry.get("id");
        if (idObj == null) {
            progress.increment();
            return;
        }

        String soundId = idObj.toString();
        if (soundId.isEmpty()) {
            progress.increment();
            return;
        }

        SnakeUtils soundSection = soundsSection.getOrCreateSection(soundId);

        boolean replace = parseBoolean(soundEntry.get("replace"));
        if (replace) {
            soundSection.addData("replace", true);
        }

        List<Map<String, Object>> convertedSounds = new ArrayList<>();

        Object singleSound = soundEntry.get("sound");
        if (singleSound != null && isValidString(singleSound.toString())) {
            Map<String, Object> soundMap = createSoundMap(
                    singleSound.toString(),
                    soundEntry
            );
            convertedSounds.add(soundMap);
        }

        Object soundsListObj = soundEntry.get("sounds");
        if (soundsListObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> soundsList = (List<Object>) soundsListObj;
            for (Object soundObj : soundsList) {
                if (soundObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> soundMap = (Map<String, Object>) soundObj;
                    Object nameObj = soundMap.get("name");
                    if (nameObj == null) continue;

                    Map<String, Object> convertedSound = createSoundMap(
                            nameObj.toString(),
                            soundMap
                    );
                    convertedSounds.add(convertedSound);
                } else if (soundObj instanceof String) {
                    Map<String, Object> soundMap = createSoundMap(
                            soundObj.toString(),
                            soundEntry
                    );
                    convertedSounds.add(soundMap);
                }
            }
        }

        Object jukeboxPlayable = soundEntry.get("jukebox_playable");
        if (jukeboxPlayable instanceof Map<?, ?> jukeboxMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> finalJukeboxMap = (Map<String, Object>) jukeboxMap;
            SnakeUtils jukeboxSongsSection = craftEngineSounds.getOrCreateSection("jukebox-songs");
            SnakeUtils jukeboxSongSection = jukeboxSongsSection.getOrCreateSection(soundId);

            jukeboxSongSection.addData("sound", soundId);

            Object durationObj = finalJukeboxMap.get("duration");
            if (durationObj != null) {
                String durationStr = durationObj.toString();
                if (durationStr.endsWith("s")) {
                    try {
                        double length = Double.parseDouble(durationStr.substring(0, durationStr.length() - 1));
                        jukeboxSongSection.addData("length", length);
                    } catch (NumberFormatException e) {
                        Logger.debug("Invalid duration format: " + durationStr);
                    }
                }
            }

            Object descriptionObj = finalJukeboxMap.get("description");
            if (descriptionObj != null) {
                jukeboxSongSection.addData("description", descriptionObj.toString());
            }

            int comparatorOutput = parseInt(finalJukeboxMap.get("comparator_output"), 15);
            if (comparatorOutput != 15) {
                jukeboxSongSection.addData("comparator-output", comparatorOutput);
            }

            Object rangeObj = finalJukeboxMap.get("range");
            if (rangeObj != null) {
                int range = parseInt(rangeObj, 32);
                jukeboxSongSection.addData("range", range);
            }
        }

        if (!convertedSounds.isEmpty()) {
            soundSection.addData("sounds", convertedSounds);
        }

        progress.increment();
    }

    private Map<String, Object> createSoundMap(String soundName, Map<String, Object> properties) {
        Map<String, Object> soundMap = new LinkedHashMap<>();
        soundMap.put("name", soundName);

        boolean stream = parseBoolean(properties.get("stream"));
        if (stream) soundMap.put("stream", true);

        boolean preload = parseBoolean(properties.get("preload"));
        if (preload) soundMap.put("preload", true);

        double volume = parseDouble(properties.get("volume"), 1f);
        if (volume != 1.0) soundMap.put("volume", volume);

        double pitch = parseDouble(properties.get("pitch"), 1f);
        if (pitch != 1.0) soundMap.put("pitch", pitch);

        int weight = parseInt(properties.get("weight"), 1);
        if (weight != 1) soundMap.put("weight", weight);

        int attenuationDistance = parseInt(properties.get("attenuation_distance"), 16);
        if (attenuationDistance != 16) soundMap.put("attenuation_distance", attenuationDistance);

        return soundMap;
    }

    private void convertLanguagesSync(Optional<Player> player) {
        File languagesFile = new File("plugins/" + converterName + "/languages.yml");
        File outputFile = new File(this.plugin.getDataFolder(), "converted/" + converterName + "/CraftEngine/resources/craftengineconverter/configuration/languages/languages.yml");

        if (!languagesFile.exists() || !languagesFile.isFile()) {
            Logger.debug("Nexo languages file not found at: " + languagesFile.getAbsolutePath());
            return;
        }

        try (SnakeUtils nexoLanguages = new SnakeUtils(languagesFile)) {
            if (nexoLanguages.isEmpty()) {
                Logger.debug("Languages file is empty: " + languagesFile.getAbsolutePath());
                return;
            }

            Set<String> languageKeys = nexoLanguages.getKeys();
            if (languageKeys.isEmpty()) {
                Logger.debug("No languages found in file");
                return;
            }

            int totalTranslations = 0;
            for (String langKey : languageKeys) {
                Map<String, Object> langData = nexoLanguages.getMap(langKey);
                if (langData != null) {
                    totalTranslations += langData.size();
                }
            }

            if (totalTranslations == 0) {
                Logger.info("No translations found in languages file.");
                return;
            }

            BukkitProgressBar.Builder progressBarBuilder = new BukkitProgressBar.Builder(totalTranslations);
            if (player.isPresent()){
                progressBarBuilder.player(player.get());
                progressBarBuilder.showBar(false);
            }

            BukkitProgressBar progress = progressBarBuilder
                    .prefix("Converting Nexo languages")
                    .suffix("translations")
                    .options(ConverterOptions.LANGUAGES)
                    .updateInterval(5000)
                    .build(plugin);

            progress.start();

            try {
                File tempOutputFile = File.createTempFile("craftengine_languages", ".yml");
                tempOutputFile.deleteOnExit();

                try (SnakeUtils craftEngineLanguages = SnakeUtils.createEmpty(tempOutputFile)) {
                    for (String langKey : languageKeys) {
                        try {
                            convertLanguage(langKey, nexoLanguages, craftEngineLanguages, progress);
                        } catch (Exception e) {
                            Logger.debug("Failed to convert language: " + langKey, LogType.ERROR);
                            Map<String, Object> langData = nexoLanguages.getMap(langKey);
                            if (langData != null) {
                                progress.increment(langData.size());
                            }
                        }
                    }
                    if (!this.settings.dryRunEnabled())
                        craftEngineLanguages.save(outputFile);
                }
            } catch (Exception e) {
                Logger.showException("Failed to convert languages file: " + languagesFile.getName(), e);
            } finally {
                progress.stop();
            }
        } catch (Exception e) {
            Logger.showException("Failed to load languages file: " + languagesFile.getName(), e);
        }
    }

    private void convertLanguage(String langKey, SnakeUtils nexoLanguages,
                                 SnakeUtils craftEngineLanguages, BukkitProgressBar progress) {
        Map<String, Object> nexoLangData = nexoLanguages.getMap(langKey);

        if (nexoLangData == null || nexoLangData.isEmpty()) {
            return;
        }

        String craftEngineLangKey = langKey.equals("global") ? "en" : langKey;

        for (Map.Entry<String, Object> entry : nexoLangData.entrySet()) {
            try {
                String translationKey = "translations." + craftEngineLangKey + "." + entry.getKey();
                craftEngineLanguages.addData(translationKey, entry.getValue());
            } catch (Exception e) {
                Logger.debug("Failed to convert translation: " + entry.getKey() + " in language: " + langKey, LogType.ERROR);
            }

            progress.increment();
        }
    }


    private void convertImagesSync(Optional<Player> player) {
        File inputBase = new File("plugins/" + converterName + "/glyphs");
        File outputBase = new File(this.plugin.getDataFolder(), "converted/" + converterName + "/CraftEngine/resources/craftengineconverter/configuration/images");

        if (!inputBase.exists() || !inputBase.isDirectory()) {
            Logger.debug("Nexo glyph directory not found at: " + inputBase.getAbsolutePath());
            return;
        }

        if (outputBase.exists()) {
            deleteDirectory(outputBase);
        }

        if (!outputBase.mkdirs()) {
            Logger.debug("Failed to create Nexo images output directory", LogType.ERROR);
            return;
        }

        Queue<ConfigFile> toConvert = new LinkedList<>();
        populateQueue(inputBase, inputBase, toConvert);

        if (toConvert.isEmpty()) {
            Logger.info("No images found to convert.");
            return;
        }

        int totalImages = 0;
        for (ConfigFile configFile : toConvert) {
            totalImages += countItemsInConfig(configFile.config());
        }

        BukkitProgressBar.Builder progressBarBuilder = new BukkitProgressBar.Builder(totalImages);
        if (player.isPresent()){
            progressBarBuilder.player(player.get());
            progressBarBuilder.showBar(false);
        }

        BukkitProgressBar progress = progressBarBuilder
                .prefix("Converting Nexo images")
                .suffix("images")
                .options(ConverterOptions.IMAGES)
                .updateInterval(5000)
                .build(plugin);

        progress.start();

        try {
            processImagesConfigs(toConvert, outputBase, progress);
            toConvert.clear();
        } catch (Exception e) {
            Logger.showException("Error during Nexo images conversion", e);
        } finally {
            progress.stop();
        }
    }

    private void processImagesConfigs(Queue<ConfigFile> toConvert, File outputBase, BukkitProgressBar progress) {
        for (ConfigFile configFile : toConvert) {
            processImageFile(configFile, outputBase, progress);
        }
    }

    private void processImageFile(ConfigFile configFile, File outputBase, BukkitProgressBar progress) {
        String fileName = configFile.sourceFile().getName();
        YamlConfiguration config = configFile.config();

        YamlConfiguration convertedConfig = new YamlConfiguration();
        ConfigurationSection imagesSection = convertedConfig.createSection("images");
        Set<String> keys = config.getKeys(false);

        int convertedCount = 0;

        for (String key : keys) {
            ConfigurationSection imageSection = config.getConfigurationSection(key);

            if (imageSection == null) {
                progress.increment();
                continue;
            }

            try {
                String finalKey = "default:" + key;
                ConfigurationSection section = imagesSection.createSection(finalKey);

                String texture = imageSection.getString("texture");
                if (isValidString(texture)) {
                    section.set("file", namespaced(texture));
                }

                int ascent = imageSection.getInt("ascent", 0);
                if (ascent != 0) {
                    section.set("ascent", ascent);
                }

                int height = imageSection.getInt("height", 0);
                if (height != 0) {
                    section.set("height", height);
                }

                String font = imageSection.getString("font");
                if (isValidString(font)) {
                    section.set("font", font);
                }

                int rows = imageSection.getInt("rows", 0);
                int cols = imageSection.getInt("columns", 0);
                if (rows > 0 && cols > 0) {
                    section.set("grid-size", rows + "," + cols);
                }

                CraftEngineImageUtils.register(key, new ImageConversion(finalKey, rows, cols));
                convertedCount++;
            } catch (Exception e) {
                Logger.debug("Failed to convert image: " + key, LogType.ERROR);
            }

            progress.increment();
        }
        if (this.settings.dryRunEnabled()) return;
        if (convertedCount > 0) {
            try {
                Path relative = configFile.baseDir().toPath().relativize(configFile.sourceFile().toPath());
                File output = new File(outputBase, relative.toString());

                if (!output.getParentFile().exists()) {
                    if (!output.getParentFile().mkdirs()) {
                        Logger.debug("Failed to create output directory for converted image file: " +
                                output.getParentFile().getAbsolutePath(), LogType.ERROR);
                    }
                }

                convertedConfig.save(output);
            } catch (IOException e) {
                Logger.showException("Failed to save converted image file: " + fileName, e);
            } catch (IllegalArgumentException e) {
                Logger.showException("Failed to compute relative path for: " + configFile.sourceFile().getPath(), e);
            }
        }
    }

    @Override
    public CompletableFuture<Void> convertPack(boolean async, Optional<Player> player){
        return executeTask(async, ()-> convertPackSync(player));
    }

    private int countFilesInDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return 0;
        }

        int count = 0;
        File[] files = directory.listFiles();
        if (files == null) return 0;

        for (File file : files) {
            if (file.isDirectory()) {
                count += countFilesInDirectory(file);
            } else if (file.isFile()) {
                count++;
            }
        }

        return count;
    }

    private int countFilesInZip(File zipFile) {
        int count = 0;

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFile.toPath())))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                try {
                    validateZipEntryName(entry.getName());
                    if (!entry.isDirectory()) {
                        count++;
                    }
                } catch (IOException ignored) {
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            Logger.debug("Failed to count files in ZIP: " + zipFile.getName() + " - " + e.getMessage(), LogType.ERROR);
        }

        return count;
    }

    private void convertPackSync(Optional<Player> player) {
        ExecutorService executor = null;
        try {
            File inputPackFile = new File("plugins/" + converterName + "/pack");
            File outputPackFile = new File(this.plugin.getDataFolder(), "converted/"+converterName+"/CraftEngine/resources/craftengineconverter/resourcepack");

            if (!inputPackFile.exists() || !inputPackFile.isDirectory()) {
                Logger.info("Nexo pack directory not found at: " + inputPackFile.getAbsolutePath());
                return;
            }

            if (outputPackFile.exists()) {
                deleteDirectory(outputPackFile);
            }
            if (!outputPackFile.mkdirs()) {
                Logger.debug("Failed to create Nexo pack output directory", LogType.ERROR);
                return;
            }

            int totalFiles = 0;

            File mainAssetsFolder = new File(inputPackFile, "assets");
            totalFiles += countFilesInDirectory(mainAssetsFolder);

            File nexoExternalPacksFolder = new File(inputPackFile, "external_packs");
            if (nexoExternalPacksFolder.exists() && nexoExternalPacksFolder.isDirectory()) {
                File[] externalPacks = nexoExternalPacksFolder.listFiles();
                if (externalPacks != null) {
                    for (File externalPack : externalPacks) {
                        if (externalPack.isDirectory()) {
                            File externalPackAssetsFolder = new File(externalPack, "assets");
                            totalFiles += countFilesInDirectory(externalPackAssetsFolder);
                        } else if (externalPack.isFile() && externalPack.getName().endsWith(".zip")) {
                            totalFiles += countFilesInZip(externalPack);
                        }
                    }
                }
            }

            BukkitProgressBar.Builder progressBarBuilder = new BukkitProgressBar.Builder(totalFiles);
            if (player.isPresent()){
                progressBarBuilder.player(player.get());
                progressBarBuilder.showBar(false);
            }
            BukkitProgressBar progress = progressBarBuilder
                    .prefix("Converting Nexo resource pack")
                    .suffix("files")
                    .options(ConverterOptions.PACKS)
                    .updateInterval(5000)
                    .build(plugin);
            progress.start();

            int threadCount = Math.max(1, this.getSettings().threadCount());
            boolean useMultiThread = threadCount > 1;

            if (useMultiThread) {
                executor = Executors.newFixedThreadPool(threadCount);
            }
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Exception> errorRef = new AtomicReference<>();

            try {
                File outputAssetsFolder = new File(outputPackFile, "assets");

                copyAssetsFolder(new File(inputPackFile, "assets"), outputAssetsFolder, "main", progress, executor, latch, errorRef, useMultiThread);

                if (nexoExternalPacksFolder.exists() && nexoExternalPacksFolder.isDirectory()) {
                    File[] externalPacks = nexoExternalPacksFolder.listFiles();
                    if (externalPacks != null) {
                        for (File externalPack : externalPacks) {
                            if (externalPack.isDirectory()) {
                                File externalPackAssetsFolder = new File(externalPack, "assets");
                                copyAssetsFolder(externalPackAssetsFolder, outputAssetsFolder, externalPack.getName(), progress, executor, latch, errorRef, useMultiThread);
                            } else if (externalPack.isFile() && externalPack.getName().endsWith(".zip")) {
                                extractAndCopyZipAssets(externalPack, outputAssetsFolder, externalPack.getName().replace(".zip", ""), progress, executor, latch, errorRef, useMultiThread);
                            }
                        }
                    }
                }

                if (useMultiThread) {
                    latch.countDown();
                    executor.shutdown();
                    if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                        Logger.debug("Timeout waiting for file operations to complete", LogType.ERROR);
                        Logger.debug("Forcing shutdown of file operation threads", LogType.ERROR);
                    }
                }

                if (errorRef.get() != null) {
                    throw errorRef.get();
                }

            } finally {
                progress.stop();
                if (executor != null && !executor.isShutdown()) {
                    executor.shutdownNow();
                }
            }
        } catch (Exception e) {
            Logger.showException("Error during Nexo pack conversion", e);
        } finally {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }
        }
    }

    private void copyAssetsFolder(File assetsFolder, File outputAssetsFolder, String packName,
                                  BukkitProgressBar progress, ExecutorService executor,
                                  CountDownLatch latch, AtomicReference<Exception> errorRef,
                                  boolean useMultiThread) {
        if (!assetsFolder.exists() || !assetsFolder.isDirectory()) {
            Logger.debug("Assets folder not found for pack '" + packName + "' at: " + assetsFolder.getAbsolutePath());
            return;
        }

        try {
            copyDirectory(assetsFolder, outputAssetsFolder, assetsFolder, progress, executor, latch, errorRef, useMultiThread);
        } catch (IOException e) {
            Logger.info("Failed to copy assets from " + packName + " pack: " + e.getMessage(), LogType.ERROR);
            errorRef.compareAndSet(null, e);
        }
    }

    private void copyDirectory(File source, File destination, File assetsRoot,
                               BukkitProgressBar progress, ExecutorService executor,
                               CountDownLatch latch, AtomicReference<Exception> errorRef,
                               boolean useMultiThread) throws IOException {
        if (!this.settings.dryRunEnabled() && !destination.exists() && !destination.mkdirs()) {
            Logger.debug("Failed to create destination directory: " + destination.getAbsolutePath(), LogType.ERROR);
            return;
        }

        File[] files = source.listFiles();
        if (files == null) return;

        for (File file : files) {
            Path relativePath = assetsRoot.toPath().relativize(file.toPath());
            String relativePathStr = relativePath.toString().replace("\\", "/");

            String[] parts = relativePathStr.split("/", 2);
            String namespace = parts[0];
            String pathInNamespace = parts.length > 1 ? parts[1] : "";

            String fullPath = namespace + ":" + pathInNamespace;

            if (Configuration.isPathBlacklisted(fullPath)) {
                if (file.isFile()) {
                    progress.increment();
                }
                continue;
            }

            if (file.isFile()) {
                String fullPathWithFile = namespace + ":" + pathInNamespace + "/" + file.getName();
                if (Configuration.isPathBlacklisted(fullPathWithFile)) {
                    progress.increment();
                    continue;
                }
            }

            PackMapping resolvedMapping = resolvePackMapping(namespace, pathInNamespace);

            File targetFile;
            if (resolvedMapping != null) {
                String mappedFullPath = resolvedMapping.namespaceTarget() + "/" + resolvedMapping.targetPath();

                if (file.isFile()) {
                    targetFile = new File(destination, mappedFullPath + "/" + file.getName());
                } else {
                    targetFile = new File(destination, mappedFullPath);
                }
            } else {
                targetFile = new File(destination, relativePathStr);
            }

            if (file.isDirectory()) {
                if (!this.settings.dryRunEnabled() && !targetFile.exists() && !targetFile.mkdirs()) {
                    Logger.debug("Failed to create target directory: " + targetFile.getAbsolutePath(), LogType.ERROR);
                }

                if (resolvedMapping != null) {
                    copyDirectoryContents(file, targetFile, progress, executor, latch, errorRef, useMultiThread);
                } else {
                    copyDirectory(file, destination, assetsRoot, progress, executor, latch, errorRef, useMultiThread);
                }
            } else {
                copyFileWithProgress(progress, executor, latch, errorRef, useMultiThread, file, targetFile);
            }
        }
    }

    private void copyFileWithProgress(BukkitProgressBar progress, ExecutorService executor, CountDownLatch latch, AtomicReference<Exception> errorRef, boolean useMultiThread, File file, File targetFile) throws IOException {
        if (useMultiThread) {
            executor.submit(() -> {
                try {
                    latch.await();
                    if (!this.settings.dryRunEnabled() && !targetFile.getParentFile().exists()
                            && !targetFile.getParentFile().mkdirs()) {
                        Logger.debug("Failed to create parent directory for file: " + targetFile.getAbsolutePath(), LogType.ERROR);
                    }
                    copyFile(file, targetFile);
                    progress.increment();
                } catch (Exception e) {
                    Logger.debug("Error copying file: " + file.getName() + " - " + e.getMessage(), LogType.ERROR);
                    errorRef.compareAndSet(null, e);
                }
            });
        } else {
            if (!this.settings.dryRunEnabled() && !targetFile.getParentFile().exists()
                    && !targetFile.getParentFile().mkdirs()) {
                Logger.debug("Failed to create parent directory for file: " + targetFile.getAbsolutePath(), LogType.ERROR);
            }
            copyFile(file, targetFile);
            progress.increment();
        }
    }

    private void copyDirectoryContents(File source, File destination, BukkitProgressBar progress,
                                       ExecutorService executor, CountDownLatch latch,
                                       AtomicReference<Exception> errorRef, boolean useMultiThread) throws IOException {
        if (!this.settings.dryRunEnabled() && !destination.exists() && !destination.mkdirs()) {
            Logger.debug("Failed to create destination directory: " + destination.getAbsolutePath(), LogType.ERROR);
            return;
        }

        File[] files = source.listFiles();
        if (files == null) return;

        for (File file : files) {
            File targetFile = new File(destination, file.getName());

            if (file.isDirectory()) {
                copyDirectoryContents(file, targetFile, progress, executor, latch, errorRef, useMultiThread);
            } else {
                copyFileWithProgress(progress, executor, latch, errorRef, useMultiThread, file, targetFile);
            }
        }
    }

    private void extractAndCopyZipAssets(File zipFile, File outputAssetsFolder, String packName,
                                         BukkitProgressBar progress, ExecutorService executor,
                                         CountDownLatch latch, AtomicReference<Exception> errorRef,
                                         boolean useMultiThread) {
        File tempDir = new File(this.plugin.getDataFolder(), "temp/zip_extract_" + System.currentTimeMillis());

        if (!this.settings.dryRunEnabled() && !tempDir.exists() && !tempDir.mkdirs()) {
            Logger.debug("Failed to create temporary directory for ZIP extraction: " + tempDir.getAbsolutePath(), LogType.ERROR);
            return;
        }

        try {
            extractZip(zipFile.toPath(), tempDir.toPath(), progress, executor, latch, errorRef, useMultiThread);

            File extractedAssetsFolder = new File(tempDir, "assets");
            if (extractedAssetsFolder.exists() && extractedAssetsFolder.isDirectory()) {
                copyAssetsFolder(extractedAssetsFolder, outputAssetsFolder, packName, progress, executor, latch, errorRef, useMultiThread);
            } else if (!this.settings.dryRunEnabled()) {
                Logger.debug("No assets folder found in ZIP: " + zipFile.getName());
            }

            if (!this.settings.dryRunEnabled()) {
                deleteDirectory(tempDir);
            }
        } catch (IOException e) {
            Logger.showException("Failed to extract and copy assets from ZIP: " + zipFile.getName(), e);
            errorRef.compareAndSet(null, e);
        } finally {
            if (!this.settings.dryRunEnabled() && tempDir.exists()) {
                deleteDirectory(tempDir);
            }
        }
    }

    private void extractZip(Path zipPath, Path targetDir, BukkitProgressBar progress,
                            ExecutorService executor, CountDownLatch latch,
                            AtomicReference<Exception> errorRef, boolean useMultiThread) throws IOException {
        if (this.settings.dryRunEnabled()) {
            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipPath)))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        progress.increment();
                    }
                    zis.closeEntry();
                }
            }
            return;
        }

        Files.createDirectories(targetDir);
        File canonicalTargetDir = targetDir.toFile().getCanonicalFile();

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipPath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = validateZipEntryName(entry.getName());
                File destinationFile = new File(canonicalTargetDir, entryName);

                File canonicalDestination = destinationFile.getCanonicalFile();

                if (!canonicalDestination.toPath().startsWith(canonicalTargetDir.toPath())) {
                    throw new IOException("Entry outside target: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(canonicalDestination.toPath());
                    zis.closeEntry();
                    continue;
                }

                Files.createDirectories(canonicalDestination.getParentFile().toPath());

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] tempBuffer = new byte[8192];
                int len;
                while ((len = zis.read(tempBuffer)) > 0) {
                    buffer.write(tempBuffer, 0, len);
                }
                byte[] fileContent = buffer.toByteArray();

                Path finalPath = canonicalDestination.toPath();
                if (useMultiThread) {
                    executor.submit(() -> {
                        try {
                            latch.await();
                            try (OutputStream out = Files.newOutputStream(finalPath,
                                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                                out.write(fileContent);
                            }
                            progress.increment();
                        } catch (Exception e) {
                            Logger.debug("Error extracting file from ZIP: " + entryName + " - " + e.getMessage(), LogType.ERROR);
                            errorRef.compareAndSet(null, e);
                        }
                    });
                } else {
                    try (OutputStream out = Files.newOutputStream(finalPath,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        out.write(fileContent);
                    }
                    progress.increment();
                }

                zis.closeEntry();
            }
        }
    }

    private void copyFile(File source, File destination) throws IOException {
        if (this.settings.dryRunEnabled()) return;
        Files.copy(
            source.toPath(),
            destination.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        );
    }

    private String validateZipEntryName(@Nullable String entryName) throws IOException {
        // Reject null or empty names
        if (entryName == null || entryName.isEmpty()) {
            throw new IOException("Invalid zip entry: empty name");
        }

        // Decode URL encoding to catch obfuscated attacks like "..%2F..%2Fetc%2Fpasswd"
        String decoded;
        try {
            decoded = java.net.URLDecoder.decode(entryName, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            decoded = entryName; // Keep original if decoding fails
        }

        // Block UNC paths: \\server\share\file
        if (entryName.startsWith("\\\\") || decoded.startsWith("\\\\")) {
            throw new IOException("Invalid zip entry: UNC path detected - " + entryName);
        }

        // Block network paths: //server/share/file
        if (entryName.startsWith("//") || decoded.startsWith("//")) {
            throw new IOException("Invalid zip entry: network path detected - " + entryName);
        }

        // Block absolute paths: /etc/passwd or \Windows\System32
        if (entryName.startsWith("/") || entryName.startsWith("\\") ||
                decoded.startsWith("/") || decoded.startsWith("\\")) {
            throw new IOException("Invalid zip entry: absolute path - " + entryName);
        }

        // Block Windows drive letters: C:\file or D:/document
        if ((entryName.length() >= 2 && entryName.charAt(1) == ':') ||
                (decoded.length() >= 2 && decoded.charAt(1) == ':')) {
            throw new IOException("Invalid zip entry: drive letter - " + entryName);
        }

        // Normalize path separators for consistent checking
        String normalized = entryName.replace("\\", "/");
        String decodedNormalized = decoded.replace("\\", "/");

        // Block parent directory references: ../../../etc/passwd
        if (normalized.contains("../") || normalized.contains("/..") || normalized.equals("..") ||
                decodedNormalized.contains("../") || decodedNormalized.contains("/..") || decodedNormalized.equals("..")) {
            throw new IOException("Invalid zip entry: parent reference - " + entryName);
        }

        return entryName;
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else if (!file.delete()){
                    Logger.debug("Failed to delete file: " + file.getAbsolutePath(), LogType.ERROR);
                }
            }
        }
        if (!directory.delete()){
            Logger.debug("Failed to delete directory: " + directory.getAbsolutePath(), LogType.ERROR);
        }
    }

    public void generateCategorie(List<String> itemsIds, YamlConfiguration config, String fileName) {
        if (itemsIds.isEmpty()) return;
        ConfigurationSection categoriesSection = config.createSection("categories");
        ConfigurationSection categorySection = categoriesSection.createSection(itemsIds.getFirst());
        categorySection.set("name", (Configuration.disableDefaultItalic? "<!i>":"") + "Category "+fileName);
        categorySection.set("icon", itemsIds.getFirst());
        categorySection.set("list", itemsIds);
    }
}
