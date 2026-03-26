package fr.robie.craftengineconverter.api.configuration.item.loottables;

import fr.robie.craftengineconverter.api.configuration.SectionSerializable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LootConfiguration extends SectionSerializable {

    @Contract("!null -> !null; null -> null")
    default @Nullable String namespaced(String path) {
        return this.namespaced(path, "minecraft");
    }

    @Contract("null, _ -> null")
    default @Nullable String namespaced(String path, @NotNull String defaultNamespace) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        return path.contains(":") ? path : defaultNamespace + ":" + path;
    }
}
