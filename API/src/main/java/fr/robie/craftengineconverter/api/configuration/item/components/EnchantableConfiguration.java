package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class EnchantableConfiguration implements ItemConfigurationSerializable {
    private final int enchantability;

    public EnchantableConfiguration(int enchantability) {
        this.enchantability = enchantability;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection enchantableComponent = this.getOrCreateSection(components, "minecraft:enchantable");
        enchantableComponent.set("value", this.enchantability);
    }
}
