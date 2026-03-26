package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class UseCooldownConfiguration implements ItemConfigurationSerializable {
    private final float seconds;
    private final String cooldownGroup;

    public UseCooldownConfiguration(float seconds, String cooldownGroup) {
        this.seconds = seconds;
        this.cooldownGroup = cooldownGroup;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection useCooldownComponent = this.getOrCreateSection(components, "minecraft:use_cooldown");
        useCooldownComponent.set("seconds", this.seconds);
        if (this.cooldownGroup != null) {
            useCooldownComponent.set("cooldown_group", this.cooldownGroup);
        }
    }
}
