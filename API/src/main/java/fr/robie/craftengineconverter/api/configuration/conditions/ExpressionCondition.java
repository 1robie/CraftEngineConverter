package fr.robie.craftengineconverter.api.configuration.conditions;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ExpressionCondition extends AbstractCondition {
    private final String expression;

    public ExpressionCondition(@NotNull String expression) {
        super("expression");
        this.expression = Objects.requireNonNull(expression, "expression cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("expression", this.expression);
    }
}
