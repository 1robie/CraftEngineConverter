package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class WeaponConfiguration implements ItemConfigurationSerializable {
    private final int itemDamagePerAttack;
    private final float disableBlockingForSeconds;

    public WeaponConfiguration(int itemDamagePerAttack, float disableBlockingForSeconds) {
        this.itemDamagePerAttack = itemDamagePerAttack;
        this.disableBlockingForSeconds = disableBlockingForSeconds;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection weaponComponent = this.getOrCreateSection(components, "minecraft:weapon");
        if (this.itemDamagePerAttack != 1) {
            weaponComponent.set("item_damage_per_attack", this.itemDamagePerAttack);
        }
        if (this.disableBlockingForSeconds != 0) {
            weaponComponent.set("disable_blocking_for_seconds", this.disableBlockingForSeconds);
        }
    }
}
