package fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities;

import java.util.LinkedHashMap;
import java.util.Map;

public class ArmorStandRenderer implements EntityRenderer {
    private final String type = "armor_stand";
    private String item;
    private String position;
    private String scale;
    private Boolean small;
    private String glowColor;

    public ArmorStandRenderer setItem(String item) {
        this.item = item;
        return this;
    }

    public ArmorStandRenderer setPosition(String position) {
        this.position = position;
        return this;
    }

    public ArmorStandRenderer setScale(String scale) {
        this.scale = scale;
        return this;
    }

    public ArmorStandRenderer setSmall(Boolean small) {
        this.small = small;
        return this;
    }

    public ArmorStandRenderer setGlowColor(String glowColor) {
        this.glowColor = glowColor;
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", this.type);
        if (this.item != null) {
            map.put("item", this.item);
        }
        if (this.position != null) {
            map.put("position", this.position);
        }
        if (this.scale != null) {
            map.put("scale", this.scale);
        }
        if (this.small != null) {
            map.put("small", this.small);
        }
        if (this.glowColor != null) {
            map.put("glow-color", this.glowColor);
        }
        return map;
    }
}
