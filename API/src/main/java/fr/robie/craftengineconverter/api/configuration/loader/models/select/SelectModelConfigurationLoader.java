package fr.robie.craftengineconverter.api.configuration.loader.models.select;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationRegistry;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public interface SelectModelConfigurationLoader<T> {

    @NotNull
    String getPropertyName();

    SelectModelConfiguration<T> load(@NotNull ConfigurationSection section);

    default ModelConfiguration loadModel(ConfigurationSection modelSection) {
        return ModelConfigurationRegistry.load(modelSection);
    }
}
