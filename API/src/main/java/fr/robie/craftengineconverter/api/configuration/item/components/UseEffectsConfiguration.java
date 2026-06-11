package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.component.BedrockComponent;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class UseEffectsConfiguration implements ItemConfigurationSerializable, BedrockComponent {

    private final boolean canSprint;
    private final double speedMultiplier;
    private final boolean interactVibrations;

    public UseEffectsConfiguration(boolean canSprint, double speedMultiplier, boolean interactVibrations) {
        this.canSprint = canSprint;
        this.speedMultiplier = speedMultiplier;
        this.interactVibrations = interactVibrations;
    }

    /**
     * To use for bedrock items, as they don't have the can_sprint property is not present in bedrock.
     *
     * @param speedMultiplier
     * @param interactVibrations
     */
    public UseEffectsConfiguration(double speedMultiplier, boolean interactVibrations) {
        this(false, speedMultiplier, interactVibrations);
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
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

    @Override
    public void applyTo(@NotNull JsonObject componentObject) {
        JsonObject useEffectsComponent = new JsonObject();

        if (this.speedMultiplier != 0.2) {
            useEffectsComponent.addProperty("speed_multiplier", Math.max(0.0, Math.min(1.0, this.speedMultiplier)));
        }

        if (!this.interactVibrations) {
            useEffectsComponent.addProperty("interact_vibrations", false);
        }

        componentObject.add("minecraft:use_effects", useEffectsComponent);
    }
}