package fr.robie.craftengineconverter.common.utils.yaml.file;

import fr.robie.craftengineconverter.common.utils.yaml.MemoryConfiguration;
import fr.robie.craftengineconverter.common.utils.yaml.MemoryConfigurationOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class FileConfigurationOptions extends MemoryConfigurationOptions {
    private List<String> header = Collections.emptyList();
    private List<String> footer = Collections.emptyList();
    private boolean parseComments = true;

    protected FileConfigurationOptions(@NotNull MemoryConfiguration configuration) {
        super(configuration);
    }

    @NotNull
    @Override
    public FileConfiguration configuration() {
        return (FileConfiguration) super.configuration();
    }

    @NotNull
    @Override
    public FileConfigurationOptions copyDefaults(boolean value) {
        super.copyDefaults(value);
        return this;
    }

    @NotNull
    @Override
    public FileConfigurationOptions pathSeparator(char value) {
        super.pathSeparator(value);
        return this;
    }

    @NotNull
    public List<String> getHeader() {
        return this.header;
    }

    @NotNull
    public FileConfigurationOptions setHeader(@Nullable List<String> value) {
        this.header = (value == null) ? Collections.emptyList() : Collections.unmodifiableList(value);
        return this;
    }

    @NotNull
    public List<String> getFooter() {
        return this.footer;
    }

    @NotNull
    public FileConfigurationOptions setFooter(@Nullable List<String> value) {
        this.footer = (value == null) ? Collections.emptyList() : Collections.unmodifiableList(value);
        return this;
    }

    public boolean parseComments() {
        return this.parseComments;
    }

    @NotNull
    public MemoryConfigurationOptions parseComments(boolean value) {
        this.parseComments = value;
        return this;
    }
}
