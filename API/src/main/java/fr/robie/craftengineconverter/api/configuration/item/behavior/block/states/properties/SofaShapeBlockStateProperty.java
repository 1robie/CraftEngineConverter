package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockStateProperty;
import fr.robie.yamllibrary.ConfigurationSection;
import net.momirealms.craftengine.core.block.property.type.SofaShape;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public record SofaShapeBlockStateProperty(String name, SofaShape value) implements BlockStateProperty<SofaShape> {
    public SofaShapeBlockStateProperty(@NotNull String name, @NotNull SofaShape value) {
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
        section.set("type", "sofa_shape");
        section.set("default", this.value.name().toLowerCase(Locale.ROOT));
    }
}
