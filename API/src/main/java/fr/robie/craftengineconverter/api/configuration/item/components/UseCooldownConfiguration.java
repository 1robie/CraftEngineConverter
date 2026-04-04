package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.component.BedrockComponent;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class UseCooldownConfiguration implements ItemConfigurationSerializable, BedrockComponent {
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

    @Override
    public void applyTo(@NotNull JsonObject componentObject) {
        JsonObject useCooldownComponent = new JsonObject();
        useCooldownComponent.addProperty("seconds", this.seconds);
        if (this.cooldownGroup != null) {
            useCooldownComponent.addProperty("cooldown_group", this.cooldownGroup);
        }
        componentObject.add("minecraft:use_cooldown", useCooldownComponent);
    }
}
