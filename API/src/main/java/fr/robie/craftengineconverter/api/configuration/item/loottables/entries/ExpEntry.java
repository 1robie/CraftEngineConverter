package fr.robie.craftengineconverter.api.configuration.item.loottables.entries;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ExpEntry extends AbstractLootEntry {
    private final Object count;

    public ExpEntry(@NotNull Object count) {
        super("exp");
        this.count = Objects.requireNonNull(count, "count cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("count", this.count);
    }
}
