package fr.robie.craftengineconverter.api.configuration.loader.models.composite;

import fr.robie.craftengineconverter.api.annotations.AutoModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.composite.CompositeModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationRegistry;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@AutoModelConfigurationLoader({"composite", "minecraft:composite"})
public class CompositeModelConfigurationLoader implements ModelConfigurationLoader<ModelConfiguration> {
    @Override
    public @Nullable ModelConfiguration load(@NotNull ConfigurationSection section) {
        CompositeModelConfiguration config = new CompositeModelConfiguration();

        List<ConfigurationSection> models = section.getSectionList("models");
        for (ConfigurationSection modelSection : models) {
            ModelConfiguration child = ModelConfigurationRegistry.load(modelSection);
            if (child != null) {
                config.addModel(child);
            }
        }

        return config;
    }
}
