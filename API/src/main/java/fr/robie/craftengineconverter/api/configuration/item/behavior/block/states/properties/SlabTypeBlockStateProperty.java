package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockStateProperty;
import net.momirealms.craftengine.core.block.property.type.SlabType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Locale;

public class SlabTypeBlockStateProperty implements BlockStateProperty<SlabType> {
    private final String name;
    private final SlabType value;

    public SlabTypeBlockStateProperty(@NotNull String name, @NotNull SlabType value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public @NonNull SlabType value() {
        return this.value;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection propertiesSection) {
        ConfigurationSection section = propertiesSection.createSection(this.name);
        section.set("type", "slab_type");
        section.set("default", this.value.name().toLowerCase(Locale.ROOT));
    }
}
