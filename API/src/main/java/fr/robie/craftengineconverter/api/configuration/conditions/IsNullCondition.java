package fr.robie.craftengineconverter.api.configuration.conditions;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class IsNullCondition extends AbstractCondition {
    private final String argument;

    public IsNullCondition(@NotNull String argument) {
        super("is_null");
        this.argument = Objects.requireNonNull(argument, "argument cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("argument", this.argument);
    }
}
