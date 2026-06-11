package fr.robie.craftengineconverter.api.configuration.loader.tints;

import fr.robie.craftengineconverter.api.annotations.AutoTintConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.tints.CustomModelDataTintConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.tints.TintConfigurationLoader;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jspecify.annotations.NonNull;

@AutoTintConfigurationLoader({"custom_model_data", "minecraft:custom_model_data"})
public class CustomModelDataTintLoader implements TintConfigurationLoader {

    @Override
    public CustomModelDataTintConfiguration load(@NonNull ConfigurationSection section) {
        int index = section.getInt("index", 0);
        Object defaultValue = section.get("default");
        return new CustomModelDataTintConfiguration(index, defaultValue);
    }
}