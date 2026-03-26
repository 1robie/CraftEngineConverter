package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class ParticleFunction extends AbstractEventFunction {
    private final String particle;
    private Object x, y, z;
    private Integer count;
    private Double offsetX, offsetY, offsetZ;
    private Double speed;
    private String item;
    private String blockState;
    private Double charge;
    private Integer shriek;
    private String color;
    private Double scale;
    private String from, to;
    private Object targetX, targetY, targetZ;
    private Integer arrivalTime;
    private Integer duration;

    public ParticleFunction(String particle) {
        super("particle");
        this.particle = particle;
    }

    public void setPos(Object x, Object y, Object z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setOffset(double x, double y, double z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public void setBlockState(String blockState) {
        this.blockState = blockState;
    }

    public void setCharge(double charge) {
        this.charge = charge;
    }

    public void setShriek(int shriek) {
        this.shriek = shriek;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public void setTransition(String from, String to, double scale) {
        this.from = from;
        this.to = to;
        this.scale = scale;
    }

    public void setVibration(Object tx, Object ty, Object tz, int arrivalTime) {
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        this.arrivalTime = arrivalTime;
    }

    public void setTrail(Object tx, Object ty, Object tz, int duration) {
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        this.duration = duration;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("particle", this.particle);
        if (this.x != null) {
            map.put("x", this.x);
        }
        if (this.y != null) {
            map.put("y", this.y);
        }
        if (this.z != null) {
            map.put("z", this.z);
        }
        if (this.count != null) {
            map.put("count", this.count);
        }
        if (this.offsetX != null) {
            map.put("offset-x", this.offsetX);
        }
        if (this.offsetY != null) {
            map.put("offset-y", this.offsetY);
        }
        if (this.offsetZ != null) {
            map.put("offset-z", this.offsetZ);
        }
        if (this.speed != null) {
            map.put("speed", this.speed);
        }
        if (this.item != null) {
            map.put("item", this.item);
        }
        if (this.blockState != null) {
            map.put("block-state", this.blockState);
        }
        if (this.charge != null) {
            map.put("charge", this.charge);
        }
        if (this.shriek != null) {
            map.put("shriek", this.shriek);
        }
        if (this.color != null) {
            map.put("color", this.color);
        }
        if (this.scale != null) {
            map.put("scale", this.scale);
        }
        if (this.from != null) {
            map.put("from", this.from);
        }
        if (this.to != null) {
            map.put("to", this.to);
        }
        if (this.targetX != null) {
            map.put("target-x", this.targetX);
        }
        if (this.targetY != null) {
            map.put("target-y", this.targetY);
        }
        if (this.targetZ != null) {
            map.put("target-z", this.targetZ);
        }
        if (this.arrivalTime != null) {
            map.put("arrival-time", this.arrivalTime);
        }
        if (this.duration != null) {
            map.put("duration", this.duration);
        }
        return map;
    }
}
