package fr.robie.craftengineconverter.api.configuration.events;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public interface Event {
    void serialize(@NotNull ConfigurationSection section);
}
