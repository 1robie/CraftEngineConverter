package fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities;

import java.util.LinkedHashMap;
import java.util.Map;

public class BetterModelRenderer implements EntityRenderer {
    private final String type = "better_model";
    private String model;
    private String position;
    private Float yaw;
    private Float pitch;
    private Boolean sightTrace;

    public BetterModelRenderer setModel(String model) {
        this.model = model;
        return this;
    }

    public BetterModelRenderer setPosition(String position) {
        this.position = position;
        return this;
    }

    public BetterModelRenderer setYaw(Float yaw) {
        this.yaw = yaw;
        return this;
    }

    public BetterModelRenderer setPitch(Float pitch) {
        this.pitch = pitch;
        return this;
    }

    public BetterModelRenderer setSightTrace(Boolean sightTrace) {
        this.sightTrace = sightTrace;
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", this.type);
        if (this.model != null) {
            map.put("model", this.model);
        }
        if (this.position != null) {
            map.put("position", this.position);
        }
        if (this.yaw != null) {
            map.put("yaw", this.yaw);
        }
        if (this.pitch != null) {
            map.put("pitch", this.pitch);
        }
        if (this.sightTrace != null) {
            map.put("sight-trace", this.sightTrace);
        }
        return map;
    }
}
