package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public interface BlockStateProperty<T> extends SectionProvider {
    @NotNull String getName();

    @NotNull
    T getValue();

    void serialize(@NotNull ConfigurationSection propertiesSection);
}
