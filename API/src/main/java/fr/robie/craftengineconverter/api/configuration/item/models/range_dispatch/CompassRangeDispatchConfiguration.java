package fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class CompassRangeDispatchConfiguration extends RangeDispatchModelConfiguration {
    private final String target;
    private final boolean wobble;

    public CompassRangeDispatchConfiguration(@NotNull String target, boolean wobble) {
        super("minecraft:compass");
        this.target = target;
        this.wobble = wobble;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("target", this.target);
        section.set("wobble", this.wobble);
    }
}
