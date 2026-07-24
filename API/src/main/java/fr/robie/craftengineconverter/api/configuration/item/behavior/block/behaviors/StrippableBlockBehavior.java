package fr.robie.craftengineconverter.api.configuration.item.behavior.block.behaviors;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StrippableBlockBehavior implements BlockBehavior {
    private String stripped;
    private final List<String> excludedProperties;

    public StrippableBlockBehavior() {
        this.excludedProperties = new ArrayList<>();
    }

    public StrippableBlockBehavior setStripped(String stripped) {
        this.stripped = stripped;
        return this;
    }

    public StrippableBlockBehavior addExcludedProperty(String property) {
        this.excludedProperties.add(property);
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "strippable_block");
        if (this.stripped != null) {
            data.put("stripped", this.stripped);
        }
        if (!this.excludedProperties.isEmpty()) {
            data.put("excluded-properties", this.excludedProperties);
        }
        return data;
    }
}
