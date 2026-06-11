package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class CustomModelDataConfiguration implements ItemConfigurationSerializable {
    private final int customModelData;

    public CustomModelDataConfiguration(int customModelData) {
        this.customModelData = customModelData;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        this.getOrCreateSection(itemSection, "data").set("custom-model-data", this.customModelData);
    }
}
