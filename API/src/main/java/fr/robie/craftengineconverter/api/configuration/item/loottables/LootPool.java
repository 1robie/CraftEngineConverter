package fr.robie.craftengineconverter.api.configuration.item.loottables;

import fr.robie.craftengineconverter.api.configuration.conditions.Condition;
import fr.robie.craftengineconverter.api.configuration.item.loottables.entries.LootEntry;
import fr.robie.craftengineconverter.api.configuration.item.loottables.functions.LootFunction;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LootPool implements LootConfiguration {
    private Object rolls = 1;
    private final List<Condition> conditions = new ArrayList<>();
    private final List<LootEntry> entries = new ArrayList<>();
    private final List<LootFunction> functions = new ArrayList<>();

    public void setRolls(@NotNull Object rolls) {
        this.rolls = Objects.requireNonNull(rolls, "rolls cannot be null");
    }

    public void addCondition(@NotNull Condition condition) {
        this.conditions.add(Objects.requireNonNull(condition, "condition cannot be null"));
    }

    public void addEntry(@NotNull LootEntry entry) {
        this.entries.add(Objects.requireNonNull(entry, "entry cannot be null"));
    }

    public void addFunction(@NotNull LootFunction function) {
        this.functions.add(Objects.requireNonNull(function, "function cannot be null"));
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        section.set("rolls", this.rolls);

        if (!this.conditions.isEmpty()) {
            section.set("conditions", ConfigurationSerializationUtils.serializeCollection(this.conditions, ConfigurationSerializationUtils::toMap));
        }

        if (!this.entries.isEmpty()) {
            section.set("entries", ConfigurationSerializationUtils.serializeCollection(this.entries, ConfigurationSerializationUtils::toMap));
        }

        if (!this.functions.isEmpty()) {
            section.set("functions", ConfigurationSerializationUtils.serializeCollection(this.functions, ConfigurationSerializationUtils::toMap));
        }
    }
}
