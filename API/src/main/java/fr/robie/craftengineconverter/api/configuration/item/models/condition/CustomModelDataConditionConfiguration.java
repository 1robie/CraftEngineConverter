package fr.robie.craftengineconverter.api.configuration.item.models.condition;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class CustomModelDataConditionConfiguration extends ConditionModelConfiguration {
    private final int index;

    public CustomModelDataConditionConfiguration(int index) {
        super("minecraft:custom_model_data");
        this.index = index;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("index", this.index);
    }

    public int getIndex() {
        return this.index;
    }
}
