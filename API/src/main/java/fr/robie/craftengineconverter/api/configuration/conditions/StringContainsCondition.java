package fr.robie.craftengineconverter.api.configuration.conditions;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class StringContainsCondition extends AbstractCondition {
    private final String value1;
    private final String value2;

    public StringContainsCondition(@NotNull String value1, @NotNull String value2) {
        super("string_contains");
        this.value1 = Objects.requireNonNull(value1, "value1 cannot be null");
        this.value2 = Objects.requireNonNull(value2, "value2 cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("value1", this.value1);
        section.set("value2", this.value2);
    }
}
