package fr.robie.craftengineconverter.api.configuration.loader.tints;

import fr.robie.craftengineconverter.api.annotations.AutoTintConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.tints.PotionTintConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.tints.TintConfigurationLoader;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jspecify.annotations.NonNull;

@AutoTintConfigurationLoader({"potion", "minecraft:potion"})
public class PotionTintLoader implements TintConfigurationLoader {

    @Override
    public PotionTintConfiguration load(@NonNull ConfigurationSection section) {
        Object o = section.get("default");
        return o == null ? null : new PotionTintConfiguration(o);
    }
}