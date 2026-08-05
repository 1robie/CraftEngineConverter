package fr.robie.craftengineconverter.api.configuration.template;

import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A value bound to a template placeholder.
 * <p>
 * Most arguments are plain scalars, lists or maps. A <i>map</i> argument carrying a {@code type:} key is
 * instead a computed argument, dispatched by {@link #fromValue}.
 */
public interface TemplateArgument {

    /** Resolves this argument. May return {@code null}, which drops the entry that referenced it. */
    @Nullable
    Object resolve(@NotNull String node, @NotNull Map<String, TemplateArgument> arguments);

    /** An argument that resolves to nothing, dropping whatever entry referenced it. */
    TemplateArgument NULL = (node, arguments) -> null;

    /**
     * Wraps an already-resolved config value as an argument.
     * <p>
     * A map with a {@code type:} key selects a computed argument. CraftEngine registers twelve such types,
     * but its own shipped templates use exactly one ({@code condition}), so only the structural kinds and
     * {@code condition} are implemented; the rest warn once and fall back to being treated as plain data,
     * which is correct for anything that does not actually need computing.
     */
    @NotNull
    static TemplateArgument fromValue(@Nullable Object value) {
        if (value == null) return NULL;

        if (value instanceof Map<?, ?> map) {
            Object type = map.get("type");
            if (type instanceof String typeName) {
                return computed(stripNamespace(typeName), map);
            }
            return new Structural(map);
        }
        if (value instanceof List<?> list) {
            return new Structural(list);
        }
        return new Structural(value);
    }

    private static String stripNamespace(String type) {
        int colon = type.indexOf(':');
        return (colon < 0 ? type : type.substring(colon + 1)).toLowerCase(Locale.ROOT);
    }

    private static TemplateArgument computed(String type, Map<?, ?> map) {
        return switch (type) {
            case "condition" -> new Condition(map);
            case "plain", "object", "map", "list" -> new Structural(map);
            case "null" -> NULL;
            default -> {
                Logger.warn("Unsupported template argument type '" + type + "' - treating it as plain data, which may leave a placeholder unresolved");
                yield new Structural(map);
            }
        };
    }

    /** A plain scalar, list or map, resolved as-is. */
    final class Structural implements TemplateArgument {
        private final Object value;

        Structural(Object value) {
            this.value = value;
        }

        @Override
        public Object resolve(@NotNull String node, @NotNull Map<String, TemplateArgument> arguments) {
            return this.value;
        }

        @Override
        public String toString() {
            return "Structural(" + this.value + ")";
        }
    }

    /**
     * Picks between two values on a boolean.
     * <p>
     * {@code {type: condition, condition: ${flag:-false}, on_true: a, on_false: b}} — the one computed
     * argument type CraftEngine's own templates use. The condition has already had its placeholders
     * resolved by the time it arrives here, so this is a string-truthiness test.
     */
    final class Condition implements TemplateArgument {
        private final Object condition;
        private final Object onTrue;
        private final Object onFalse;

        Condition(Map<?, ?> map) {
            this.condition = map.get("condition");
            this.onTrue = map.containsKey("on_true") ? map.get("on_true") : map.get("on-true");
            this.onFalse = map.containsKey("on_false") ? map.get("on_false") : map.get("on-false");
        }

        @Override
        public Object resolve(@NotNull String node, @NotNull Map<String, TemplateArgument> arguments) {
            return isTrue(this.condition) ? this.onTrue : this.onFalse;
        }

        private static boolean isTrue(Object value) {
            if (value == null) return false;
            if (value instanceof Boolean bool) return bool;
            return Boolean.parseBoolean(String.valueOf(value).trim());
        }

        @Override
        public String toString() {
            return "Condition(" + this.condition + ")";
        }
    }
}
