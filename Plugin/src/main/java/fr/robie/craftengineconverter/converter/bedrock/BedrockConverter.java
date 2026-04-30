package fr.robie.craftengineconverter.converter.bedrock;

import fr.robie.craftengineconverter.api.configuration.bedrock.ItemTextureConfiguration;
import fr.robie.craftengineconverter.api.configuration.bedrock.ManifestConfiguration;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.MappingsConfiguration;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.ItemMapping;
import fr.robie.craftengineconverter.api.logger.Logger;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.craftengineconverter.api.utils.FileUtils;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import fr.robie.craftengineconverter.common.utils.yaml.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class BedrockConverter {
    private final File pluginFolder;

    public BedrockConverter(File pluginFolder) {
        this.pluginFolder = pluginFolder;
    }

    public void convert() {
        File outputFolder = new File(this.pluginFolder, "bedrock-converted/Geyser-Spigot");

        if (outputFolder.exists()) {
            FileUtils.deleteDirectory(outputFolder);
        }

        if (!FileUtils.mkdirs(outputFolder)) {
            return;
        }

        File customMappings = new File(outputFolder, "custom_mappings");
        if (!FileUtils.mkdirs(customMappings)) {
            return;
        }

        File packs = new File(outputFolder, "packs");
        if (!FileUtils.mkdirs(packs)) {
            return;
        }

        File textures = new File(packs, "textures");
        if (!FileUtils.mkdirs(textures)) {
            return;
        }

        this.convertItemMappingsFolder(new File(this.pluginFolder, "bedrock/items"), outputFolder);
    }

    private void convertItemMappingsFolder(@NotNull File inputFolder, @NotNull File outputFolder) {
        if (!inputFolder.exists() || !inputFolder.isDirectory()) {
            Logger.info("Input folder does not exist or is not a directory: " + inputFolder.getAbsolutePath());
            return;
        }

        File[] listed = inputFolder.listFiles();
        if (listed == null) {
            Logger.info("No files found in input folder: " + inputFolder.getAbsolutePath());
            return;
        }

        if (!FileUtils.mkdirs(outputFolder)) {
            Logger.info("Failed to create output folder: " + outputFolder.getAbsolutePath());
            return;
        }

        MappingsConfiguration mappingsConfiguration = new MappingsConfiguration();
        ItemTextureConfiguration itemTextureConfiguration = new ItemTextureConfiguration();

        for (File file : listed) {
            if (file.isFile() && FileUtils.isYmlFile(file)) {
                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
                ConfigurationSection items = yamlConfiguration.getConfigurationSection("items");
                if (items != null) {
                    for (String itemId : items.getKeys(false)) {
                        ConfigurationSection itemSection = items.getConfigurationSection(itemId);
                        if (itemSection != null) {
                            BedrockItemLoader itemLoader = new BedrockItemLoader(itemId, itemSection);

                            ItemMapping load = itemLoader.load();
                            if (load != null) {
                                mappingsConfiguration.addItemMapping(load);

                                load.getTexturesData().forEach(itemTextureConfiguration::addTextureData);
                            }
                        }
                    }
                }
            }
        }

        Logger.info("Saving mappings and textures to: " + outputFolder.getAbsolutePath());
        mappingsConfiguration.saveMappings(outputFolder.toPath());
        itemTextureConfiguration.save(outputFolder.toPath());
    }


    private void convertPack(File inputFolder, File outputPackFolder) {
        // if zip xxx

        File[] listed = inputFolder.listFiles();
        if (listed == null) {
            return;
        }

        for (var file : listed) {
            String fileName = file.getName();
            if (file.isDirectory()) {
                if (fileName.equalsIgnoreCase("assets")) {
                    File[] subFiles = file.listFiles();
                    if (subFiles != null) {
                        for (var namespacedFile : subFiles) {
                            if (namespacedFile.isDirectory()) {
                                File[] folderType = namespacedFile.listFiles();
                                if (folderType != null) {
                                    for (var typeFile : folderType) {
                                        if (typeFile.isDirectory()) {
                                            String typeName = typeFile.getName();
                                            switch (typeName) {
                                                case "textures", "models" ->
                                                        FileUtils.copyDirectory(typeFile, new File(outputPackFolder, typeName));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                String fileNameWithoutExtension = FileUtils.getFileNameWithoutExtension(file);
                String extension = FileUtils.getFileExtension(file);
                if (fileNameWithoutExtension.equalsIgnoreCase("pack")) {
                    File dest = new File(outputPackFolder, "pack_icon" + extension);
                    FileUtils.copyFile(file, dest);
                } else if (fileName.equalsIgnoreCase("pack.mcmeta")) {
                    FileCacheManager.getJsonCache().getEntryFile(file.toPath()).ifPresent(jsonObjectFileCacheEntry -> {
                        ManifestConfiguration configuration = ManifestConfiguration.fromJavaPackFormat(jsonObjectFileCacheEntry.getData());
                        configuration.saveManifest(outputPackFolder.toPath());
                    });
                }
            }
        }
    }
}
