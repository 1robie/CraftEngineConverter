package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.component.BedrockComponent;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class EnchantmentGlintOverrideConfiguration implements ItemConfigurationSerializable, BedrockComponent {
    private final boolean enchantGlintOverride;

    public EnchantmentGlintOverrideConfiguration(boolean enchantGlintOverride) {
        this.enchantGlintOverride = enchantGlintOverride;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        this.getOrCreateSection(itemSection, "components").set("minecraft:enchantment_glint_override", this.enchantGlintOverride);
    }

    @Override
    public void applyTo(@NotNull JsonObject componentObject) {
        componentObject.addProperty("minecraft:enchantment_glint_override", this.enchantGlintOverride);
    }
}
