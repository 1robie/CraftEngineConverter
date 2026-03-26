package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class BreakBlockFunction extends AbstractEventFunction {
    private Object x;
    private Object y;
    private Object z;

    public BreakBlockFunction() {
        super("break_block");
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

    public Object getX() {
        return this.x;
    }

    public Object getY() {
        return this.y;
    }

    public Object getZ() {
        return this.z;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
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
