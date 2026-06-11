package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockStateProperty;
import fr.robie.yamllibrary.ConfigurationSection;
import net.momirealms.craftengine.core.block.property.type.AnchorType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Locale;

public class AnchorTypeBlockStateProperty implements BlockStateProperty<AnchorType> {
    private final String name;
    private final AnchorType value;

    public AnchorTypeBlockStateProperty(@NotNull String name, @NotNull AnchorType value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public @NonNull AnchorType value() {
        return this.value;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection propertiesSection) {
        ConfigurationSection section = propertiesSection.createSection(this.name);
        section.set("type", "anchor_type");
        section.set("default", this.value.name().toLowerCase(Locale.ROOT));
    }
}
