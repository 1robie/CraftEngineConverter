package fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModelEngineRenderer implements EntityRenderer {
    private final String type = "model_engine";
    private String model;
    private String position;
    private Float yaw;
    private Float pitch;

    public ModelEngineRenderer setModel(String model) {
        this.model = model;
        return this;
    }

    public ModelEngineRenderer setPosition(String position) {
        this.position = position;
        return this;
    }

    public ModelEngineRenderer setYaw(Float yaw) {
        this.yaw = yaw;
        return this;
    }

    public ModelEngineRenderer setPitch(Float pitch) {
        this.pitch = pitch;
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
        return map;
    }
}
