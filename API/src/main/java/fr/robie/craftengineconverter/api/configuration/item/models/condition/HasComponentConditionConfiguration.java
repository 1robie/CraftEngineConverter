package fr.robie.craftengineconverter.api.configuration.item.models.condition;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class HasComponentConditionConfiguration extends ConditionModelConfiguration {
    private final String component;
    private final boolean ignoreDefault;

    public HasComponentConditionConfiguration(@NotNull String component, boolean ignoreDefault) {
        super("minecraft:has_component");
        this.component = this.namespaced(component);
        this.ignoreDefault = ignoreDefault;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("component", this.component);
        section.set("ignore-default", this.ignoreDefault);
    }
}
