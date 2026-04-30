package fr.robie.craftengineconverter.converter.bedrock;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.GroupDefinitionMapping;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.ItemMapping;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.ItemModelItemMapping;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.LegacyItemMapping;
import fr.robie.craftengineconverter.api.configuration.bedrock.texture.TextureData;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.condition.ConditionModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.GenerationConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.SimpleModelConfiguration;
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
        Material material = this.getMaterial();
        ModelConfiguration modelConfiguration = ModelConfigurationRegistry.load(this.itemSection.getConfigurationSection("model"));

        if (modelConfiguration instanceof ConditionModelConfiguration conditionModelConfiguration) {
            GroupDefinitionMapping groupDefinitionMapping = new GroupDefinitionMapping(material, this.itemId);

            ItemModelItemMapping onFalse = new ItemModelItemMapping(material, this.itemId, null);
            ItemModelItemMapping onTrue = new ItemModelItemMapping(material, this.itemId + "_" + conditionModelConfiguration.getProperty(), null);

            onTrue.setBedrockPredicate(conditionModelConfiguration.getOnTruePredicate());
            onFalse.setBedrockPredicate(conditionModelConfiguration.getOnFalsePredicate());

            this.convertItem(onTrue);
            this.convertItem(onFalse);

            groupDefinitionMapping.addDefinition(onTrue);
            groupDefinitionMapping.addDefinition(onFalse);

            this.addTextureDataIfSimpleModel(conditionModelConfiguration.getOnFalse(), groupDefinitionMapping, this.itemId + "_false");
            this.addTextureDataIfSimpleModel(conditionModelConfiguration.getOnTrue(), groupDefinitionMapping, this.itemId + "_true");

            return groupDefinitionMapping;
        }

        if (this.itemSection.isString("item-model")) {
            itemMapping = new ItemModelItemMapping(
                    material,
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
                        material,
                        this.itemId,
                        floats.getFirst()
                );
            } else {
                return null;
            }
        }


        return itemMapping;
    }

    private void convertItem(ItemMapping itemMapping) {
        String itemName = this.itemSection.getString("data.item-name", this.itemSection.getString("data.custom-name"));
        if (itemName != null) {
            itemMapping.setDisplayName(itemName);
        }

    }

    /**
     * Adds TextureData to the groupDefinitionMapping if the modelConfiguration is a SimpleModelConfiguration with a GenerationConfiguration.
     *
     * @param modelConfiguration     the model configuration (can be null)
     * @param groupDefinitionMapping the group definition mapping to add to
     * @param textureId              the texture id to use for toTextureData
     */
    private void addTextureDataIfSimpleModel(ModelConfiguration modelConfiguration, GroupDefinitionMapping groupDefinitionMapping, String textureId) {
        if (modelConfiguration instanceof SimpleModelConfiguration simpleModelConfiguration) {
            GenerationConfiguration generation = simpleModelConfiguration.getGeneration();
            if (generation != null) {
                TextureData textureData = generation.toTextureData(textureId);
                if (textureData != null) {
                    groupDefinitionMapping.addTextureData(textureData);
                }
            }
        }
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
