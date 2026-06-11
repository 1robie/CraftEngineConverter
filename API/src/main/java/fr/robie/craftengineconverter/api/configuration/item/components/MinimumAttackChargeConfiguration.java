package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class MinimumAttackChargeConfiguration implements ItemConfigurationSerializable {
    private final float minimumAttackCharge;

    public MinimumAttackChargeConfiguration(float minimumAttackCharge) {
        this.minimumAttackCharge = minimumAttackCharge;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        if (this.minimumAttackCharge < 0 || this.minimumAttackCharge > 1) {
            return;
        }
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection attackChargeComponent = this.getOrCreateSection(components, "minecraft:attack_charge");
        attackChargeComponent.set("minimum_attack_charge", this.minimumAttackCharge);
    }
}
