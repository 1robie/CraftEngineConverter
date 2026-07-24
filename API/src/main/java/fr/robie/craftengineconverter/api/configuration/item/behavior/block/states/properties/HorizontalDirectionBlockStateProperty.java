package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.utils.HorizontalDirection;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public record HorizontalDirectionBlockStateProperty(String name,
                                                    HorizontalDirection value) implements BlockStateProperty<HorizontalDirection> {
    public HorizontalDirectionBlockStateProperty(@NotNull String name, @NotNull HorizontalDirection value) {
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
        section.set("type", "horizontal_direction");
        section.set("default", this.value.name().toLowerCase(Locale.ROOT));
    }
}
