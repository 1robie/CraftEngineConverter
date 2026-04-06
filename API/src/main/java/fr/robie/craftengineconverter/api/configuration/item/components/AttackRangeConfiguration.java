package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.component.BedrockComponent;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class AttackRangeConfiguration implements ItemConfigurationSerializable, BedrockComponent {

    private final double minReach;
    private final double maxReach;
    private final double minCreativeReach;
    private final double maxCreativeReach;
    private final double hitboxMargin;
    private final double mobFactor;

    public AttackRangeConfiguration(double minReach, double maxReach, double minCreativeReach, double maxCreativeReach, double hitboxMargin, double mobFactor) {
        this.minReach = minReach;
        this.maxReach = maxReach;
        this.minCreativeReach = minCreativeReach;
        this.maxCreativeReach = maxCreativeReach;
        this.hitboxMargin = hitboxMargin;
        this.mobFactor = mobFactor;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection attackRangeSection = this.getOrCreateSection(components, "minecraft:attack_range");

        if (this.minReach != 0.0) {
            attackRangeSection.set("min_reach", this.clamp(this.minReach, 0.0, 64.0));
        }

        if (this.maxReach != 3.0) {
            attackRangeSection.set("max_reach", this.clamp(this.maxReach, 0.0, 64.0));
        }

        if (this.minCreativeReach != 0.0) {
            attackRangeSection.set("min_creative_reach", this.clamp(this.minCreativeReach, 0.0, 64.0));
        }

        if (this.maxCreativeReach != 5.0) {
            attackRangeSection.set("max_creative_reach", this.clamp(this.maxCreativeReach, 0.0, 64.0));
        }

        if (this.hitboxMargin != 0.3) {
            attackRangeSection.set("hitbox_margin", this.clamp(this.hitboxMargin, 0.0, 1.0));
        }

        if (this.mobFactor != 1.0) {
            attackRangeSection.set("mob_factor", this.clamp(this.mobFactor, 0.0, 2.0));
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void applyTo(@NotNull JsonObject componentObject) {
        JsonObject attackRangeComponent = new JsonObject();

        if (this.minReach != 0.0) {
            attackRangeComponent.addProperty("min_reach", this.clamp(this.minReach, 0.0, 64.0));
        }

        if (this.maxReach != 3.0) {
            attackRangeComponent.addProperty("max_reach", this.clamp(this.maxReach, 0.0, 64.0));
        }

        if (this.minCreativeReach != 0.0) {
            attackRangeComponent.addProperty("min_creative_reach", this.clamp(this.minCreativeReach, 0.0, 64.0));
        }

        if (this.maxCreativeReach != 5.0) {
            attackRangeComponent.addProperty("max_creative_reach", this.clamp(this.maxCreativeReach, 0.0, 64.0));
        }

        if (this.hitboxMargin != 0.3) {
            attackRangeComponent.addProperty("hitbox_margin", this.clamp(this.hitboxMargin, 0.0, 1.0));
        }

        if (this.mobFactor != 1.0) {
            attackRangeComponent.addProperty("mob_factor", this.clamp(this.mobFactor, 0.0, 2.0));
        }

        componentObject.add("minecraft:attack_range", attackRangeComponent);
    }
}