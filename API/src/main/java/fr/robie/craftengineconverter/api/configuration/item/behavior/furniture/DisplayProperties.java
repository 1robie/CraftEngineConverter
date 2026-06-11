package fr.robie.craftengineconverter.api.configuration.item.behavior.furniture;

import fr.robie.craftengineconverter.api.utils.FloatsUtils;
import net.momirealms.craftengine.core.entity.display.Billboard;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DisplayProperties {
    private Billboard billboard = Billboard.FIXED;
    private final FloatsUtils translation = new FloatsUtils(3, new float[]{0, 0, 0});
    private final FloatsUtils scale = new FloatsUtils(3, new float[]{1, 1, 1});
    private final FloatsUtils glowColor = new FloatsUtils(3, new float[]{-1, -1, -1});
    private int brightnessBlockLight = -1;
    private int brightnessSkyLight = -1;
    private float viewRange = 1.0f;
    private float[] rotationEuler = null;
    private float[] rotationQuaternion = null;
    private Float rotationYaw = null;

    public void setBillboard(@NotNull Billboard billboard) { this.billboard = billboard; }
    public void setTranslation(float x, float y, float z) {
        this.translation.setValue(0, x);
        this.translation.setValue(1, y);
        this.translation.setValue(2, z);
    }
    public void setScale(float x, float y, float z) {
        this.scale.setValue(0, x);
        this.scale.setValue(1, y);
        this.scale.setValue(2, z);
    }
    public void setGlowColor(float r, float g, float b) {
        this.glowColor.setValue(0, r);
        this.glowColor.setValue(1, g);
        this.glowColor.setValue(2, b);
    }
    public void setBrightness(int blockLight, int skyLight) { this.brightnessBlockLight = blockLight; this.brightnessSkyLight = skyLight; }
    public void setViewRange(float viewRange) { this.viewRange = viewRange; }
    public void setRotationYaw(float yaw) { this.rotationYaw = yaw; this.rotationEuler = null; this.rotationQuaternion = null; }
    public void setRotationEuler(float x, float y, float z) { this.rotationEuler = new float[]{x, y, z}; this.rotationQuaternion = null; this.rotationYaw = null; }
    public void setRotationQuaternion(float x, float y, float z, float w) { this.rotationQuaternion = new float[]{x, y, z, w}; this.rotationEuler = null; this.rotationYaw = null; }

    public void serialize(Map<String, Object> data) {
        if (this.billboard != Billboard.FIXED)
            data.put("billboard", this.billboard.name().toLowerCase(Locale.ROOT));
        if (this.translation.isUpdated())
            data.put("translation", this.translation.toString());
        if (this.scale.isUpdated())
            data.put("scale", this.scale.toString());
        if (this.glowColor.isUpdated())
            data.put("glow-color", this.glowColor.toString());
        if (this.brightnessBlockLight != -1 || this.brightnessSkyLight != -1) {
            Map<String, Integer> brightness = new HashMap<>();
            if (this.brightnessBlockLight != -1) brightness.put("block-light", this.brightnessBlockLight);
            if (this.brightnessSkyLight != -1) brightness.put("sky-light", this.brightnessSkyLight);
            data.put("brightness", brightness);
        }
        if (this.viewRange != 1.0f)
            data.put("view-range", this.viewRange);
        if (this.rotationYaw != null)
            data.put("rotation", this.rotationYaw);
        else if (this.rotationEuler != null)
            data.put("rotation", this.rotationEuler);
        else if (this.rotationQuaternion != null)
            data.put("rotation", this.rotationQuaternion);
    }

    public void addTranslation(float x, float y, float z) {
        this.translation.addValue(0, x);
        this.translation.addValue(1, y);
        this.translation.addValue(2, z);
    }
}
