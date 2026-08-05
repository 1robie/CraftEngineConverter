package fr.robie.craftengineconverter.api.configuration.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Views a {@link JsonObject} as a {@link ConfigurationSection}.
 * <p>
 * Java item model definitions ({@code assets/<namespace>/items/*.json}) describe exactly the same
 * structure that CraftEngine expresses in YAML under an item's {@code model:} key, and the
 * {@code ModelConfiguration} loaders already read both spellings of every key
 * ({@code on-true} / {@code on_true}) and both the bare and {@code minecraft:}-prefixed type names.
 * They are therefore already capable of parsing Java's JSON — they just cannot accept it, because they
 * take a {@code ConfigurationSection}.
 * <p>
 * Adapting the input here means the whole loader tree works on Java resource packs unchanged, rather
 * than every loader needing a parallel JSON overload.
 */
public final class JsonConfigurationAdapter {

    private JsonConfigurationAdapter() {
        throw new UnsupportedOperationException("JsonConfigurationAdapter is a utility class and cannot be instantiated.");
    }

    /**
     * Wraps {@code json} in a detached {@link ConfigurationSection}.
     * <p>
     * See {@link ConfigurationTrees#toSection(Map)} for how the map becomes a section tree.
     */
    @NotNull
    public static ConfigurationSection toSection(@NotNull JsonObject json) {
        return ConfigurationTrees.toSection(toMap(json));
    }

    /** The JSON tree as a plain map, for callers that want to work on it directly. */
    @NotNull
    public static Map<String, Object> toMap(@NotNull JsonObject json) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            map.put(entry.getKey(), toValue(entry.getValue()));
        }
        return map;
    }

    @Nullable
    private static Object toValue(@NotNull JsonElement element) {
        if (element.isJsonObject()) {
            // Nested maps are turned into child sections by createSection, so plain maps suffice here.
            return toMap(element.getAsJsonObject());
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            List<Object> list = new ArrayList<>(array.size());
            for (JsonElement child : array) {
                list.add(toValue(child));
            }
            return list;
        }
        if (element.isJsonPrimitive()) {
            return toPrimitive(element.getAsJsonPrimitive());
        }
        return null;
    }

    @NotNull
    private static Object toPrimitive(@NotNull JsonPrimitive primitive) {
        if (primitive.isBoolean()) return primitive.getAsBoolean();
        if (primitive.isString()) return primitive.getAsString();

        String raw = primitive.getAsString();
        if (raw.indexOf('.') < 0 && raw.indexOf('e') < 0 && raw.indexOf('E') < 0) {
            try {
                return Integer.valueOf(raw);
            } catch (NumberFormatException ignored) {
                try {
                    return Long.valueOf(raw);
                } catch (NumberFormatException ignored2) {
                }
            }
        }
        try {
            return Double.valueOf(raw);
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }
}
