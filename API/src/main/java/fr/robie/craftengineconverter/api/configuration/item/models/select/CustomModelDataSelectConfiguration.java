package fr.robie.craftengineconverter.api.configuration.item.models.select;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class CustomModelDataSelectConfiguration extends SelectModelConfiguration<String> {
    private final int index;

    public CustomModelDataSelectConfiguration(int index) {
        super("minecraft:custom_model_data");
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("index", this.index);
    }
}
