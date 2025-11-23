package fr.robie.craftEngineConverter.converter.nexo;

import fr.robie.craftEngineConverter.CraftEngineConverter;
import fr.robie.craftEngineConverter.converter.Converter;
import fr.robie.craftEngineConverter.utils.Configuration;
import fr.robie.craftEngineConverter.utils.logger.LogType;
import fr.robie.craftEngineConverter.utils.logger.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class NexoConverter extends Converter {
    public NexoConverter(CraftEngineConverter plugin) {
        super(plugin,"Nexo");
    }

    @Override
    public void convertItems(){
        File inputBase = new File("plugins/" + converterName + "/items");
        File outputBase = new File(this.plugin.getDataFolder(), "converted/"+converterName+"/CraftEngine/resources/craftengineconverter/configuration/items");
        if (!inputBase.exists() || !inputBase.isDirectory()) {
            Logger.info("Nexo items directory not found at: " + inputBase.getAbsolutePath());
            return;
        }
        AtomicInteger loadedItems = new AtomicInteger(0);
        try {
            this.plugin.getFoliaCompatibilityManager().runAsync(()->{
                processDirectory(inputBase, inputBase, outputBase, loadedItems);
            });
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
                NexoItemConverter nexoItemConverter = new NexoItemConverter(section, finalItemId, items.createSection(finalItemId));
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
    public void convertPack(){
        try {
            this.plugin.getFoliaCompatibilityManager().runAsync(() -> {
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

                // Copy main assets folder
                copyAssetsFolder(new File(inputPackFile, "assets"), outputAssetsFolder, "main");

                // Copy external packs assets
                File nexoExternalPacksFolder = new File(inputPackFile, "external_packs");
                if (nexoExternalPacksFolder.exists() && nexoExternalPacksFolder.isDirectory()) {
                    File[] externalPacks = nexoExternalPacksFolder.listFiles();
                    if (externalPacks != null) {
                        for (File externalPack : externalPacks) {
                            if (!externalPack.isDirectory()) continue;
                            File externalPackAssetsFolder = new File(externalPack, "assets");
                            copyAssetsFolder(externalPackAssetsFolder, outputAssetsFolder, externalPack.getName());
                        }
                    }
                }

                Logger.info("Pack conversion completed successfully");
            });
        } catch (Exception e) {
            Logger.info("Error during Nexo pack conversion: " + e.getMessage(), LogType.ERROR);
        }
    }

    private void copyAssetsFolder(File assetsFolder, File outputAssetsFolder, String packName) {
        if (!assetsFolder.exists() || !assetsFolder.isDirectory()) {
            Logger.debug("Assets folder not found for pack '" + packName + "' at: " + assetsFolder.getAbsolutePath());
            return;
        }

        try {
            copyDirectory(assetsFolder, outputAssetsFolder);
        } catch (IOException e) {
            Logger.info("Failed to copy assets from " + packName + " pack: " + e.getMessage(), LogType.ERROR);
        }
    }

    private void copyDirectory(File source, File destination) throws IOException {
        if (!destination.exists()) {
            destination.mkdirs();
        }

        File[] files = source.listFiles();
        if (files == null) return;

        for (File file : files) {
            File newFile = new File(destination, file.getName());
            if (file.isDirectory()) {
                copyDirectory(file, newFile);
            } else {
                copyFile(file, newFile);
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
