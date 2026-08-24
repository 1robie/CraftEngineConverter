package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code minecraft:potion_contents} component. CraftEngine has no dedicated processor for it, so it is written as
 * a raw component under {@code data.components}.
 */
public class PotionContentsConfiguration implements ItemConfigurationSerializable {
    private final List<CustomEffect> customEffects = new ArrayList<>();
    private @Nullable String potion;
    private @Nullable Integer customColor;

    public PotionContentsConfiguration addEffect(@NotNull CustomEffect effect) {
        this.customEffects.add(effect);
        return this;
    }

    /** The base potion this stack is brewed from, e.g. {@code minecraft:swiftness}. */
    public PotionContentsConfiguration setPotion(@Nullable String potion) {
        this.potion = potion;
        return this;
    }

    public PotionContentsConfiguration setCustomColor(@Nullable Integer customColor) {
        this.customColor = customColor;
        return this;
    }

    public boolean hasContents() {
        return !this.customEffects.isEmpty() || this.potion != null || this.customColor != null;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        if (!this.hasContents()) {
            return;
        }

        ConfigurationSection components = this.getComponentsSection(itemSection);
        ConfigurationSection potionContents = this.getOrCreateSection(components, "minecraft:potion_contents");

        if (this.potion != null) {
            potionContents.set("potion", this.potion);
        }
        if (this.customColor != null) {
            potionContents.set("custom_color", this.customColor);
        }
        if (this.customEffects.isEmpty()) {
            return;
        }

        List<Map<String, Object>> effects = new ArrayList<>(this.customEffects.size());
        for (CustomEffect effect : this.customEffects) {
            effects.add(effect.serialize());
        }
        potionContents.set("custom_effects", effects);
    }

    /**
     * One entry of {@code custom_effects}. Only values that differ from the vanilla defaults are written, so a plain
     * effect stays a three-key map rather than carrying every flag.
     */
    public record CustomEffect(@NotNull String id, int duration, int amplifier,
                               @Nullable Boolean ambient, @Nullable Boolean showParticles, @Nullable Boolean showIcon) {

        private Map<String, Object> serialize() {
            Map<String, Object> effect = new LinkedHashMap<>();
            effect.put("id", this.id);
            effect.put("duration", this.duration);
            effect.put("amplifier", this.amplifier);
            if (this.ambient != null) {
                effect.put("ambient", this.ambient);
            }
            if (this.showParticles != null) {
                effect.put("show_particles", this.showParticles);
            }
            if (this.showIcon != null) {
                effect.put("show_icon", this.showIcon);
            }
            return effect;
        }
    }
}
