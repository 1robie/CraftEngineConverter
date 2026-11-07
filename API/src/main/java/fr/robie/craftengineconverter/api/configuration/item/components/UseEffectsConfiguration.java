package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class UseEffectsConfiguration implements ItemConfigurationSerializable {

    private final boolean canSprint;
    private final double speedMultiplier;
    private final boolean interactVibrations;

    public UseEffectsConfiguration(boolean canSprint, double speedMultiplier, boolean interactVibrations) {
        this.canSprint = canSprint;
        this.speedMultiplier = speedMultiplier;
        this.interactVibrations = interactVibrations;
    }

    /**
     * Not every client has the can_sprint property, so this is kept off by default.
     *
     * @param speedMultiplier
     * @param interactVibrations
     */
    public UseEffectsConfiguration(double speedMultiplier, boolean interactVibrations) {
        this(false, speedMultiplier, interactVibrations);
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getComponentsSection(itemSection);
        ConfigurationSection useEffectsSection = this.getOrCreateSection(components, "minecraft:use_effects");

        if (this.canSprint) {
            useEffectsSection.set("can_sprint", true);
        }

        if (this.speedMultiplier != 0.2) {
            useEffectsSection.set("speed_multiplier", Math.max(0.0, Math.min(1.0, this.speedMultiplier)));
        }

        if (!this.interactVibrations) {
            useEffectsSection.set("interact_vibrations", false);
        }
    }

}