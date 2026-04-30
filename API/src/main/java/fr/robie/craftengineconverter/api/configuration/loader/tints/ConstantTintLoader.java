package fr.robie.craftengineconverter.api.configuration.loader.tints;

import fr.robie.craftengineconverter.api.annotations.AutoTintConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.tints.ConstantTintConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.tints.TintConfigurationLoader;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;

@AutoTintConfigurationLoader({"constant", "minecraft:constant"})
public class ConstantTintLoader implements TintConfigurationLoader {

    @Override
    public ConstantTintConfiguration load(ConfigurationSection section) {
        Object value = section.get("value");
        return value == null ? null : new ConstantTintConfiguration(value);
    }
}