package fr.robie.craftEngineConverter.converter.nexo;

import fr.robie.craftEngineConverter.CraftEngineConverter;
import fr.robie.craftEngineConverter.converter.Converter;
import fr.robie.craftEngineConverter.core.utils.YamlUtils;
import fr.robie.craftEngineConverter.core.utils.logger.LogType;
import fr.robie.craftEngineConverter.core.utils.logger.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class NexoConverter extends YamlUtils implements Converter {
    private final CraftEngineConverter plugin;

    public NexoConverter(CraftEngineConverter plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void convertAll() {

    }

    @Override
    public void convertItems(){
        File inputBase = new File("plugins/Nexo/items");
        File outputBase = new File(plugin.getDataFolder(), "converted/Nexo/CraftEngine/resources/craftengineconverter/configuration/items");
        if (!inputBase.exists() || !inputBase.isDirectory()) {
            return;
        }
        long startTime = System.currentTimeMillis();
        AtomicInteger loadedItems = new AtomicInteger(0);
        try {
            this.plugin.getFoliaCompatibilityManager().runAsync(()->{
                processDirectory(inputBase, inputBase, outputBase, loadedItems);
                Logger.info("Converted " + loadedItems + " Nexo items. Time taken: " + (System.currentTimeMillis() - startTime) + " ms");
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

    public void generateCategorie(List<String> itemsIds, YamlConfiguration config, String fileName) {
        if (itemsIds.isEmpty()) return;
        ConfigurationSection categoriesSection = config.createSection("categories");
        ConfigurationSection categorySection = categoriesSection.createSection(itemsIds.getFirst());
        categorySection.set("name", "Category "+fileName);
        categorySection.set("icon", itemsIds.getFirst());
        categorySection.set("list", itemsIds);
    }
}
