package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
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
        ConfigurationSection components = this.getComponentsSection(itemSection);
        ConfigurationSection useCooldownComponent = this.getOrCreateSection(components, "minecraft:use_cooldown");
        useCooldownComponent.set("seconds", this.seconds);
        if (this.cooldownGroup != null) {
            useCooldownComponent.set("cooldown_group", this.cooldownGroup);
        }
    }

}
