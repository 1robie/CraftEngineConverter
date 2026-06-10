package fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.element;

import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.ItemElement;
import fr.robie.craftengineconverter.api.utils.FloatsUtils;
import org.bukkit.DyeColor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ArmorStandElement extends ItemElement {
    private final FloatsUtils scale = new FloatsUtils(3, new float[]{1, 1, 1});
    private boolean isSmall = false;
    private DyeColor glowColor = DyeColor.WHITE;

    public ArmorStandElement(@NotNull String item) { super(item); }

    public void setScale(float x, float y, float z) {
        this.scale.setValue(0, x);
        this.scale.setValue(1, y);
        this.scale.setValue(2, z);
    }
    public void setSmall(boolean small) { this.isSmall = small; }
    public void setGlowColor(@NotNull DyeColor glowColor) { this.glowColor = glowColor; }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "armor_stand");
        super.serialize(data);
        if (this.scale.isUpdated())
            data.put("scale", this.scale.toString());
        if (this.isSmall)
            data.put("small", true);
        if (this.glowColor != DyeColor.WHITE)
            data.put("glow-color", this.glowColor.name().toLowerCase(Locale.ROOT));
        return data;
    }
}
