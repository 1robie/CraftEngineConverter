package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.component.BedrockComponent;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class SwingAnimationConfiguration implements ItemConfigurationSerializable, BedrockComponent {

    private final AnimationType type;
    private final int duration;

    public SwingAnimationConfiguration(AnimationType type, int duration) {
        this.type = type;
        this.duration = duration;
    }

    /**
     * Animation is hardcoded for bedrock, so only duration is configurable
     *
     * @param duration Duration of the swing animation in ticks
     */
    public SwingAnimationConfiguration(int duration) {
        this(null, duration);
    }

    @Override
    public void applyTo(@NotNull JsonObject componentObject) {
        JsonObject swingAnimationComponent = new JsonObject();

        if (this.duration != 6) {
            swingAnimationComponent.addProperty("duration", this.duration);
        }

        componentObject.add("minecraft:swing_animation", swingAnimationComponent);
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