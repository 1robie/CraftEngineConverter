package fr.robie.craftengineconverter.api.configuration.item.clientbounddata;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class OverwritableItemNameConfiguration implements ItemConfigurationSerializable {
    private final String itemName;

    public OverwritableItemNameConfiguration(@NotNull String itemName) {
        this.itemName = itemName;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection clientBoundDataSection = this.getOrCreateSection(itemSection, "client-bound-data");
        clientBoundDataSection.set("overwritable-item-name", this.itemName);
    }
}
