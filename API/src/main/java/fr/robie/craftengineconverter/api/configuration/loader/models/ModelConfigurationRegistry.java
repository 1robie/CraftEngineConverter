package fr.robie.craftengineconverter.api.configuration.loader.models;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ModelConfigurationRegistry {
    private static final Map<String, ModelConfigurationLoader<?>> LOADERS = new HashMap<>();

    private ModelConfigurationRegistry() {
        throw new UnsupportedOperationException("ModelConfigurationRegistry is a utility class and cannot be instantiated.");
    }

    public static void register(@NotNull String type, @NotNull ModelConfigurationLoader<?> loader) {
        LOADERS.put(type, loader);
    }

    @Nullable
    public static ModelConfiguration load(@Nullable ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String type = section.getString("type", "model");
        ModelConfigurationLoader<?> loader = LOADERS.get(type);
        if (loader == null) {
            Logger.warn("Unknown model type '" + type + "', skipping.");
            return null;
        }
        return loader.load(section);
    }
}
