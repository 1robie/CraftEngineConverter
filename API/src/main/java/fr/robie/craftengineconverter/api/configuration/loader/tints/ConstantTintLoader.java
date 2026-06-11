package fr.robie.craftengineconverter.api.configuration.loader.tints;

import fr.robie.craftengineconverter.api.annotations.AutoTintConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.tints.ConstantTintConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.tints.TintConfigurationLoader;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jspecify.annotations.NonNull;

@AutoTintConfigurationLoader({"constant", "minecraft:constant"})
public class ConstantTintLoader implements TintConfigurationLoader {

    @Override
    public ConstantTintConfiguration load(@NonNull ConfigurationSection section) {
        Object value = section.get("value");
        return value == null ? null : new ConstantTintConfiguration(value);
    }
}