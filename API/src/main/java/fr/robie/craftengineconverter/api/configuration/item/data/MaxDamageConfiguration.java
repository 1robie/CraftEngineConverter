package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class MaxDamageConfiguration implements ItemConfigurationSerializable {
    private final int maxDamage;

    public MaxDamageConfiguration(int maxDamage) {
        this.maxDamage = maxDamage;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        this.getOrCreateSection(itemSection, "data").set("max-damage", this.maxDamage);
    }
}
