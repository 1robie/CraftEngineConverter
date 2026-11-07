package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class MaxDamageComponent implements ItemConfigurationSerializable {
    private final int maxDamage;

    public MaxDamageComponent(int maxDamage) {
        assert maxDamage >= 0 : "Max damage must be non-negative";
        this.maxDamage = maxDamage;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getComponentsSection(itemSection);
        components.set("minecraft:max_damage", this.maxDamage);
    }
}
