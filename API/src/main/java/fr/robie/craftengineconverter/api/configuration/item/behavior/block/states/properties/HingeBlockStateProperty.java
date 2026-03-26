package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockStateProperty;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class HingeBlockStateProperty implements BlockStateProperty<Door.Hinge> {
    private final String name;
    private final Door.Hinge value;

    public HingeBlockStateProperty(@NotNull String name, @NotNull Door.Hinge value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public Door.@NonNull Hinge value() {
        return this.value;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection propertiesSection) {
        ConfigurationSection section = propertiesSection.createSection(this.name);
        section.set("type", "hinge");
        section.set("default", this.value.name().toLowerCase());
    }
}
