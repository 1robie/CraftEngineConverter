package fr.robie.craftengineconverter.api.configuration.functions.internal;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PlaceBlockFunction extends AbstractEventFunction {
    private final String blockState;
    private Object x;
    private Object y;
    private Object z;

    public PlaceBlockFunction(@NotNull String blockState) {
        super("place_block");
        this.blockState = blockState;
    }

    public void setX(Object x) {
        this.x = x;
    }

    public void setY(Object y) {
        this.y = y;
    }

    public void setZ(Object z) {
        this.z = z;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("block-state", this.blockState);
        if (this.x != null) {
            map.put("x", this.x);
        }
        if (this.y != null) {
            map.put("y", this.y);
        }
        if (this.z != null) {
            map.put("z", this.z);
        }
        return map;
    }
}
