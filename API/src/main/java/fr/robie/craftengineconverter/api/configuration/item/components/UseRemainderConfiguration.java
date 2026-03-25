package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class UseRemainderConfiguration implements ItemConfigurationSerializable {
    private final String remainderItemId;
    private final int remainderCount;

    public UseRemainderConfiguration(String remainderItemId, int remainderCount) {
        this.remainderItemId = remainderItemId;
        this.remainderCount = remainderCount;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        if (remainderItemId == null || remainderItemId.isEmpty() || remainderCount <= 0) {
            return;
        }

        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection useRemainderSection = this.getOrCreateSection(components, "minecraft:use_remainder");
        useRemainderSection.set("item", this.remainderItemId);
        if (remainderCount > 1) {
            useRemainderSection.set("count", this.remainderCount);
        }
    }
}
