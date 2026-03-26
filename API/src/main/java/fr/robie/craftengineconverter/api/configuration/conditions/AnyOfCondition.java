package fr.robie.craftengineconverter.api.configuration.conditions;

import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class AnyOfCondition extends AbstractCondition {
    private final List<Condition> terms;

    public AnyOfCondition(@NotNull List<Condition> terms) {
        super("any_of");
        this.terms = Objects.requireNonNull(terms, "terms cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("terms", ConfigurationSerializationUtils.serializeCollection(this.terms, ConfigurationSerializationUtils::toMap));
    }
}