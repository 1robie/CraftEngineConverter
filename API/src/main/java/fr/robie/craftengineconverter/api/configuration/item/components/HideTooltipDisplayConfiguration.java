package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class HideTooltipDisplayConfiguration implements ItemConfigurationSerializable {
    private final boolean hideTooltip;

    public HideTooltipDisplayConfiguration(boolean hideTooltip) {
        this.hideTooltip = hideTooltip;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection tooltipDisplay = this.getOrCreateSection(components, "minecraft:tooltip_display");
        tooltipDisplay.set("hide_tooltip", this.hideTooltip);
    }
}
