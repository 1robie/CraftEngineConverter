package fr.robie.craftengineconverter.api.configuration.conditions;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

public class MatchBlockPropertyCondition extends AbstractCondition {
    private final Map<String, Object> properties;

    public MatchBlockPropertyCondition(@NotNull Map<String, Object> properties) {
        super("match_block_property");
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("properties", this.properties);
    }
}
