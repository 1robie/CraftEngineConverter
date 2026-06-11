package fr.robie.craftengineconverter.api.configuration.loader.models.tints;

import fr.robie.craftengineconverter.api.configuration.item.models.tints.TintConfiguration;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TintConfigurationRegistry {
    private static final Map<String, TintConfigurationLoader> LOADERS = new HashMap<>();

    private TintConfigurationRegistry() {
    }

    public static void register(@NotNull String type, @NotNull TintConfigurationLoader loader) {
        LOADERS.put(type, loader);
    }

    @Nullable
    public static TintConfiguration load(@Nullable ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String type = section.getString("type");
        if (type == null) {
            Logger.warn("Tint section is missing 'type' field, skipping.");
            return null;
        }
        if (type.startsWith("minecraft:")) {
            type = type.substring("minecraft:".length());
        }
        TintConfigurationLoader loader = LOADERS.get(type);
        if (loader == null) {
            Logger.info("Unknown tint type '" + type + "', skipping.");
            return null;
        }
        return loader.load(section);
    }

    @NotNull
    public static List<TintConfiguration> loadList(@NotNull ConfigurationSection parent, @NotNull String key) {
        List<TintConfiguration> result = new ArrayList<>();
        List<ConfigurationSection> sections = parent.getSectionList(key);
        for (ConfigurationSection section : sections) {
            TintConfiguration tint = load(section);
            if (tint != null) {
                result.add(tint);
            }
        }
        return result;
    }
}
