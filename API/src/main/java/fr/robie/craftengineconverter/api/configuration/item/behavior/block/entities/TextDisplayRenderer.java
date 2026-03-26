package fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities;

import java.util.LinkedHashMap;
import java.util.Map;

public class TextDisplayRenderer implements EntityRenderer {
    private final String type = "text_display";
    private String text;
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
    private Integer lineWidth;
    private String backgroundColor;
    private Integer textOpacity;
    private Boolean hasShadow;
    private Boolean isSeeThrough;
    private Boolean useDefaultBackgroundColor;
    private String alignment;

    public TextDisplayRenderer setText(String text) {
        this.text = text;
        return this;
    }

    public TextDisplayRenderer setBillboard(String billboard) {
        this.billboard = billboard;
        return this;
    }

    public TextDisplayRenderer setTranslation(String translation) {
        this.translation = translation;
        return this;
    }

    public TextDisplayRenderer setRotation(String rotation) {
        this.rotation = rotation;
        return this;
    }

    public TextDisplayRenderer setPosition(String position) {
        this.position = position;
        return this;
    }

    public TextDisplayRenderer setYaw(Float yaw) {
        this.yaw = yaw;
        return this;
    }

    public TextDisplayRenderer setPitch(Float pitch) {
        this.pitch = pitch;
        return this;
    }

    public TextDisplayRenderer setScale(String scale) {
        this.scale = scale;
        return this;
    }

    public TextDisplayRenderer setGlowColor(String glowColor) {
        this.glowColor = glowColor;
        return this;
    }

    public TextDisplayRenderer setBrightness(Map<String, Integer> brightness) {
        this.brightness = brightness;
        return this;
    }

    public TextDisplayRenderer setViewRange(Float viewRange) {
        this.viewRange = viewRange;
        return this;
    }

    public TextDisplayRenderer setLineWidth(Integer lineWidth) {
        this.lineWidth = lineWidth;
        return this;
    }

    public TextDisplayRenderer setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public TextDisplayRenderer setTextOpacity(Integer textOpacity) {
        this.textOpacity = textOpacity;
        return this;
    }

    public TextDisplayRenderer setHasShadow(Boolean hasShadow) {
        this.hasShadow = hasShadow;
        return this;
    }

    public TextDisplayRenderer setIsSeeThrough(Boolean isSeeThrough) {
        this.isSeeThrough = isSeeThrough;
        return this;
    }

    public TextDisplayRenderer setUseDefaultBackgroundColor(Boolean useDefaultBackgroundColor) {
        this.useDefaultBackgroundColor = useDefaultBackgroundColor;
        return this;
    }

    public TextDisplayRenderer setAlignment(String alignment) {
        this.alignment = alignment;
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", this.type);
        if (this.text != null) {
            map.put("text", this.text);
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
        if (this.lineWidth != null) {
            map.put("line-width", this.lineWidth);
        }
        if (this.backgroundColor != null) {
            map.put("background-color", this.backgroundColor);
        }
        if (this.textOpacity != null) {
            map.put("text-opacity", this.textOpacity);
        }
        if (this.hasShadow != null) {
            map.put("has-shadow", this.hasShadow);
        }
        if (this.isSeeThrough != null) {
            map.put("is-see-through", this.isSeeThrough);
        }
        if (this.useDefaultBackgroundColor != null) {
            map.put("use-default-background-color", this.useDefaultBackgroundColor);
        }
        if (this.alignment != null) {
            map.put("alignment", this.alignment);
        }
        return map;
    }
}
