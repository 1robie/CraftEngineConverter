package fr.robie.craftengineconverter.api.configuration.loader.models.tints;

import fr.robie.craftengineconverter.api.configuration.item.models.tints.TintConfiguration;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface TintConfigurationLoader {
    @Nullable
    TintConfiguration load(@NotNull ConfigurationSection section);
}
 