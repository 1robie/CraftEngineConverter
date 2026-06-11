package fr.robie.craftengineconverter.api.configuration.item.loottables.functions;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DropExpFunction extends AbstractLootFunction {
    private final Object count;

    public DropExpFunction(@NotNull Object count) {
        super("drop_exp");
        this.count = Objects.requireNonNull(count, "count cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("count", this.count);
    }
}
