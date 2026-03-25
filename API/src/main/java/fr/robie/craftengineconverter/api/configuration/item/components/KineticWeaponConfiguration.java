package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class KineticWeaponConfiguration implements ItemConfigurationSerializable {
    private final long delayTicks;
    private final double damageMultiplier;
    private final double forwardMovement;
    private final String sound;
    private final String hitSound;
    private final KineticConditions dismountConditions;
    private final KineticConditions knockbackConditions;
    private final KineticConditions damageConditions;

    public KineticWeaponConfiguration(long delayTicks, double damageMultiplier, double forwardMovement, String sound, String hitSound, KineticConditions dismountConditions, KineticConditions knockbackConditions, KineticConditions damageConditions) {
        this.delayTicks = delayTicks;
        this.damageMultiplier = damageMultiplier;
        this.forwardMovement = forwardMovement;
        this.sound = sound;
        this.hitSound = hitSound;
        this.dismountConditions = dismountConditions;
        this.knockbackConditions = knockbackConditions;
        this.damageConditions = damageConditions;
    }

    public record KineticConditions(long maxDurationTicks, double minSpeed, double minRelativeSpeed) {

        public void serialize(@NotNull ConfigurationSection section) {
            if (this.maxDurationTicks > 0) {
                section.set("max_duration_ticks", this.maxDurationTicks);
            }
            if (this.minSpeed > 0.0) {
                section.set("min_speed", this.minSpeed);
            }
            if (this.minRelativeSpeed > 0.0) {
                section.set("min_relative_speed", this.minRelativeSpeed);
            }
        }
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection kineticSection = this.getOrCreateSection(components, "minecraft:kinetic_weapon");

        if (this.delayTicks > 0) {
            kineticSection.set("delay_ticks", this.delayTicks);
        }

        if (this.damageMultiplier != 1.0) {
            kineticSection.set("damage_multiplier", this.damageMultiplier);
        }

        if (this.forwardMovement != 0.0) {
            kineticSection.set("forward_movement", this.forwardMovement);
        }

        if (this.sound != null && !this.sound.isBlank()) {
            kineticSection.set("sound", this.sound);
        }

        if (this.hitSound != null && !this.hitSound.isBlank()) {
            kineticSection.set("hit_sound", this.hitSound);
        }

        if (this.dismountConditions != null) {
            this.dismountConditions.serialize(this.getOrCreateSection(kineticSection, "dismount_conditions"));
        }

        if (this.knockbackConditions != null) {
            this.knockbackConditions.serialize(this.getOrCreateSection(kineticSection, "knockback_conditions"));
        }

        if (this.damageConditions != null) {
            this.damageConditions.serialize(this.getOrCreateSection(kineticSection, "damage_conditions"));
        }
    }
}