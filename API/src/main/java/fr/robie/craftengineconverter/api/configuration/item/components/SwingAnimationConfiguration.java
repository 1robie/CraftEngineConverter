package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class SwingAnimationConfiguration implements ItemConfigurationSerializable {

    private final AnimationType type;
    private final int duration;

    public SwingAnimationConfiguration(AnimationType type, int duration) {
        this.type = type;
        this.duration = duration;
    }

    public enum AnimationType {
        NONE, WHACK, STAB;

        public String toKey() {
            return this.name().toLowerCase();
        }
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection swingAnimationSection = this.getOrCreateSection(components, "minecraft:swing_animation");

        if (this.type != AnimationType.WHACK) {
            swingAnimationSection.set("type", this.type.toKey());
        }

        if (this.duration != 6) {
            swingAnimationSection.set("duration", this.duration);
        }
    }
}