package fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class DamageRangeDispatchConfiguration extends RangeDispatchModelConfiguration {
    private final boolean normalize;

    public DamageRangeDispatchConfiguration(boolean normalize) {
        super("minecraft:damage");
        this.normalize = normalize;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("normalize", this.normalize);
    }
}
