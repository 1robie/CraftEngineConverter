package fr.robie.craftengineconverter.api.configuration.item.behavior.block.behaviors;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnLiquidBlockBehavior implements BlockBehavior {
    private Boolean stackable;
    private final List<String> liquidType;

    public OnLiquidBlockBehavior() {
        this.liquidType = new ArrayList<>();
    }

    public OnLiquidBlockBehavior setStackable(boolean stackable) {
        this.stackable = stackable;
        return this;
    }

    public OnLiquidBlockBehavior addLiquidType(String type) {
        this.liquidType.add(type);
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "on_liquid_block");
        if (this.stackable != null) {
            data.put("stackable", this.stackable);
        }
        if (!this.liquidType.isEmpty()) {
            data.put("liquid-type", this.liquidType);
        }
        return data;
    }
}
