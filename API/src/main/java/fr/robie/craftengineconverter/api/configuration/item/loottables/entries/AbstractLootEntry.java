package fr.robie.craftengineconverter.api.configuration.item.loottables.entries;

import fr.robie.craftengineconverter.api.configuration.conditions.Condition;
import fr.robie.craftengineconverter.api.configuration.item.loottables.functions.LootFunction;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractLootEntry implements LootEntry {
    private final String type;
    private final List<Condition> conditions = new ArrayList<>();
    private final List<LootFunction> functions = new ArrayList<>();
    private Integer weight;

    protected AbstractLootEntry(@NotNull String type) {
        this.type = type;
    }

    @Override
    public void addCondition(Condition condition) {
        this.conditions.add(Objects.requireNonNull(condition, "condition cannot be null"));
    }

    @Override
    public List<Condition> getConditions() {
        return this.conditions;
    }

    @Override
    public void addFunction(LootFunction function) {
        this.functions.add(Objects.requireNonNull(function, "function cannot be null"));
    }

    @Override
    public List<LootFunction> getFunctions() {
        return this.functions;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        section.set("type", this.type);
        if (this.weight != null) {
            section.set("weight", this.weight);
        }

        if (!this.conditions.isEmpty()) {
            section.set("conditions", ConfigurationSerializationUtils.serializeCollection(this.conditions, ConfigurationSerializationUtils::toMap));
        }

        if (!this.functions.isEmpty()) {
            section.set("functions", ConfigurationSerializationUtils.serializeCollection(this.functions, ConfigurationSerializationUtils::toMap));
        }
    }
}
