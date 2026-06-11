package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.utils.HorizontalDirection;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Locale;

public class HorizontalDirectionBlockStateProperty implements BlockStateProperty<HorizontalDirection> {
    private final String name;
    private final HorizontalDirection value;

    public HorizontalDirectionBlockStateProperty(@NotNull String name, @NotNull HorizontalDirection value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public @NonNull HorizontalDirection value() {
        return this.value;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection propertiesSection) {
        ConfigurationSection section = propertiesSection.createSection(this.name);
        section.set("type", "horizontal_direction");
        section.set("default", this.value.name().toLowerCase(Locale.ROOT));
    }
}
