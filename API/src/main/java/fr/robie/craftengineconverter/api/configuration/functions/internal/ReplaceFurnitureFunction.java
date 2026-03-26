package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class ReplaceFurnitureFunction extends AbstractEventFunction {
    private final String furnitureId;
    private Object x;
    private Object y;
    private Object z;
    private Object pitch;
    private Object yaw;
    private String variant;
    private Boolean dropLoot;
    private Boolean playSound;

    public ReplaceFurnitureFunction(String furnitureId) {
        super("replace_furniture");
        this.furnitureId = furnitureId;
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

    public void setPitch(Object pitch) {
        this.pitch = pitch;
    }

    public void setYaw(Object yaw) {
        this.yaw = yaw;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public void setDropLoot(boolean dropLoot) {
        this.dropLoot = dropLoot;
    }

    public void setPlaySound(boolean playSound) {
        this.playSound = playSound;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("furniture-id", this.furnitureId);
        if (this.x != null) {
            map.put("x", this.x);
        }
        if (this.y != null) {
            map.put("y", this.y);
        }
        if (this.z != null) {
            map.put("z", this.z);
        }
        if (this.pitch != null) {
            map.put("pitch", this.pitch);
        }
        if (this.yaw != null) {
            map.put("yaw", this.yaw);
        }
        if (this.variant != null) {
            map.put("variant", this.variant);
        }
        if (this.dropLoot != null) {
            map.put("drop-loot", this.dropLoot);
        }
        if (this.playSound != null) {
            map.put("play-sound", this.playSound);
        }
        return map;
    }
}
