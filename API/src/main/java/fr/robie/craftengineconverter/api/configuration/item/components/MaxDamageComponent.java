package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.component.BedrockComponent;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class MaxDamageComponent implements ItemConfigurationSerializable, BedrockComponent {
    private final int maxDamage;

    public MaxDamageComponent(int maxDamage) {
        assert maxDamage >= 0 : "Max damage must be non-negative";
        this.maxDamage = maxDamage;
    }

    @Override
    public void applyTo(@NotNull JsonObject componentObject) {
        componentObject.addProperty("minecraft:max_damage", this.maxDamage);
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        components.set("minecraft:max_damage", this.maxDamage);
    }
}
