package fr.robie.craftengineconverter.api.configuration.conditions;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractCondition implements Condition {
    private final String type;

    protected AbstractCondition(@NotNull String type) {
        this.type = type;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        section.set("type", this.type);
    }
}
