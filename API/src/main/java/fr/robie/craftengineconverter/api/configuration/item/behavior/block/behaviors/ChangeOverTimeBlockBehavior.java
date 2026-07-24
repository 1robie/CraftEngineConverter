package fr.robie.craftengineconverter.api.configuration.item.behavior.block.behaviors;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangeOverTimeBlockBehavior implements BlockBehavior {
    private Double changeSpeed;
    private String nextBlock;
    private final List<String> excludedProperties;

    public ChangeOverTimeBlockBehavior() {
        this.excludedProperties = new ArrayList<>();
    }

    public ChangeOverTimeBlockBehavior setChangeSpeed(double changeSpeed) {
        this.changeSpeed = changeSpeed;
        return this;
    }

    public ChangeOverTimeBlockBehavior setNextBlock(String nextBlock) {
        this.nextBlock = nextBlock;
        return this;
    }

    public ChangeOverTimeBlockBehavior addExcludedProperty(String property) {
        this.excludedProperties.add(property);
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "change_over_time_block");
        if (this.changeSpeed != null) {
            data.put("change-speed", this.changeSpeed);
        }
        if (this.nextBlock != null) {
            data.put("next-block", this.nextBlock);
        }
        if (!this.excludedProperties.isEmpty()) {
            data.put("excluded-properties", this.excludedProperties);
        }
        return data;
    }
}
