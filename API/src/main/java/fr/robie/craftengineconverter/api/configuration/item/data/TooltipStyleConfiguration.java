package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.ItemConfigurationSerializable;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class TooltipStyleConfiguration implements ItemConfigurationSerializable {
    private final NamespacedKey styleKey;

    public TooltipStyleConfiguration(NamespacedKey styleKey) {
        this.styleKey = styleKey;
    }

    public TooltipStyleConfiguration(String styleKey) {
        this.styleKey = NamespacedKey.fromString(styleKey);
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        if (this.styleKey != null) {
            this.getOrCreateSection(itemSection, "data").set("tooltip-style", this.styleKey.asString());
        }
    }
}
