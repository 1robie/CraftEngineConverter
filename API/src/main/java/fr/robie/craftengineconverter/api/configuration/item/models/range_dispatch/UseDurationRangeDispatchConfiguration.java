package fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class UseDurationRangeDispatchConfiguration extends RangeDispatchModelConfiguration {
    private final boolean remaining;

    public UseDurationRangeDispatchConfiguration(boolean remaining) {
        super("minecraft:use_duration");
        this.remaining = remaining;
    }

    public UseDurationRangeDispatchConfiguration() {
        this(false);
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        if (this.remaining) {
            section.set("remaining", true);
        }
    }
}
