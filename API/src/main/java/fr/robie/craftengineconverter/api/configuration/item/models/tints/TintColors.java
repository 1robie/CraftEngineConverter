package fr.robie.craftengineconverter.api.configuration.item.models.tints;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.OptionalInt;

public final class TintColors {

    private TintColors() {
        throw new UnsupportedOperationException("TintColors is a utility class and cannot be instantiated.");
    }

    public static OptionalInt toRgb(@Nullable Object value) {
        if (value == null) return OptionalInt.empty();

        if (value instanceof Number number) {
            return OptionalInt.of(number.intValue() & 0xFFFFFF);
        }

        if (value instanceof Collection<?> collection) {
            return fromComponents(collection.stream().map(String::valueOf).toArray(String[]::new));
        }

        if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) return OptionalInt.empty();
            if (trimmed.contains(",")) return fromComponents(trimmed.split(","));

            try {
                if (trimmed.startsWith("#")) {
                    return OptionalInt.of(Integer.parseInt(trimmed.substring(1), 16) & 0xFFFFFF);
                }
                return OptionalInt.of(Integer.parseInt(trimmed) & 0xFFFFFF);
            } catch (NumberFormatException e) {
                return OptionalInt.empty();
            }
        }

        return OptionalInt.empty();
    }

    private static OptionalInt fromComponents(String[] parts) {
        if (parts.length < 3) return OptionalInt.empty();
        try {
            int r = clamp(Integer.parseInt(parts[0].trim()));
            int g = clamp(Integer.parseInt(parts[1].trim()));
            int b = clamp(Integer.parseInt(parts[2].trim()));
            return OptionalInt.of((r << 16) | (g << 8) | b);
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    private static int clamp(int component) {
        return Math.clamp(component, 0, 255);
    }
}
