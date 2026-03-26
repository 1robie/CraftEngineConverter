package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class TransformBlockFunction extends AbstractEventFunction {
    private final String block;
    private Map<String, Object> properties;
    private Object x;
    private Object y;
    private Object z;

    public TransformBlockFunction(String block) {
        super("transform_block");
        this.block = block;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
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
        map.put("block", this.block);
        if (this.properties != null) {
            map.put("properties", this.properties);
        }
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
