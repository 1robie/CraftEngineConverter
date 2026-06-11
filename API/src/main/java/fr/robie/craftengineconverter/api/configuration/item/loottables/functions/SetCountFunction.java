package fr.robie.craftengineconverter.api.configuration.item.loottables.functions;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SetCountFunction extends AbstractLootFunction {
    private final Object count;
    private final boolean add;

    public SetCountFunction(@NotNull Object count, boolean add) {
        super("set_count");
        this.count = Objects.requireNonNull(count, "count cannot be null");
        this.add = add;
    }

    public SetCountFunction(@NotNull Object count) {
        this(count, false);
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("count", this.count);
        if (this.add) {
            section.set("add", true);
        }
    }
}
