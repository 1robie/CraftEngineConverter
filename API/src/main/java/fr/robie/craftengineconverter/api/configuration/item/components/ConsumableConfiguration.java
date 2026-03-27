package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.AbstractEffectsConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ConsumableConfiguration extends AbstractEffectsConfiguration {

    private final String sound;
    private final boolean hasConsumeParticles;
    private final double consumeSeconds;
    private final Animation animation;
    private final List<ConsumeEffect> onConsumeEffects;

    public ConsumableConfiguration(String sound, boolean hasConsumeParticles, double consumeSeconds, Animation animation, List<ConsumeEffect> onConsumeEffects) {
        this.sound = sound;
        this.hasConsumeParticles = hasConsumeParticles;
        this.consumeSeconds = consumeSeconds;
        this.animation = animation;
        this.onConsumeEffects = onConsumeEffects;
    }

    public enum Animation {
        NONE, EAT, DRINK, BLOCK, BOW, SPEAR, CROSSBOW, SPYGLASS, TOOT_HORN, BRUSH, BUNDLE, TRIDENT;

        public String toKey() {
            return this.name().toLowerCase();
        }
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
        if (this.animation != Animation.EAT) {
            consumableSection.set("animation", this.animation.toKey());
        }

        if (this.onConsumeEffects != null && !this.onConsumeEffects.isEmpty()) {
            consumableSection.set("on_consume_effects", this.serializeEffects(this.onConsumeEffects));
        }
    }
}