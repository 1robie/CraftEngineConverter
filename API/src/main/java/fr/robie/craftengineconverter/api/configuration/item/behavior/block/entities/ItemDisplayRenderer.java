package fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities;

import java.util.LinkedHashMap;
import java.util.Map;

public class ItemDisplayRenderer implements EntityRenderer {
    private String item;
    private String displayTransform;
    private String billboard;
    private String translation;
    private String rotation;
    private String position;
    private Float yaw;
    private Float pitch;
    private String scale;
    private String glowColor;
    private Map<String, Integer> brightness;
    private Float viewRange;


    public ItemDisplayRenderer setItem(String item) {
        this.item = item;
        return this;
    }

    public ItemDisplayRenderer setDisplayTransform(String displayTransform) {
        this.displayTransform = displayTransform;
        return this;
    }

    public ItemDisplayRenderer setBillboard(String billboard) {
        this.billboard = billboard;
        return this;
    }

    public ItemDisplayRenderer setTranslation(String translation) {
        this.translation = translation;
        return this;
    }

    public ItemDisplayRenderer setRotation(String rotation) {
        this.rotation = rotation;
        return this;
    }

    public ItemDisplayRenderer setPosition(String position) {
        this.position = position;
        return this;
    }

    public ItemDisplayRenderer setYaw(Float yaw) {
        this.yaw = yaw;
        return this;
    }

    public ItemDisplayRenderer setPitch(Float pitch) {
        this.pitch = pitch;
        return this;
    }

    public ItemDisplayRenderer setScale(String scale) {
        this.scale = scale;
        return this;
    }

    public ItemDisplayRenderer setGlowColor(String glowColor) {
        this.glowColor = glowColor;
        return this;
    }

    public ItemDisplayRenderer setBrightness(Map<String, Integer> brightness) {
        this.brightness = brightness;
        return this;
    }

    public ItemDisplayRenderer setViewRange(Float viewRange) {
        this.viewRange = viewRange;
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (this.item != null) {
            map.put("item", this.item);
        }
        if (this.displayTransform != null) {
            map.put("display-transform", this.displayTransform);
        }
        if (this.billboard != null) {
            map.put("billboard", this.billboard);
        }
        if (this.translation != null) {
            map.put("translation", this.translation);
        }
        if (this.rotation != null) {
            map.put("rotation", this.rotation);
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
        if (this.scale != null) {
            map.put("scale", this.scale);
        }
        if (this.glowColor != null) {
            map.put("glow-color", this.glowColor);
        }
        if (this.brightness != null) {
            map.put("brightness", this.brightness);
        }
        if (this.viewRange != null) {
            map.put("view-range", this.viewRange);
        }
        return map;
    }
}
