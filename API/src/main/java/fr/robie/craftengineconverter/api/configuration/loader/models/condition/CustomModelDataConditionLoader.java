package fr.robie.craftengineconverter.api.configuration.loader.models.condition;

import fr.robie.craftengineconverter.api.annotations.AutoModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.condition.CustomModelDataConditionConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.AbstractConditionLoader;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AutoModelConfigurationLoader({"custom_model_data", "minecraft:custom_model_data"})
public class CustomModelDataConditionLoader extends AbstractConditionLoader {

    @Override
    public @Nullable ModelConfiguration load(@NotNull ConfigurationSection section) {
        int index = section.getInt("index", 0);

        CustomModelDataConditionConfiguration config = new CustomModelDataConditionConfiguration(index);
        this.loadBranches(config, section);
        return config;
    }
}