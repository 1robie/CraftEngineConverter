package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TooltipDisplayConfiguration implements ItemConfigurationSerializable {
    private final List<String> hiddenComponents;

    public TooltipDisplayConfiguration(@NotNull List<String> hiddenComponents) {
        this.hiddenComponents = hiddenComponents;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        if (this.hiddenComponents.isEmpty()) {
            return;
        }
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection tooltipDisplayComponent = this.getOrCreateSection(components, "minecraft:tooltip_display");
        tooltipDisplayComponent.set("hidden_components", this.hiddenComponents);
    }
}
