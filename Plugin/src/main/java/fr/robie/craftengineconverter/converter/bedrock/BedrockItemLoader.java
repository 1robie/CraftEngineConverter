package fr.robie.craftengineconverter.converter.bedrock;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.ItemMapping;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.ItemModelItemMapping;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.LegacyItemMapping;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationRegistry;
import fr.robie.craftengineconverter.api.logger.Logger;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BedrockItemLoader {
    private final String itemId;
    private final ConfigurationSection itemSection;

    public BedrockItemLoader(@NotNull String itemId, @NotNull ConfigurationSection itemSection) {
        this.itemId = itemId;
        this.itemSection = itemSection;
    }


    public ItemMapping load() {
        ItemMapping itemMapping;
        if (this.itemSection.isString("item-model")) {
            itemMapping = new ItemModelItemMapping(
                    this.getMaterial(),
                    this.itemId,
                    this.itemSection.getString("item-model")
            );
        } else if (this.itemSection.isInt("custom-model-data")) {
            Logger.info("Item %itemId% uses custom model data integer, which is not supported for Bedrock Edition. Skipping.");
            return null;
        } else {
            List<Float> floats = this.itemSection.getFloatList("data.components.custom_model_data");
            if (floats.isEmpty()) {
                floats = this.itemSection.getFloatList("data.components.minecraft:custom_model_data");
            }
            if (!floats.isEmpty()) {
                itemMapping = new LegacyItemMapping(
                        this.getMaterial(),
                        this.itemId,
                        floats.getFirst()
                );
            } else {
                return null;
            }
        }

        String itemName = this.itemSection.getString("data.item-name", this.itemSection.getString("data.custom-name"));
        if (itemName != null) {
            itemMapping.setDisplayName(itemName);
        }

        ConfigurationSection modelSection = this.itemSection.getConfigurationSection("model");
        if (modelSection != null) {
            ModelConfiguration modelConfiguration = ModelConfigurationRegistry.load(modelSection);
            if (modelConfiguration != null) {

            }
        }

        return itemMapping;
    }


    private Material getMaterial() {
        String material = this.itemSection.getString("material");
        if (material != null) {
            try {
                return Material.valueOf(material.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Configuration.get(ConfigurationKey.DEFAULT_MATERIAL);
    }

}
