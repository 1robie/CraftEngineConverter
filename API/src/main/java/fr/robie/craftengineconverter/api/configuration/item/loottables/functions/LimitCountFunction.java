package fr.robie.craftengineconverter.api.configuration.item.loottables.functions;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class LimitCountFunction extends AbstractLootFunction {
    private final Integer min;
    private final Integer max;

    public LimitCountFunction(Integer min, Integer max) {
        super("limit_count");
        this.min = min;
        this.max = max;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        if (this.min != null) {
            section.set("min", this.min);
        }
        if (this.max != null) {
            section.set("max", this.max);
        }
    }
}
