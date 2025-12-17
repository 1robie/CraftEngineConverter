package fr.robie.craftEngineConverter.converter.nexo;

import fr.robie.craftEngineConverter.CraftEngineConverter;
import fr.robie.craftEngineConverter.converter.Converter;
import fr.robie.craftEngineConverter.utils.Configuration;
import fr.robie.craftEngineConverter.utils.SnakeUtils;
import fr.robie.craftEngineConverter.utils.logger.LogType;
import fr.robie.craftEngineConverter.utils.logger.Logger;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                Logger.info("Failed to save converted file: " + fileName, LogType.ERROR);
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                Logger.info("Failed to compute relative path for: " + itemFile.getPath(),LogType.ERROR);
                e.printStackTrace();
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
            }

            Logger.info("Pack conversion completed successfully");
        } catch (Exception e) {
            Logger.info("Error during Nexo pack conversion: " + e.getMessage(), LogType.ERROR);
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
            int rows = emojiSection.getInt("rows",-1);
            int columns = emojiSection.getInt("columns",-1);
            if (index != -1 && rows != -1 && columns != -1){
                ceEmojiSection.set("image",finalKey+":"+rows+":"+columns);
            } else {
                ceEmojiSection.set("image",finalKey+":0:0");
            }
        }
        if (convertedEmojiSection.getKeys(false).isEmpty()) {
            return;
        }
        try {
            File output = new File(outputDir, emojiFile.getName());
            convertedConfig.save(output);
        } catch (IOException e) {
            Logger.info("Failed to save converted emoji file: " + emojiFile.getName(), LogType.ERROR);
            e.printStackTrace();
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
                    Map<String, Object> nexoLangData = nexoLanguages.getMapValue(langKey);
                    if (nexoLangData == null || nexoLangData.isEmpty()) continue;

                    String craftEngineLangKey = langKey.equals("global") ? "en" : langKey;

                    craftEngineLanguages.addData("translations." + craftEngineLangKey, nexoLangData);
                }

                craftEngineLanguages.save(outputFile);
            } catch (Exception e) {
                Logger.info("Failed to convert languages file: " + languagesFile.getName(), LogType.ERROR);
                e.printStackTrace();
            }
        } catch (IOException e) {
            Logger.info("Failed to convert languages file: " + languagesFile.getName(), LogType.ERROR);
            e.printStackTrace();
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
            Logger.info("Error during Nexo images conversion: " + e.getMessage(), LogType.ERROR);
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

                ConfigurationSection section = imagesSection.createSection("default:" + key);
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
            }

            try {
                Path relative = baseDir.toPath().relativize(imageFile.toPath());
                File output = new File(outputBase, relative.toString());
                if (!output.getParentFile().exists()) {
                    output.getParentFile().mkdirs();
                }
                convertedConfig.save(output);
            } catch (IOException e) {
                Logger.info("Failed to save converted image file: " + fileName, LogType.ERROR);
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                Logger.info("Failed to compute relative path for: " + imageFile.getPath(), LogType.ERROR);
                e.printStackTrace();
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
            Logger.info("Failed to extract or copy assets from ZIP '" + zipFile.getName() + "': " + e.getMessage(), LogType.ERROR);
            e.printStackTrace();
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
