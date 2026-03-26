package fr.robie.craftengineconverter.api.configuration.conditions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        List<Map<String, Object>> serializedTerms = new ArrayList<>();
        for (Condition term : this.terms) {
            YamlConfiguration temp = new YamlConfiguration();
            term.serialize(temp);
            serializedTerms.add(temp.getValues(true));
        }
        section.set("terms", serializedTerms);
    }
}
