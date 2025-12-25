package fr.robie.craftengineconverter.converter;

import fr.robie.craftengineconverter.common.configuration.ConverterSettings;

public class BasicConverterSettings implements ConverterSettings {
    private boolean dryRun = false;
    private int threadCount = 1;

    private ConverterSettings backupSettings = null;

    @Override
    public boolean dryRunEnabled() {
        return this.dryRun;
    }

    @Override
    public int threadCount() {
        return this.threadCount;
    }

    @Override
    public void createBackup() {
        this.backupSettings = new BasicConverterSettings()
                .setDryRunEnabled(this.dryRun)
                .setThreadCount(this.threadCount);
    }

    @Override
    public void restoreBackup() {
        if (this.backupSettings != null) {
            this.dryRun = this.backupSettings.dryRunEnabled();
            this.threadCount = this.backupSettings.threadCount();
            this.backupSettings = null;
        }
    }

    @Override
    public ConverterSettings setDryRunEnabled(boolean enabled) {
        this.dryRun = enabled;
        return this;
    }

    @Override
    public ConverterSettings setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }


}
