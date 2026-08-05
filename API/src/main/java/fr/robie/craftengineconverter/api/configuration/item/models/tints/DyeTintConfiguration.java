package fr.robie.craftengineconverter.api.configuration.item.models.tints;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

public record DyeTintConfiguration(Object defaultValue) implements TintConfiguration {
    public DyeTintConfiguration(@Nullable Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    @Nullable
    public Object defaultValue() {
        return this.defaultValue;
    }

    @Override
    public OptionalInt constantColor() {
        return TintColors.toRgb(this.defaultValue);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "minecraft:dye");
        if (this.defaultValue != null) {
            map.put("default", this.defaultValue);
        }
        return map;
    }
}
