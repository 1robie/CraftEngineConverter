package fr.robie.craftengineconverter.api.configuration;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public interface SectionSerializable {
    void serialize(@NotNull ConfigurationSection configurationSection);
}
