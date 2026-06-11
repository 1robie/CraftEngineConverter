package fr.robie.craftengineconverter.api.configuration.item.behavior.block.behaviors;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockBehavior;
import net.momirealms.craftengine.core.util.Direction;

import java.util.Locale;
import java.util.Map;

public class VerticalCropBlockBehavior implements BlockBehavior {
    private final int maxHeight;
    private final float growSpeed;
    private final Direction growDirection;

    public VerticalCropBlockBehavior(int maxHeight, float growSpeed, Direction growDirection) {
        this.maxHeight = maxHeight;
        this.growSpeed = growSpeed;
        this.growDirection = growDirection;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("type", "vertical_crop_block");
        data.put("max-height", this.maxHeight);
        data.put("grow-speed", this.growSpeed);
        data.put("direction", this.growDirection.toString().toLowerCase(Locale.ROOT));
        return data;
    }
}
