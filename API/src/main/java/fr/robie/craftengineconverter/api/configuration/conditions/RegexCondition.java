package fr.robie.craftengineconverter.api.configuration.conditions;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RegexCondition extends AbstractCondition {
    private final String value;
    private final String regex;

    public RegexCondition(@NotNull String value, @NotNull String regex) {
        super("regex");
        this.value = Objects.requireNonNull(value, "value cannot be null");
        this.regex = Objects.requireNonNull(regex, "regex cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("value", this.value);
        section.set("regex", this.regex);
    }
}
