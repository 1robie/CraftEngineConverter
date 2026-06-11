package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class DamageTypeConfiguration implements ItemConfigurationSerializable {
    private final String damageType;

    public DamageTypeConfiguration(@NotNull String damageType) {
        this.damageType = damageType;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        if (this.damageType.isEmpty()) {
            return;
        }
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        components.set("minecraft:damage_type", this.damageType);
    }
}
