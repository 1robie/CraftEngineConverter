package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.AbstractEffectsConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DeathProtectionConfiguration extends AbstractEffectsConfiguration {

    private final List<ConsumeEffect> deathEffects;

    public DeathProtectionConfiguration(List<ConsumeEffect> deathEffects) {
        this.deathEffects = deathEffects;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection deathProtectionSection = this.getOrCreateSection(components, "minecraft:death_protection");

        if (this.deathEffects == null || this.deathEffects.isEmpty()) {
            deathProtectionSection.set("death_effects", new ArrayList<>());
            return;
        }

        deathProtectionSection.set("death_effects", this.serializeEffects(this.deathEffects));
    }
}