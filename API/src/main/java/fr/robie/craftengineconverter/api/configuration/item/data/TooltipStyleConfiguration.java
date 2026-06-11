package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.bukkit.NamespacedKey;
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
