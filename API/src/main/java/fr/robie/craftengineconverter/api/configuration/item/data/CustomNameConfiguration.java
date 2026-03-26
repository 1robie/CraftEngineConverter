package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class CustomNameConfiguration implements ItemConfigurationSerializable {
    private final String customName;

    public CustomNameConfiguration(@NotNull String customName) {
        this.customName = customName;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection dataSection = this.getOrCreateSection(itemSection, "data");
        dataSection.set("custom-name", this.customName);
    }
}
