package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class PiercingWeaponConfiguration implements ItemConfigurationSerializable {

    private final boolean dealsKnockback;
    private final boolean dismounts;
    private final String sound;
    private final String hitSound;

    public PiercingWeaponConfiguration(boolean dealsKnockback, boolean dismounts, String sound, String hitSound) {
        this.dealsKnockback = dealsKnockback;
        this.dismounts = dismounts;
        this.sound = sound;
        this.hitSound = hitSound;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection piercingSection = this.getOrCreateSection(components, "minecraft:piercing_weapon");

        if (!this.dealsKnockback) {
            piercingSection.set("deals_knockback", false);
        }

        if (this.dismounts) {
            piercingSection.set("dismounts", true);
        }

        if (this.sound != null && !this.sound.isBlank()) {
            piercingSection.set("sound", this.sound);
        }

        if (this.hitSound != null && !this.hitSound.isBlank()) {
            piercingSection.set("hit_sound", this.hitSound);
        }
    }
}