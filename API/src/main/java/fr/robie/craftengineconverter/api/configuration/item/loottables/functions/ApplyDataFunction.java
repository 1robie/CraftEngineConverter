package fr.robie.craftengineconverter.api.configuration.item.loottables.functions;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

public class ApplyDataFunction extends AbstractLootFunction {
    private final Map<String, Object> data;

    public ApplyDataFunction(@NotNull Map<String, Object> data) {
        super("apply_data");
        this.data = Objects.requireNonNull(data, "data cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("data", this.data);
    }
}
