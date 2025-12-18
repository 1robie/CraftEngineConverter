package fr.robie.craftengineconverter.converter.nexo;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.common.CraftEngineImageUtils;
import fr.robie.craftengineconverter.common.ImageConversion;
import fr.robie.craftengineconverter.common.configuration.Configuration;
import fr.robie.craftengineconverter.common.logger.LogType;
import fr.robie.craftengineconverter.common.logger.Logger;
import fr.robie.craftengineconverter.converter.Converter;
import fr.robie.craftengineconverter.utils.SnakeUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NexoConverter extends Converter {
    public NexoConverter(CraftEngineConverter plugin) {
        super(plugin,"Nexo");
    }

    @Override
    public CompletableFuture<Void> convertItems(boolean async){
        return executeTask(async, this::convertItemsSync);
    }

    private void convertItemsSync() {
        File inputBase = new File("plugins/" + converterName + "/items");
        File outputBase = new File(this.plugin.getDataFolder(), "converted/"+converterName+"/CraftEngine/resources/craftengineconverter/configuration/items");

        if (!inputBase.exists() || !inputBase.isDirectory()) {
            Logger.info("Nexo items directory not found at: " + inputBase.getAbsolutePath());
            return;
        }
        if (outputBase.exists()){
            deleteDirectory(outputBase);
        }
        outputBase.mkdirs();

        AtomicInteger loadedItems = new AtomicInteger(0);
        try {
            processDirectory(inputBase, inputBase, outputBase, loadedItems);
        } catch (Exception e) {
            Logger.info("Error during Nexo items conversion: " + e.getMessage());
        }
    }

    private void processDirectory(File baseDir, File currentDir, File outputBase, AtomicInteger loadedItems) {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File itemFile : files) {
            if (itemFile.isDirectory()) {
                processDirectory(baseDir, itemFile, outputBase, loadedItems);
                continue;
            }

            String fileName = itemFile.getName();
            if (!fileName.endsWith(".yml")) {
                continue;
            }

            YamlConfiguration config = getConfig(itemFile);
            YamlConfiguration convertedConfig = new YamlConfiguration();
            ConfigurationSection items = convertedConfig.createSection("items");
            Set<String> keys = config.getKeys(false);
            List<String> itemsIds = new ArrayList<>();
            String finalFileName = fileName.substring(0, fileName.length() - 4);
            for (String itemId : keys) {
                ConfigurationSection section = config.getConfigurationSection(itemId);
                if (section == null) {
                    continue;
                }
                String finalItemId = finalFileName + ":" + itemId;
                NexoItemConverter nexoItemConverter = new NexoItemConverter(this,section, finalItemId, items.createSection(finalItemId), convertedConfig);
                nexoItemConverter.convertItem();
                if (!nexoItemConverter.isExcludeFromInventory()) {
                    itemsIds.add(finalItemId);
                }
                loadedItems.incrementAndGet();
            }
            generateCategorie(itemsIds, convertedConfig, finalFileName);

            try {
                Path relative = baseDir.toPath().relativize(itemFile.toPath());
                File output = new File(outputBase, relative.toString());
                if (!output.getParentFile().exists()) {
                    output.getParentFile().mkdirs();
                }
                convertedConfig.save(output);
            } catch (IOException e) {
                Logger.showException("Failed to save converted item file: " + fileName, e);
            } catch (IllegalArgumentException e) {
                Logger.showException("Failed to compute relative path for: " + itemFile.getPath(), e);
            }
        }
    }

    @Override
    public CompletableFuture<Void> convertPack(boolean async){
        return executeTask(async, this::convertPackSync);
    }

    private void convertPackSync() {
        try {
            File inputPackFile = new File("plugins/" + converterName + "/pack");
            File outputPackFile = new File(this.plugin.getDataFolder(), "converted/"+converterName+"/CraftEngine/resources/craftengineconverter/resourcepack");

            if (!inputPackFile.exists() || !inputPackFile.isDirectory()) {
                Logger.info("Nexo pack directory not found at: " + inputPackFile.getAbsolutePath());
                return;
            }

            if (!outputPackFile.exists()) {
                outputPackFile.mkdirs();
            } else {
                deleteDirectory(outputPackFile);
                outputPackFile.mkdirs();
            }

            File outputAssetsFolder = new File(outputPackFile, "assets");

            copyAssetsFolder(new File(inputPackFile, "assets"), outputAssetsFolder, "main");

            File nexoExternalPacksFolder = new File(inputPackFile, "external_packs");
            if (nexoExternalPacksFolder.exists() && nexoExternalPacksFolder.isDirectory()) {
                File[] externalPacks = nexoExternalPacksFolder.listFiles();
                if (externalPacks != null) {
                    for (File externalPack : externalPacks) {
                        if (externalPack.isDirectory()) {
                            File externalPackAssetsFolder = new File(externalPack, "assets");
                            copyAssetsFolder(externalPackAssetsFolder, outputAssetsFolder, externalPack.getName());
                        } else if (externalPack.isFile() && externalPack.getName().endsWith(".zip")) {
                            extractAndCopyZipAssets(externalPack, outputAssetsFolder, externalPack.getName().replace(".zip", ""));
                        }
                    }
                }
            };
        } catch (Exception e) {
            Logger.showException("Error during Nexo pack conversion", e);

        }
    }

    @Override
    public CompletableFuture<Void> convertEmojis(boolean async){
        return executeTask(async, this::convertEmojisSync);
    }

    private void convertEmojisSync() {
        File inputEmojisFolder = new File("plugins/"+converterName+"/glyphs");
        File outputEmojisFolder = new File(this.plugin.getDataFolder(), "converted/"+converterName+"/CraftEngine/resources/craftengineconverter/configuration/emojis");
        if (!inputEmojisFolder.exists() || !inputEmojisFolder.isDirectory()) {
            Logger.debug("Nexo emojis directory not found at: " + inputEmojisFolder.getAbsolutePath());
            return;
        }
        if (!outputEmojisFolder.exists()) {
            outputEmojisFolder.mkdirs();
        } else {
            deleteDirectory(outputEmojisFolder);
            outputEmojisFolder.mkdirs();
        }
        processEmojisDirectory(inputEmojisFolder, outputEmojisFolder);
    }

    private void processEmojisDirectory(File inputDir, File outputDir) {
        File[] listFiles = inputDir.listFiles();
        if (listFiles == null) return;

        for (File file : listFiles) {
            if (file.isDirectory()) {
                File newOutputDir = new File(outputDir, file.getName());
                newOutputDir.mkdirs();
                processEmojisDirectory(file, newOutputDir);
            } else if (file.getName().endsWith(".yml")) {
                convertEmojiFile(file, outputDir);
            }
        }
    }

    private void convertEmojiFile(File emojiFile, File outputDir) {
        YamlConfiguration config = getConfig(emojiFile);
        Set<String> keys = config.getKeys(false);
        YamlConfiguration convertedConfig = new YamlConfiguration();
        ConfigurationSection convertedEmojiSection = convertedConfig.createSection("emoji");

        for (String key : keys){
            ConfigurationSection emojiSection = config.getConfigurationSection(key);
            if (emojiSection == null) continue;
            String finalKey = "default:" + key;
            String permission = emojiSection.getString("permission");
            List<String> placeholders = emojiSection.getStringList("placeholders");
            if (placeholders.isEmpty()) continue;
            ConfigurationSection ceEmojiSection = convertedEmojiSection.createSection(finalKey);
            if (permission != null){
                ceEmojiSection.set("permission", permission);
            }
            if (!placeholders.isEmpty()){
                ceEmojiSection.set("keywords", placeholders);
            }
            int index = emojiSection.getInt("index",-1);
            int rows = emojiSection.getInt("rows",0);
            int columns = emojiSection.getInt("columns",0);
            if (index != -1 && rows != 0 && columns != 0){
                ceEmojiSection.set("image",finalKey+":"+rows+":"+columns);
            } else {
                ceEmojiSection.set("image",finalKey+":0:0");
            }
            CraftEngineImageUtils.register(key, new ImageConversion(finalKey, rows,columns));
        }
        if (convertedEmojiSection.getKeys(false).isEmpty()) {
            return;
        }
        try {
            File output = new File(outputDir, emojiFile.getName());
            convertedConfig.save(output);
        } catch (IOException e) {
            Logger.showException("Failed to save converted emoji file: " + emojiFile.getName(), e);
        }
    }

    @Override
    public CompletableFuture<Void> convertImages(boolean async){
        return executeTask(async, this::convertImagesSync);
    }

    @Override
    public CompletableFuture<Void> convertLanguages(boolean async) {
        return executeTask(async, this::convertLanguagesSync);
    }

    @Override
    public CompletableFuture<Void> convertSounds(boolean async) {
        return executeTask(async, this::convertSoundsSync);
    }

    private void convertSoundsSync() {
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

        try {
            SnakeUtils nexoSounds = SnakeUtils.load(inputSoundFile);
            if (nexoSounds == null || nexoSounds.isEmpty()) {
                Logger.debug("Sounds file is empty: " + inputSoundFile.getAbsolutePath());
                return;
            }

            File tempOutputFile = File.createTempFile("craftengine_sounds", ".yml");
            tempOutputFile.deleteOnExit();

            try (SnakeUtils craftEngineSounds = SnakeUtils.createEmpty(tempOutputFile)) {
                SnakeUtils soundsSection = craftEngineSounds.getOrCreateSection("sounds");

                List<Map<String, Object>> nexoSoundsList = nexoSounds.getListMap("sounds");
                if (nexoSoundsList.isEmpty()) {
                    Logger.debug("No sounds found in file");
                    return;
                }

                for (Map<String, Object> soundEntry : nexoSoundsList) {
                    Object idObj = soundEntry.get("id");
                    if (idObj == null) continue;

                    String soundId = idObj.toString();
                    if (soundId.isEmpty()) continue;

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
                    if (jukeboxPlayable instanceof Map<?,?> jukeboxMap){
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
                }

                craftEngineSounds.save(outputSoundFile);
            } catch (Exception e) {
                Logger.showException("Failed to process sounds file: " + inputSoundFile.getName(), e);
            } finally {
                nexoSounds.close();
            }
        } catch (Exception e) {
            Logger.showException("Failed to convert sounds file: " + inputSoundFile.getName(), e);
        }
    }

    private Map<String, Object> createSoundMap(String soundName, Map<String, Object> properties) {
        Map<String, Object> soundMap = new LinkedHashMap<>();
        soundMap.put("name", soundName);

        boolean stream = parseBoolean(properties.get("stream"));
        if (stream) soundMap.put("stream", true);

        boolean preload = parseBoolean(properties.get("preload"));
        if (preload) soundMap.put("preload", true);

        double volume = parseDouble(properties.get("volume"),1f);
        if (volume != 1.0) soundMap.put("volume", volume);

        double pitch = parseDouble(properties.get("pitch"),1f);
        if (pitch != 1.0) soundMap.put("pitch", pitch);

        int weight = parseInt(properties.get("weight"),1);
        if (weight != 1) soundMap.put("weight", weight);

        int attenuationDistance = parseInt(properties.get("attenuation_distance"),16);
        if (attenuationDistance != 16) soundMap.put("attenuation_distance", attenuationDistance);

        return soundMap;
    }

    private void convertLanguagesSync() {
        File languagesFile = new File("plugins/" + converterName + "/languages.yml");
        File outputFile = new File(this.plugin.getDataFolder(), "converted/"+converterName+"/CraftEngine/resources/craftengineconverter/configuration/languages/languages.yml");

        if (!languagesFile.exists() || !languagesFile.isFile()) {
            Logger.debug("Nexo languages file not found at: " + languagesFile.getAbsolutePath());
            return;
        }

        try {
            SnakeUtils nexoLanguages = SnakeUtils.load(languagesFile);
            if (nexoLanguages == null || nexoLanguages.isEmpty()) {
                Logger.debug("Languages file is empty: " + languagesFile.getAbsolutePath());
                return;
            }

            File tempOutputFile = File.createTempFile("craftengine_languages", ".yml");
            tempOutputFile.deleteOnExit();

            try (SnakeUtils craftEngineLanguages = SnakeUtils.createEmpty(tempOutputFile)){
                for (String langKey : nexoLanguages.getKeys()) {
                    Map<String, Object> nexoLangData = nexoLanguages.getMap(langKey);
                    if (nexoLangData == null || nexoLangData.isEmpty()) continue;

                    String craftEngineLangKey = langKey.equals("global") ? "en" : langKey;

                    craftEngineLanguages.addData("translations." + craftEngineLangKey, nexoLangData);
                }

                craftEngineLanguages.save(outputFile);
            } catch (Exception e) {
                Logger.showException("Failed to convert languages file: " + languagesFile.getName(), e);
            }
        } catch (IOException e) {
            Logger.showException("Failed to load languages file: " + languagesFile.getName(), e);
        }
    }


    private void convertImagesSync() {
        File inputBase = new File("plugins/" + converterName + "/glyphs");
        File outputBase = new File(this.plugin.getDataFolder(), "converted/"+converterName+"/CraftEngine/resources/craftengineconverter/configuration/images");

        if (!inputBase.exists() || !inputBase.isDirectory()) {
            Logger.debug("Nexo glyph directory not found at: " + inputBase.getAbsolutePath());
            return;
        }
        if (!outputBase.exists()) {
            outputBase.mkdirs();
        } else {
            deleteDirectory(outputBase);
            outputBase.mkdirs();
        }

        try {
            processImagesDirectory(inputBase, inputBase, outputBase);
        } catch (Exception e) {
            Logger.info("Error during Nexo images conversion: " + e.getMessage());
        }
    }

    private void processImagesDirectory(File baseDir, File currentDir, File outputBase) {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File imageFile : files) {
            if (imageFile.isDirectory()) {
                processImagesDirectory(baseDir, imageFile, outputBase);
                continue;
            }

            String fileName = imageFile.getName();
            if (!fileName.endsWith(".yml")) {
                continue;
            }

            YamlConfiguration config = getConfig(imageFile);
            YamlConfiguration convertedConfig = new YamlConfiguration();
            ConfigurationSection imagesSection = convertedConfig.createSection("images");
            Set<String> keys = config.getKeys(false);

            for (String key : keys){
                ConfigurationSection imageSection = config.getConfigurationSection(key);
                if (imageSection == null) continue;

                String finalKey = "default:" + key;
                ConfigurationSection section = imagesSection.createSection(finalKey);
                String texture = imageSection.getString("texture");
                if (isValidString(texture)){
                    section.set("file", namespaced(texture));
                }
                int ascent = imageSection.getInt("ascent", 0);
                if (ascent != 0){
                    section.set("ascent", ascent);
                }
                int height = imageSection.getInt("height", 0);
                if (height != 0){
                    section.set("height", height);
                }
                String font = imageSection.getString("font");
                if (isValidString(font)){
                    section.set("font", font);
                }
                int rows = imageSection.getInt("rows",0);
                int cols = imageSection.getInt("columns",0);
                if (rows > 0 && cols > 0){
                    section.set("grid-size", rows+","+cols);
                }
                CraftEngineImageUtils.register(key, new ImageConversion(finalKey, rows, cols));
            }

            try {
                Path relative = baseDir.toPath().relativize(imageFile.toPath());
                File output = new File(outputBase, relative.toString());
                if (!output.getParentFile().exists()) {
                    output.getParentFile().mkdirs();
                }
                convertedConfig.save(output);
            } catch (IOException e) {
                Logger.showException("Failed to save converted image file: " + fileName, e);
            } catch (IllegalArgumentException e) {
                Logger.showException("Failed to compute relative path for: " + imageFile.getPath(), e);
            }
        }
    }


    private void copyAssetsFolder(File assetsFolder, File outputAssetsFolder, String packName) {
        if (!assetsFolder.exists() || !assetsFolder.isDirectory()) {
            Logger.debug("Assets folder not found for pack '" + packName + "' at: " + assetsFolder.getAbsolutePath());
            return;
        }

        try {
            copyDirectory(assetsFolder, outputAssetsFolder, assetsFolder);
        } catch (IOException e) {
            Logger.info("Failed to copy assets from " + packName + " pack: " + e.getMessage(), LogType.ERROR);
        }
    }

    private void copyDirectory(File source, File destination, File assetsRoot) throws IOException {
        if (!destination.exists()) {
            destination.mkdirs();
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
                continue;
            }

            if (file.isFile()) {
                String fullPathWithFile = namespace + ":" + pathInNamespace + "/" + file.getName();
                if (Configuration.isPathBlacklisted(fullPathWithFile)) {
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
                if (!targetFile.exists()) {
                    targetFile.mkdirs();
                }

                if (resolvedMapping != null) {
                    copyDirectoryContents(file, targetFile);
                } else {
                    copyDirectory(file, destination, assetsRoot);
                }
            } else {
                if (!targetFile.getParentFile().exists()) {
                    targetFile.getParentFile().mkdirs();
                }
                copyFile(file, targetFile);
            }
        }
    }

    private void copyDirectoryContents(File source, File destination) throws IOException {
        if (!destination.exists()) {
            destination.mkdirs();
        }

        File[] files = source.listFiles();
        if (files == null) return;

        for (File file : files) {
            File targetFile = new File(destination, file.getName());

            if (file.isDirectory()) {
                copyDirectoryContents(file, targetFile);
            } else {
                if (!targetFile.getParentFile().exists()) {
                    targetFile.getParentFile().mkdirs();
                }
                copyFile(file, targetFile);
            }
        }
    }

    private void copyFile(File source, File destination) throws IOException {
        Files.copy(
            source.toPath(),
            destination.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        );
    }

    private void extractAndCopyZipAssets(File zipFile, File outputAssetsFolder, String packName) {
        File tempDir = new File(this.plugin.getDataFolder(), "temp/zip_extract_" + System.currentTimeMillis());
        tempDir.mkdirs();

        try {
            extractZip(zipFile.toPath(), tempDir.toPath());

            File extractedAssetsFolder = new File(tempDir, "assets");
            if (extractedAssetsFolder.exists() && extractedAssetsFolder.isDirectory()) {
                Logger.debug("Found assets folder in ZIP: " + zipFile.getName());
                copyAssetsFolder(extractedAssetsFolder, outputAssetsFolder, packName);
            } else {
                Logger.debug("No assets folder found in ZIP: " + zipFile.getName());
            }

            deleteDirectory(tempDir);
        } catch (IOException e) {
            Logger.showException("Failed to extract and copy assets from ZIP: " + zipFile.getName(), e);
        } finally {
            if (tempDir.exists()) {
                deleteDirectory(tempDir);
            }
        }
    }

    private void extractZip(Path zipPath, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipPath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    Files.createDirectories(targetDir.resolve(entry.getName()));
                    zis.closeEntry();
                    continue;
                }

                Path resolved = targetDir.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(targetDir.normalize())) {
                    throw new IOException("Bad zip entry: " + entry.getName());
                }

                Files.createDirectories(resolved.getParent());
                try (OutputStream out = Files.newOutputStream(resolved, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
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
