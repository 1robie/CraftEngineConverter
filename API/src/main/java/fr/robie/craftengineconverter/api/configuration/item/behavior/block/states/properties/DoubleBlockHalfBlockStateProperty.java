package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockStateProperty;
import net.momirealms.craftengine.core.block.property.type.DoubleBlockHalf;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Locale;

public class DoubleBlockHalfBlockStateProperty implements BlockStateProperty<DoubleBlockHalf> {
    private final String name;
    private final DoubleBlockHalf value;

    public DoubleBlockHalfBlockStateProperty(@NotNull String name, @NotNull DoubleBlockHalf value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public @NonNull DoubleBlockHalf value() {
        return this.value;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection propertiesSection) {
        ConfigurationSection section = propertiesSection.createSection(this.name);
        section.set("type", "double_block_half");
        section.set("default", this.value.name().toLowerCase(Locale.ROOT));
    }
}
