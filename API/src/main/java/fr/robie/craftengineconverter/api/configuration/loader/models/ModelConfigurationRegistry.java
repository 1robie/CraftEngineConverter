package fr.robie.craftengineconverter.api.configuration.loader.models;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.composite.CompositeModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.loader.models.condition.*;
import fr.robie.craftengineconverter.api.configuration.loader.models.model.SimpleModelConfigurationLoader;
import fr.robie.craftengineconverter.api.logger.LogType;
import fr.robie.craftengineconverter.api.logger.Logger;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ModelConfigurationRegistry {
    private static final Map<String, ModelConfigurationLoader> LOADERS = new HashMap<>();

    static {
        register("model", new SimpleModelConfigurationLoader());
        register("composite", new CompositeModelConfigurationLoader());
        register("condition", new ConditionModelConfigurationLoader());
        register("has_component", new HasComponentConditionLoader());
        register("custom_model_data", new CustomModelDataConditionLoader());
        register("keybind_down", new KeybindDownConditionLoader());
        register("component", new ComponentConditionLoader());
    }

    private ModelConfigurationRegistry() {
        throw new UnsupportedOperationException("ModelConfigurationRegistry is a utility class and cannot be instantiated.");
    }

    public static void register(@NotNull String type, @NotNull ModelConfigurationLoader loader) {
        LOADERS.put(type, loader);
    }

    @Nullable
    public static ModelConfiguration load(@Nullable ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String type = section.getString("type", "model");
        if (type.startsWith("minecraft:")) {
            type = type.substring("minecraft:".length());
        }
        ModelConfigurationLoader loader = LOADERS.get(type);
        if (loader == null) {
            Logger.info("Unknown model type '" + type + "', skipping.", LogType.WARNING);
            return null;
        }
        return loader.load(section);
    }
}
