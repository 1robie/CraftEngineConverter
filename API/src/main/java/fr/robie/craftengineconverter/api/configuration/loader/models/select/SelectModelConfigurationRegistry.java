package fr.robie.craftengineconverter.api.configuration.loader.models.select;

import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SelectModelConfigurationRegistry {
    private static final Map<String, SelectModelConfigurationLoader<?>> LOADERS = new HashMap<>();

    static {
        registerLoader(new ChargeTypeSelectConfigurationLoader());
        registerLoader(new BlockStateSelectConfigurationLoader());
        registerLoader(new ComponentSelectConfigurationLoader());
        registerLoader(new CustomModelDataSelectConfigurationLoader());
        registerLoader(new DisplayContentSelectConfigurationLoader());
        registerLoader(new LocalTimeSelectConfigurationLoader());
        registerLoader(new MainHandSelectConfigurationLoader());
    }

    public static void registerLoader(SelectModelConfigurationLoader<?> loader) {
        LOADERS.put(loader.getPropertyName(), loader);
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

        SelectModelConfigurationLoader<?> loader = LOADERS.get(property);
        if (loader == null) {
            if (property.startsWith("minecraft:")) {
                loader = LOADERS.get(property.substring("minecraft:".length()));
            } else {
                loader = LOADERS.get("minecraft:" + property);
            }
        }

        if (loader == null) {
            return null;
        }
        return loader.load(section);
    }
}
