package fr.robie.craftengineconverter.api.configuration;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public interface SectionSerializable {
    void serialize(@NotNull ConfigurationSection configurationSection);
}
