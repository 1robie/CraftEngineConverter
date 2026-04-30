package fr.robie.craftengineconverter.api.configuration.item.models.select;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class CustomModelDataSelectConfiguration extends SelectModelConfiguration<String> {
    private final int index;

    public CustomModelDataSelectConfiguration(int index) {
        super("minecraft:custom_model_data");
        this.index = index;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("index", this.index);
    }
}
