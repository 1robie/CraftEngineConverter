package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public interface BlockStateProperty<T> extends SectionProvider {
    @NotNull String name();

    @NotNull
    T value();

    void serialize(@NotNull ConfigurationSection propertiesSection);
}
