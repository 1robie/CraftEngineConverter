package fr.robie.craftengineconverter.api.configuration.loader.tints;

import fr.robie.craftengineconverter.api.annotations.AutoTintConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.tints.MapColorTintConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.tints.TintConfigurationLoader;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jspecify.annotations.NonNull;

@AutoTintConfigurationLoader({"map_color", "minecraft:map_color"})
public class MapColorTintLoader implements TintConfigurationLoader {

    @Override
    public MapColorTintConfiguration load(@NonNull ConfigurationSection section) {
        Object defaultValue = section.get("default");
        return defaultValue == null ? null : new MapColorTintConfiguration(defaultValue);
    }
}