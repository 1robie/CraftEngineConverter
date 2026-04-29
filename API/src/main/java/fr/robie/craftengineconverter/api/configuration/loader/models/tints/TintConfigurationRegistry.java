package fr.robie.craftengineconverter.api.configuration.loader.models.tints;

import fr.robie.craftengineconverter.api.configuration.item.models.tints.*;
import fr.robie.craftengineconverter.api.logger.LogType;
import fr.robie.craftengineconverter.api.logger.Logger;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TintConfigurationRegistry {
    private static final Map<String, TintConfigurationLoader> LOADERS = new HashMap<>();

    static {
        register("constant", TintConfigurationRegistry::loadConstant);
        register("dye", TintConfigurationRegistry::loadDye);
        register("firework", TintConfigurationRegistry::loadFirework);
        register("grass", TintConfigurationRegistry::loadGrass);
        register("map_color", TintConfigurationRegistry::loadMapColor);
        register("potion", TintConfigurationRegistry::loadPotion);
        register("team", TintConfigurationRegistry::loadTeam);
        register("custom_model_data", TintConfigurationRegistry::loadCustomModelData);
    }

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
            Logger.info("Tint section is missing 'type' field, skipping.", LogType.WARNING);
            return null;
        }
        if (type.startsWith("minecraft:")) {
            type = type.substring("minecraft:".length());
        }
        TintConfigurationLoader loader = LOADERS.get(type);
        if (loader == null) {
            Logger.info("Unknown tint type '" + type + "', skipping.", LogType.WARNING);
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

    private static TintConfiguration loadConstant(ConfigurationSection section) {
        Object value = section.get("value");
        if (value == null) {
            return null;
        }
        return new ConstantTintConfiguration(value);
    }

    private static TintConfiguration loadCustomModelData(ConfigurationSection section) {
        int index = section.getInt("index", 0);
        Object defaultValue = section.get("default");
        return new CustomModelDataTintConfiguration(index, defaultValue);
    }

    private static TintConfiguration loadDye(ConfigurationSection section) {
        Object o = section.get("default");
        return o == null ? null : new DyeTintConfiguration(o);
    }

    private static TintConfiguration loadFirework(ConfigurationSection section) {
        Object o = section.get("default");
        return o == null ? null : new FireworkTintConfiguration(o);
    }

    private static TintConfiguration loadGrass(ConfigurationSection section) {
        float temperature = (float) section.getDouble("temperature", 0.0);
        float downfall = (float) section.getDouble("downfall", 0.0);
        return new GrassTintConfiguration(temperature, downfall);
    }

    private static TintConfiguration loadMapColor(ConfigurationSection section) {
        Object o = section.get("default");
        return o == null ? null : new MapColorTintConfiguration(o);
    }

    private static TintConfiguration loadPotion(ConfigurationSection section) {
        Object o = section.get("default");
        return o == null ? null : new PotionTintConfiguration(o);
    }

    private static TintConfiguration loadTeam(ConfigurationSection section) {
        Object o = section.get("default");
        return o == null ? null : new TeamTintConfiguration(o);
    }
}
