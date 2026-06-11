package fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class CustomModelDataRangeDispatchConfiguration extends RangeDispatchModelConfiguration {
    private final int index;

    public CustomModelDataRangeDispatchConfiguration(int index) {
        super("minecraft:custom_model_data");
        this.index = index;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("index", this.index);
    }
}
