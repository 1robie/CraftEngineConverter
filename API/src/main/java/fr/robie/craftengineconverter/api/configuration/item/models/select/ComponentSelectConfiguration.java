package fr.robie.craftengineconverter.api.configuration.item.models.select;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ComponentSelectConfiguration extends SelectModelConfiguration<Object> {
    private final String component;

    public ComponentSelectConfiguration(@NotNull String component) {
        super("minecraft:component");
        this.component = this.namespaced(Objects.requireNonNull(component, "component cannot be null"));
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("component", this.component);
    }
}
