package fr.robie.craftengineconverter.api.configuration.loader.tints;

import fr.robie.craftengineconverter.api.annotations.AutoTintConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.tints.FireworkTintConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.tints.TintConfigurationLoader;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;

@AutoTintConfigurationLoader({"firework", "minecraft:firework"})
public class FireworkTintLoader implements TintConfigurationLoader {

    @Override
    public FireworkTintConfiguration load(ConfigurationSection section) {
        Object o = section.get("default");
        return o == null ? null : new FireworkTintConfiguration(o);
    }
}