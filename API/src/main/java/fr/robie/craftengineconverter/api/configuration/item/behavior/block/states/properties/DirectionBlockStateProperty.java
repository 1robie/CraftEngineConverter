package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockStateProperty;
import fr.robie.yamllibrary.ConfigurationSection;
import net.momirealms.craftengine.core.util.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public record DirectionBlockStateProperty(String name, Direction value) implements BlockStateProperty<Direction> {
    public DirectionBlockStateProperty(@NotNull String name, @NotNull Direction value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection propertiesSection) {
        ConfigurationSection section = propertiesSection.createSection(this.name);
        section.set("type", "direction");
        section.set("default", this.value.name().toLowerCase(Locale.ROOT));
    }
}
