package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class EnchantableConfiguration implements ItemConfigurationSerializable {
    private final int enchantability;

    public EnchantableConfiguration(int enchantability) {
        this.enchantability = enchantability;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getComponentsSection(itemSection);
        ConfigurationSection enchantableComponent = this.getOrCreateSection(components, "minecraft:enchantable");
        enchantableComponent.set("value", this.enchantability);
    }

}
