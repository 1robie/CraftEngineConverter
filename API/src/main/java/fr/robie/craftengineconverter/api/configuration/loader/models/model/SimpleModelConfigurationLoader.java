package fr.robie.craftengineconverter.api.configuration.loader.models.model;

import fr.robie.craftengineconverter.api.annotations.AutoModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.GenerationConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.SimpleModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.tints.TintConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.loader.models.tints.TintConfigurationRegistry;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


@AutoModelConfigurationLoader({"model", "minecraft:model"})
public class SimpleModelConfigurationLoader implements ModelConfigurationLoader<ModelConfiguration> {

    @Override
    public @Nullable ModelConfiguration load(@NotNull ConfigurationSection section) {
        String path = section.getString("path");
        if (path == null) {
            return null;
        }

        SimpleModelConfiguration config = new SimpleModelConfiguration(path);

        ConfigurationSection generationSection = section.getConfigurationSection("generation");
        if (generationSection != null) {
            String parent = generationSection.getString("parent");
            GenerationConfiguration generation = new GenerationConfiguration(parent);

            ConfigurationSection texturesSection = generationSection.getConfigurationSection("textures");
            if (texturesSection != null) {
                for (String key : texturesSection.getKeys(false)) {
                    String value = texturesSection.getString(key);
                    if (value != null) {
                        generation.addTexture(key, value);
                    }
                }
            }
            config.setGeneration(generation);
        }

        List<TintConfiguration> tints = TintConfigurationRegistry.loadList(section, "tints");
        tints.forEach(config::addTint);

        return config;
    }
}
