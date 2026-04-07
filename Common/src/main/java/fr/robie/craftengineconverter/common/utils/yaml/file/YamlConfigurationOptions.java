package fr.robie.craftengineconverter.common.utils.yaml.file;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class YamlConfigurationOptions extends FileConfigurationOptions {
    private int indent = 2;
    private int width = 80;
    private int codePointLimit = Integer.MAX_VALUE;

    protected YamlConfigurationOptions(@NotNull YamlConfiguration configuration) {
        super(configuration);
    }

    @NotNull
    @Override
    public YamlConfiguration configuration() {
        return (YamlConfiguration) super.configuration();
    }

    @NotNull
    @Override
    public YamlConfigurationOptions copyDefaults(boolean value) {
        super.copyDefaults(value);
        return this;
    }

    @NotNull
    @Override
    public YamlConfigurationOptions pathSeparator(char value) {
        super.pathSeparator(value);
        return this;
    }

    @NotNull
    @Override
    public YamlConfigurationOptions setHeader(@Nullable List<String> value) {
        super.setHeader(value);
        return this;
    }

    @NotNull
    @Override
    public YamlConfigurationOptions setFooter(@Nullable List<String> value) {
        super.setFooter(value);
        return this;
    }

    @NotNull
    @Override
    public YamlConfigurationOptions parseComments(boolean value) {
        super.parseComments(value);
        return this;
    }

    public int indent() {
        return this.indent;
    }

    @NotNull
    public YamlConfigurationOptions indent(int value) {
        Preconditions.checkArgument(value >= 2, "Indent must be at least 2 characters");
        Preconditions.checkArgument(value <= 9, "Indent cannot be greater than 9 characters");

        this.indent = value;
        return this;
    }

    public int width() {
        return this.width;
    }

    @NotNull
    public YamlConfigurationOptions width(int value) {
        this.width = value;
        return this;
    }

    public int codePointLimit() {
        return this.codePointLimit;
    }

    @NotNull
    public YamlConfigurationOptions codePointLimit(int codePointLimit) {
        this.codePointLimit = codePointLimit;
        return this;
    }
}
