package fr.robie.craftengineconverter.api.configuration.loader.models;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.messageflow.logger.Logger;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModelConfigurationRegistry {
    private static final Map<String, ModelConfigurationLoader<?>> LOADERS = new HashMap<>();
    /**
     * Types with no Bedrock equivalent that are still worth naming, so a skip reads as a known limitation rather
     * than as a typo. {@code special} has left this set: it now resolves to its {@code base} model — see
     * {@code SpecialModelConfigurationLoader}.
     */
    private static final Set<String> BEDROCK_UNREPRESENTABLE_TYPES = Set.of();

    private static String stripNamespace(String type) {
        int colon = type.indexOf(':');
        return colon < 0 ? type : type.substring(colon + 1);
    }

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
            loader = LOADERS.get(stripNamespace(type));
        }
        if (loader == null) {
            if (BEDROCK_UNREPRESENTABLE_TYPES.contains(stripNamespace(type))) {
                Logger.debug("Model type '" + type + "' has no Bedrock equivalent, using a plain definition");
            } else {
                Logger.warn("Unknown model type '" + type + "', skipping.");
            }
            return null;
        }
        return loader.load(section);
    }
}
