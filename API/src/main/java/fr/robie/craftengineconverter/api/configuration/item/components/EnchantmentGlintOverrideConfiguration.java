package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class EnchantmentGlintOverrideConfiguration implements ItemConfigurationSerializable {
    private final boolean enchantGlintOverride;

    public EnchantmentGlintOverrideConfiguration(boolean enchantGlintOverride) {
        this.enchantGlintOverride = enchantGlintOverride;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        this.getComponentsSection(itemSection).set("minecraft:enchantment_glint_override", this.enchantGlintOverride);
    }

}
