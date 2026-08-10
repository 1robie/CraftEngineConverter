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

    /**
     * Whether the thresholds are compared against the time <i>left</i> rather than the time elapsed.
     * <p>
     * It inverts the direction of every comparison, so anything reproducing this dispatch has to know.
     */
    public boolean isRemaining() {
        return this.remaining;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        if (this.remaining) {
            section.set("remaining", true);
        }
    }
}
