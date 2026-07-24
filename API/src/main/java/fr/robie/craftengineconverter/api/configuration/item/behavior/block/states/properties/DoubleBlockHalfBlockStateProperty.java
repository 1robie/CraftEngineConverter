package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockStateProperty;
import fr.robie.yamllibrary.ConfigurationSection;
import net.momirealms.craftengine.core.block.property.type.DoubleBlockHalf;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public record DoubleBlockHalfBlockStateProperty(String name,
                                                DoubleBlockHalf value) implements BlockStateProperty<DoubleBlockHalf> {
    public DoubleBlockHalfBlockStateProperty(@NotNull String name, @NotNull DoubleBlockHalf value) {
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
        section.set("type", "double_block_half");
        section.set("default", this.value.name().toLowerCase(Locale.ROOT));
    }
}
