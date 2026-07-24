package fr.robie.craftengineconverter.api.configuration.item.behavior.block.behaviors;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SturdyBaseBlockBehavior implements BlockBehavior {
    private String direction;
    private final List<String> supportTypes;

    public SturdyBaseBlockBehavior() {
        this.supportTypes = new ArrayList<>();
    }

    public SturdyBaseBlockBehavior setDirection(String direction) {
        this.direction = direction;
        return this;
    }

    public SturdyBaseBlockBehavior addSupportType(String supportType) {
        this.supportTypes.add(supportType);
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "sturdy_base_block");
        if (this.direction != null) {
            data.put("direction", this.direction);
        }
        if (!this.supportTypes.isEmpty()) {
            data.put("support-types", this.supportTypes);
        }
        return data;
    }
}
