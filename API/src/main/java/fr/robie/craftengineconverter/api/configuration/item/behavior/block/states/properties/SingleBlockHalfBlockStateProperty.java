package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockStateProperty;
import fr.robie.yamllibrary.ConfigurationSection;
import net.momirealms.craftengine.core.block.property.type.SingleBlockHalf;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Locale;

public class SingleBlockHalfBlockStateProperty implements BlockStateProperty<SingleBlockHalf> {
    private final String name;
    private final SingleBlockHalf value;

    public SingleBlockHalfBlockStateProperty(@NotNull String name, @NotNull SingleBlockHalf value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public @NonNull SingleBlockHalf value() {
        return this.value;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection propertiesSection) {
        ConfigurationSection section = propertiesSection.createSection(this.name);
        section.set("type", "single_block_half");
        section.set("default", this.value.name().toLowerCase(Locale.ROOT));
    }
}
