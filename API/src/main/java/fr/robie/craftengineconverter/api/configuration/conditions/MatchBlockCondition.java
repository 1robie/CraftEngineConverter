package fr.robie.craftengineconverter.api.configuration.conditions;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MatchBlockCondition extends AbstractCondition {
    private final String x;
    private final String y;
    private final String z;
    private final String id;
    private final boolean regex;

    public MatchBlockCondition(@NotNull String x, @NotNull String y, @NotNull String z, @NotNull String id, boolean regex) {
        super("match_block");
        this.x = Objects.requireNonNull(x, "x cannot be null");
        this.y = Objects.requireNonNull(y, "y cannot be null");
        this.z = Objects.requireNonNull(z, "z cannot be null");
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.regex = regex;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("x", this.x);
        section.set("y", this.y);
        section.set("z", this.z);
        section.set("id", this.id);
        section.set("regex", this.regex);
    }
}
