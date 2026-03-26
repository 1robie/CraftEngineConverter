package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class TeleportFunction extends AbstractEventFunction {
    private final Object x;
    private final Object y;
    private final Object z;
    private Object pitch;
    private Object yaw;
    private String world;

    public TeleportFunction(Object x, Object y, Object z) {
        super("teleport");
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setPitch(Object pitch) {
        this.pitch = pitch;
    }

    public void setYaw(Object yaw) {
        this.yaw = yaw;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("x", this.x);
        map.put("y", this.y);
        map.put("z", this.z);
        if (this.pitch != null) {
            map.put("pitch", this.pitch);
        }
        if (this.yaw != null) {
            map.put("yaw", this.yaw);
        }
        if (this.world != null) {
            map.put("world", this.world);
        }
        return map;
    }
}
