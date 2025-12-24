package fr.robie.craftengineconverter.common.configuration;

import org.jetbrains.annotations.Contract;

public interface ConverterSettings {
    boolean dryRunEnabled();

    @Contract(value = "_ -> this", mutates = "this")
    ConverterSettings setDryRunEnabled(boolean enabled);
}
