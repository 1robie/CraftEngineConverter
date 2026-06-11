package fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class UseCycleRangeDispatchConfiguration extends RangeDispatchModelConfiguration {
    private final float period;

    public UseCycleRangeDispatchConfiguration(float period) {
        super("minecraft:use_cycle");
        this.period = period;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("period", this.period);
    }
}
