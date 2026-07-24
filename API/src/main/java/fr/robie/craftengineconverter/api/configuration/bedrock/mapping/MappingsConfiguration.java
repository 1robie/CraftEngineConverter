package fr.robie.craftengineconverter.api.configuration.bedrock.mapping;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.ItemMapping;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingsConfiguration {
    private static final int formatVersion = 2;

    private final Map<Material, List<ItemMapping>> itemMappings = new HashMap<>();

    public MappingsConfiguration addItemMapping(@NotNull ItemMapping itemMapping) {
        this.itemMappings.computeIfAbsent(itemMapping.getJavaMaterial(), k -> new ArrayList<>()).add(itemMapping);
        return this;
    }

    public List<ItemMapping> getMappings(@NotNull Material material) {
        return this.itemMappings.get(material);
    }

    public boolean removeItemMapping(@NotNull Material material, @NotNull ItemMapping mapping) {
        List<ItemMapping> list = this.itemMappings.get(material);
        return list != null && list.remove(mapping);
    }

    public void replaceItemMapping(@NotNull Material material, @NotNull ItemMapping old, @NotNull ItemMapping replacement) {
        List<ItemMapping> list = this.itemMappings.get(material);
        if (list != null) {
            int index = list.indexOf(old);
            if (index >= 0) {
                list.set(index, replacement);
            }
        }
    }

    public void saveMappings(@NotNull Path directory) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("format_version", formatVersion);

        JsonObject itemsObject = new JsonObject();

        for (var entry : this.itemMappings.entrySet()) {
            Material material = entry.getKey();
            List<ItemMapping> mappings = entry.getValue();

            JsonArray mappingsArray = new JsonArray();
            for (ItemMapping mapping : mappings) {
                mappingsArray.add(mapping.serialize());
            }
            itemsObject.add("minecraft:" + material.name().toLowerCase(), mappingsArray);
        }

        jsonObject.add("items", itemsObject);

        Path mappingsFile = directory.resolve("geyser_item_mappings.json");
        FileCacheManager.saveJsonToFile(mappingsFile, jsonObject);
    }
}
