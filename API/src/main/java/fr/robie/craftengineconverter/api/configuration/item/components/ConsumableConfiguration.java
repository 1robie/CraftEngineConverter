package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.component.BedrockComponent;
import fr.robie.craftengineconverter.api.configuration.item.AbstractEffectsConfiguration;
import fr.robie.craftengineconverter.api.enums.item.component.ConsumableAnimation;
import fr.robie.craftengineconverter.api.utils.item.component.ConsumeEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class ConsumableConfiguration extends AbstractEffectsConfiguration implements BedrockComponent {

    private final String sound;
    private final boolean hasConsumeParticles;
    private final double consumeSeconds;
    private final ConsumableAnimation animation;
    private final List<ConsumeEffect> onConsumeEffects;

    public ConsumableConfiguration(String sound, boolean hasConsumeParticles, double consumeSeconds, ConsumableAnimation animation, List<ConsumeEffect> onConsumeEffects) {
        this.sound = sound;
        this.hasConsumeParticles = hasConsumeParticles;
        this.consumeSeconds = consumeSeconds;
        this.animation = animation;
        this.onConsumeEffects = onConsumeEffects;
    }

    public ConsumableConfiguration(double consumeSeconds, ConsumableAnimation animation, List<ConsumeEffect> onConsumeEffects) {
        this(null, true, consumeSeconds, animation, onConsumeEffects);
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection consumableSection = this.getOrCreateSection(components, "minecraft:consumable");

        if (!this.sound.equals("entity.generic.eat")) {
            consumableSection.set("sound", this.sound);
        }
        if (!this.hasConsumeParticles) {
            consumableSection.set("has_consume_particles", false);
        }
        if (this.consumeSeconds != 1.6) {
            consumableSection.set("consume_seconds", this.consumeSeconds);
        }
        if (this.animation != ConsumableAnimation.EAT) {
            consumableSection.set("animation", this.animation.toKey());
        }

        if (this.onConsumeEffects != null && !this.onConsumeEffects.isEmpty()) {
            consumableSection.set("on_consume_effects", this.serializeEffects(this.onConsumeEffects));
        }
    }

    @Override
    public void applyTo(@NotNull JsonObject componentObject) {
        JsonObject consumableObject = new JsonObject();
        if (this.animation != ConsumableAnimation.EAT) {
            consumableObject.addProperty("animation", this.animation.toKey());
        }
        if (this.onConsumeEffects != null && !this.onConsumeEffects.isEmpty()) {
            consumableObject.add("on_consume_effects", this.serializeEffectsToJsonArray(this.onConsumeEffects));
        }
        componentObject.add("minecraft:consumable", consumableObject);
    }
}