package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CustomDataConfiguration implements ItemConfigurationSerializable {
    private final List<CustomDataEntry> customDataEntries;

    public CustomDataConfiguration(@NotNull List<CustomDataEntry> customDataEntries) {
        this.customDataEntries = customDataEntries;
    }


    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection customDataComponent = this.getOrCreateSection(components, "minecraft:custom_data");
        for (CustomDataEntry entry : this.customDataEntries) {
            customDataComponent.set(entry.key(), entry.value());
        }
    }

    public record CustomDataEntry(String key, Object value) {
    }
}
