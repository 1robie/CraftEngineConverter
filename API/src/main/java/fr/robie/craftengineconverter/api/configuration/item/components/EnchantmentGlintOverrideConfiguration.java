package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class EnchantmentGlintOverrideConfiguration implements ItemConfigurationSerializable {
    private final boolean enchantGlintOverride;

    public EnchantmentGlintOverrideConfiguration(boolean enchantGlintOverride) {
        this.enchantGlintOverride = enchantGlintOverride;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        this.getOrCreateSection(itemSection, "components").set("minecraft:enchantment_glint_override", this.enchantGlintOverride);
    }
}
