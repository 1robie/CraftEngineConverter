package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import java.util.Locale;

public class SwingAnimationConfiguration implements ItemConfigurationSerializable {


    private final AnimationType type;
    private final int duration;

    public SwingAnimationConfiguration(AnimationType type, int duration) {
        this.type = type;
        this.duration = duration;
    }

    /**
     * Animation is hardcoded by the client, so only duration is configurable
     *
     * @param duration Duration of the swing animation in ticks
     */
    public SwingAnimationConfiguration(int duration) {
        this(null, duration);
    }

    public enum AnimationType {
        NONE, WHACK, STAB;

        public String toKey() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getComponentsSection(itemSection);
        ConfigurationSection swingAnimationSection = this.getOrCreateSection(components, "minecraft:swing_animation");

        if (this.type != AnimationType.WHACK) {
            swingAnimationSection.set("type", this.type.toKey());
        }

        if (this.duration != 6) {
            swingAnimationSection.set("duration", this.duration);
        }
    }
}