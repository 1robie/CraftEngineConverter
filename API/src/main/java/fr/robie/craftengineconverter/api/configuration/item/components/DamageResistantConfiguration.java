package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class DamageResistantConfiguration implements ItemConfigurationSerializable {
    private final String damageResistantType;

    public DamageResistantConfiguration(@NotNull String damageResistantType) {
        this.damageResistantType = damageResistantType;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection damageResistantComponent = this.getOrCreateSection(components, "minecraft:damage_resistant");
        damageResistantComponent.set("types", this.damageResistantType);
    }
}
