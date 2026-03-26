package fr.robie.craftengineconverter.api.configuration.functions.internal;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CycleBlockPropertyFunction extends AbstractEventFunction {
    private final String property;
    private Object inverse;
    private Object x;
    private Object y;
    private Object z;
    private Map<String, Object> rules;

    public CycleBlockPropertyFunction(@NotNull String property) {
        super("cycle_block_property");
        this.property = property;
    }

    public void setInverse(Object inverse) {
        this.inverse = inverse;
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

    public void setRules(Map<String, Object> rules) {
        this.rules = rules;
    }

    public String getProperty() {
        return this.property;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("property", this.property);
        if (this.inverse != null) {
            map.put("inverse", this.inverse);
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
        if (this.rules != null) {
            map.put("rules", this.rules);
        }
        return map;
    }
}
