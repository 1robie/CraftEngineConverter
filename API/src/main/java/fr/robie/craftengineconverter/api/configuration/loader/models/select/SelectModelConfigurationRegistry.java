package fr.robie.craftengineconverter.api.configuration.loader.models.select;

import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationLoader;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SelectModelConfigurationRegistry {
    private static final Map<String, ModelConfigurationLoader<SelectModelConfiguration<?>>> LOADERS = new HashMap<>();

    public static void register(@NotNull String property, @NotNull ModelConfigurationLoader<SelectModelConfiguration<?>> loader) {
        LOADERS.put(property, loader);
    }

    @Nullable
    public static SelectModelConfiguration<?> load(@Nullable ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String property = section.getString("property");
        if (property == null) {
            return null;
        }

        ModelConfigurationLoader<SelectModelConfiguration<?>> loader = LOADERS.get(property);
        if (loader == null) {
            return null;
        }
        return loader.load(section);
    }
}
