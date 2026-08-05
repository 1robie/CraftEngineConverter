package fr.robie.craftengineconverter.api.configuration.loader;

import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts between {@link ConfigurationSection} and plain {@code Map}/{@code List} trees.
 * <p>
 * The converter passes {@code ConfigurationSection} around, but the template engine and the JSON adapter
 * both need to walk and rebuild raw trees. Keeping both directions here means the
 * {@code MemoryConfiguration.createSection(path, map)} detail — which recursively turns nested maps into
 * child sections — lives in one place.
 */
public final class ConfigurationTrees {

    private ConfigurationTrees() {
        throw new UnsupportedOperationException("ConfigurationTrees is a utility class and cannot be instantiated.");
    }

    /**
     * Wraps a map tree as a detached {@link ConfigurationSection}.
     * <p>
     * The section is parented to a throwaway {@link MemoryConfiguration}; only the returned section is of
     * interest, so the path it is created under is arbitrary.
     */
    @NotNull
    public static ConfigurationSection toSection(@NotNull Map<String, Object> map) {
        return new MemoryConfiguration().createSection("root", map);
    }

    /** Copies a section into a plain, insertion-ordered map tree. */
    @NotNull
    public static Map<String, Object> toMap(@NotNull ConfigurationSection section) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            out.put(key, toValue(section.get(key)));
        }
        return out;
    }

    @Nullable
    private static Object toValue(@Nullable Object value) {
        if (value instanceof ConfigurationSection nested) {
            return toMap(nested);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                out.put(String.valueOf(entry.getKey()), toValue(entry.getValue()));
            }
            return out;
        }
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object element : list) out.add(toValue(element));
            return out;
        }
        return value;
    }
}
