package fr.robie.craftengineconverter.api.configuration.item.models.tints;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

public record ConstantTintConfiguration(Object value) implements TintConfiguration {
    public ConstantTintConfiguration(@NotNull Object value) {
        this.value = value;
    }

    @Override
    @NotNull
    public Object value() {
        return this.value;
    }

    @Override
    public OptionalInt constantColor() {
        return TintColors.toRgb(this.value);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "minecraft:constant");
        map.put("value", this.value);
        return map;
    }
}
