package fr.robie.craftengineconverter.api.configuration.item.behavior.block.behaviors;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackableBlockBehavior implements BlockBehavior {
    private String property;
    private final List<String> items;

    public StackableBlockBehavior() {
        this.items = new ArrayList<>();
    }

    public StackableBlockBehavior setProperty(String property) {
        this.property = property;
        return this;
    }

    public StackableBlockBehavior addItem(String item) {
        this.items.add(item);
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "stackable_block");
        if (this.property != null) {
            data.put("property", this.property);
        }
        if (!this.items.isEmpty()) {
            data.put("items", this.items);
        }
        return data;
    }
}
