package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockStateProperty;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class IntBlockStateProperty implements BlockStateProperty<Integer> {
    private final String name;
    private final int value;

    private final int min;
    private final int max;

    public IntBlockStateProperty(@NotNull String name, int value, int min, int max) {
        this.name = name;
        this.value = value;
        this.min = min;
        this.max = max;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public @NonNull Integer value() {
        return this.value;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection propertiesSection) {
        ConfigurationSection section = propertiesSection.createSection(this.name);
        section.set("type", "int");
        section.set("default", this.value);
        section.set("range", String.format("%d~%d", this.min, this.max));
    }
}
