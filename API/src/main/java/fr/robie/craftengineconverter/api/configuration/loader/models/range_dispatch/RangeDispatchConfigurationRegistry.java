package fr.robie.craftengineconverter.api.configuration.loader.models.range_dispatch;

import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.RangeDispatchModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RangeDispatchConfigurationRegistry {
    private static final Map<String, ModelConfigurationLoader<RangeDispatchModelConfiguration>> LOADERS = new HashMap<>();

    public static void register(@NotNull String property, @NotNull ModelConfigurationLoader<RangeDispatchModelConfiguration> loader) {
        LOADERS.put(property, loader);
    }

    @Nullable
    public static RangeDispatchModelConfiguration load(@Nullable fr.robie.craftengineconverter.api.yaml.ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String property = section.getString("property");
        if (property == null) {
            return null;
        }

        ModelConfigurationLoader<RangeDispatchModelConfiguration> loader = LOADERS.get(property);
        if (loader == null) {
            return null;
        }
        return loader.load(section);
    }
}
