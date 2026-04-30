package fr.robie.craftengineconverter.api.configuration.loader.models.range_dispatch;

import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.RangeDispatchModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationLoader;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public class CompassRangeDispatchConfigurationLoader implements ModelConfigurationLoader<RangeDispatchModelConfiguration> {


    @Override
    public @Nullable RangeDispatchModelConfiguration load(@NotNull ConfigurationSection section) {
        int scale = section.getInt("scale", 1);
    }
}
