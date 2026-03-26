package fr.robie.craftengineconverter.api.configuration.conditions;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class MatchItemCondition extends AbstractCondition {
    private final Object id;
    private final boolean regex;

    public MatchItemCondition(@NotNull String id, boolean regex) {
        super("match_item");
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.regex = regex;
    }

    public MatchItemCondition(@NotNull List<String> ids, boolean regex) {
        super("match_item");
        this.id = Objects.requireNonNull(ids, "ids cannot be null");
        this.regex = regex;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("id", this.id);
        section.set("regex", this.regex);
    }
}
