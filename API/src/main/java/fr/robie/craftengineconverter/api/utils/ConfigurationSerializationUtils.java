package fr.robie.craftengineconverter.api.utils;

import fr.robie.craftengineconverter.api.configuration.SectionSerializable;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for common configuration serialization patterns.
 */
public final class ConfigurationSerializationUtils {

    private ConfigurationSerializationUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Converts a {@link SectionSerializable} to a Map.
     *
     * @param serializable The object to serialize.
     * @return A Map representing the serialized object.
     */
    public static @NotNull Map<String, Object> toMap(@NotNull SectionSerializable serializable) {
        return toMap(serializable, true);
    }

    /**
     * Converts a {@link SectionSerializable} to a Map.
     *
     * @param serializable The object to serialize.
     * @param deep         Whether to perform a deep serialization.
     * @return A Map representing the serialized object.
     */
    public static @NotNull Map<String, Object> toMap(@NotNull SectionSerializable serializable, boolean deep) {
        YamlConfiguration temp = new YamlConfiguration();
        serializable.serialize(temp);
        return temp.getValues(deep);
    }

    /**
     * Serializes a collection of objects using a provided mapper function.
     *
     * @param collection The collection to serialize.
     * @param mapper     The function to use for serialization.
     * @param <T>        The type of objects in the collection.
     * @param <R>        The type of the serialized form.
     * @return A list of serialized objects.
     */
    public static <T, R> @NotNull List<R> serializeCollection(@NotNull Collection<T> collection, Function<T, R> mapper) {
        return collection.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }
}
