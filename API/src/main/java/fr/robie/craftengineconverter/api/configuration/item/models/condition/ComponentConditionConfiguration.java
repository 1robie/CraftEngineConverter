package fr.robie.craftengineconverter.api.configuration.item.models.condition;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class ComponentConditionConfiguration extends ConditionModelConfiguration {
    private final String predicate;
    private final Object value;

    public ComponentConditionConfiguration(@NotNull String predicate, @NotNull Object value) {
        super("minecraft:component");
        this.predicate = predicate;
        this.value = value;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("predicate", this.predicate);
        section.set("value", this.value);
    }
}
