package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockStateProperty;
import net.momirealms.craftengine.core.block.property.type.SofaShape;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Locale;

public class SofaShapeBlockStateProperty implements BlockStateProperty<SofaShape> {
    private final String name;
    private final SofaShape value;

    public SofaShapeBlockStateProperty(@NotNull String name, @NotNull SofaShape value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public @NonNull SofaShape value() {
        return this.value;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection propertiesSection) {
        ConfigurationSection section = propertiesSection.createSection(this.name);
        section.set("type", "sofa_shape");
        section.set("default", this.value.name().toLowerCase(Locale.ROOT));
    }
}
