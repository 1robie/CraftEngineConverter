package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockStateProperty;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class StringBlockStateProperty implements BlockStateProperty<String> {
    private final String name;
    private final String value;
    private final List<String> allowedValues;

    public StringBlockStateProperty(@NotNull String name, @NotNull String value, @NotNull List<String> allowedValues) {
        this.name = name;
        this.value = value;
        this.allowedValues = allowedValues;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public @NonNull String value() {
        return this.value;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection propertiesSection) {
        ConfigurationSection section = propertiesSection.createSection(this.name);
        section.set("type", "string");
        section.set("default", this.value);
        section.set("values", this.allowedValues);
    }
}
