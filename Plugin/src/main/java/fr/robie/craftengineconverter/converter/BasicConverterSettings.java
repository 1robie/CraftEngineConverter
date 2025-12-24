package fr.robie.craftengineconverter.converter;

import fr.robie.craftengineconverter.common.configuration.ConverterSettings;

public class BasicConverterSettings implements ConverterSettings {
    private boolean dryRun = false;

    @Override
    public boolean dryRunEnabled() {
        return this.dryRun;
    }

    @Override
    public ConverterSettings setDryRunEnabled(boolean enabled) {
        this.dryRun = enabled;
        return this;
    }
}
