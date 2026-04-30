package fr.robie.craftengineconverter.api.configuration.loader.tints;

import fr.robie.craftengineconverter.api.annotations.AutoTintConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.tints.GrassTintConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.tints.TintConfigurationLoader;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;

@AutoTintConfigurationLoader({"grass", "minecraft:grass"})
public class GrassTintLoader implements TintConfigurationLoader {

    @Override
    public GrassTintConfiguration load(ConfigurationSection section) {
        float temperature = (float) section.getDouble("temperature", 0.0);
        float downfall = (float) section.getDouble("downfall", 0.0);
        return new GrassTintConfiguration(temperature, downfall);
    }
}