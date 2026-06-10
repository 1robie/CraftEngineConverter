package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockStateProperty;
import net.momirealms.craftengine.core.block.property.type.StairsShape;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Locale;

public class StairsShapeBlockStateProperty implements BlockStateProperty<StairsShape> {
    private final String name;
    private final StairsShape value;

    public StairsShapeBlockStateProperty(@NotNull String name, @NotNull StairsShape value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public @NonNull StairsShape value() {
        return this.value;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection propertiesSection) {
        ConfigurationSection section = propertiesSection.createSection(this.name);
        section.set("type", "stairs_shape");
        section.set("default", this.value.name().toLowerCase(Locale.ROOT));
    }
}
