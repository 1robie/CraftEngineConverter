package fr.robie.craftengineconverter.api.configuration.item.behavior.block.behaviors;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NearLiquidBlockBehavior implements BlockBehavior {
    private String liquidType;
    private Boolean stackable;
    private final List<String> positions;

    public NearLiquidBlockBehavior() {
        this.positions = new ArrayList<>();
    }

    public NearLiquidBlockBehavior setLiquidType(String liquidType) {
        this.liquidType = liquidType;
        return this;
    }

    public NearLiquidBlockBehavior setStackable(boolean stackable) {
        this.stackable = stackable;
        return this;
    }

    public NearLiquidBlockBehavior addPosition(String position) {
        this.positions.add(position);
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "near_liquid_block");
        if (this.liquidType != null) {
            data.put("liquid-type", this.liquidType);
        }
        if (this.stackable != null) {
            data.put("stackable", this.stackable);
        }
        if (!this.positions.isEmpty()) {
            data.put("positions", this.positions);
        }
        return data;
    }
}
