package fr.robie.craftengineconverter.api.configuration.item.clientbounddata;

import fr.robie.craftengineconverter.api.configuration.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OverwritableLoreConfiguration implements ItemConfigurationSerializable {
    private final List<String> lore;

    public OverwritableLoreConfiguration(@NotNull List<String> lore) {
        this.lore = lore;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection clientBoundDataSection = this.getOrCreateSection(itemSection, "client-bound-data");
        clientBoundDataSection.set("overwritable-lore", this.lore);
    }
}
