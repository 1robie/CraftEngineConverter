package fr.robie.craftengineconverter.common.configuration;

import org.jetbrains.annotations.Contract;

public interface ConverterSettings {
    boolean dryRunEnabled();
    int threadCount();

    void createBackup();
    void restoreBackup();

    @Contract(value = "_ -> this", mutates = "this")
    ConverterSettings setDryRunEnabled(boolean enabled);
    @Contract(value = "_ -> this", mutates = "this")
    ConverterSettings setThreadCount(int threadCount);
}
