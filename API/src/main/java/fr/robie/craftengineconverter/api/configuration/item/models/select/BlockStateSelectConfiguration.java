package fr.robie.craftengineconverter.api.configuration.item.models.select;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class BlockStateSelectConfiguration extends SelectModelConfiguration<Object> {
    private final String blockStateProperty;

    public BlockStateSelectConfiguration(@NotNull String blockStateProperty) {
        super("minecraft:block_state");
        this.blockStateProperty = blockStateProperty;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("block-state-property", this.blockStateProperty);
    }
}
