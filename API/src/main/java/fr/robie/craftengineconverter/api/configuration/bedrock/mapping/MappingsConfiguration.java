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

        Path mappingsFile = directory.resolve("items_mappings.json");
        FileCacheManager.saveJsonToFile(mappingsFile, jsonObject);
    }
}
