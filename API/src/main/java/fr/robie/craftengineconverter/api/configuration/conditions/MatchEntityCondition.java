package fr.robie.craftengineconverter.api.configuration.conditions;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MatchEntityCondition extends AbstractCondition {
    private final String id;
    private final boolean regex;

    public MatchEntityCondition(@NotNull String id, boolean regex) {
        super("match_entity");
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.regex = regex;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("id", this.id);
        section.set("regex", this.regex);
    }
}
