package fr.robie.craftengineconverter.api.configuration.loader.tints;

import fr.robie.craftengineconverter.api.annotations.AutoTintConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.tints.DyeTintConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.tints.TintConfigurationLoader;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jspecify.annotations.NonNull;

@AutoTintConfigurationLoader({"dye", "minecraft:dye"})
public class DyeTintLoader implements TintConfigurationLoader {

    @Override
    public DyeTintConfiguration load(@NonNull ConfigurationSection section) {
        Object o = section.get("default");
        return o == null ? null : new DyeTintConfiguration(o);
    }
}