package fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TimeRangeDispatchConfiguration extends RangeDispatchModelConfiguration {
    private final String source;
    private final boolean wobble;

    public TimeRangeDispatchConfiguration(@NotNull String source, boolean wobble) {
        super("minecraft:time");
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.wobble = wobble;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("source", this.source);
        section.set("wobble", this.wobble);
    }
}
