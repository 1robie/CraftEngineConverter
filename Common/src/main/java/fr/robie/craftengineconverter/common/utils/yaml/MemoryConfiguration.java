package fr.robie.craftengineconverter.common.utils.yaml;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public class MemoryConfiguration extends MemorySection implements fr.robie.craftengineconverter.api.yaml.Configuration {
    protected fr.robie.craftengineconverter.api.yaml.Configuration defaults;
    protected MemoryConfigurationOptions options;


    public MemoryConfiguration() {
    }


    public MemoryConfiguration(@Nullable fr.robie.craftengineconverter.api.yaml.Configuration defaults) {
        this.defaults = defaults;
    }

    @Override
    public void addDefault(@NotNull String path, @Nullable Object value) {
        Preconditions.checkArgument(path != null, "Path may not be null");

        if (this.defaults == null) {
            this.defaults = new MemoryConfiguration();
        }

        this.defaults.set(path, value);
    }

    @Override
    public void addDefaults(@NotNull Map<String, Object> defaults) {
        Preconditions.checkArgument(defaults != null, "Defaults may not be null");

        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            this.addDefault(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void addDefaults(@NotNull fr.robie.craftengineconverter.api.yaml.Configuration defaults) {
        Preconditions.checkArgument(defaults != null, "Defaults may not be null");

        for (String key : defaults.getKeys(true)) {
            if (!defaults.isConfigurationSection(key)) {
                this.addDefault(key, defaults.get(key));
            }
        }
    }

    @Override
    public void setDefaults(@NotNull fr.robie.craftengineconverter.api.yaml.Configuration defaults) {
        Preconditions.checkNotNull(defaults, "Defaults may not be null");

        this.defaults = defaults;
    }

    @Override
    @Nullable
    public fr.robie.craftengineconverter.api.yaml.Configuration getDefaults() {
        return this.defaults;
    }

    @Nullable
    @Override
    public fr.robie.craftengineconverter.api.yaml.ConfigurationSection getParent() {
        return null;
    }

    @Override
    @NotNull
    public MemoryConfigurationOptions options() {
        if (this.options == null) {
            this.options = new MemoryConfigurationOptions(this);
        }

        return this.options;
    }
}
