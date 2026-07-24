package fr.robie.craftengineconverter.api.configuration.item.models;

import fr.robie.craftengineconverter.api.configuration.SectionSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ModelConfiguration extends SectionSerializable {

    @Override
    void serialize(@NotNull ConfigurationSection section);
    
    @Contract("!null -> !null; null -> null")
    default @Nullable String namespaced(String path) {
        return this.namespaced(path, "minecraft");
    }

    @Contract("null, _ -> null")
    default @Nullable String namespaced(String path, @NotNull String defaultNamespace) {
        path = this.cleanPath(path);
        if (path == null || path.isEmpty()) {
            return null;
        }
        return path.contains(":") ? path : defaultNamespace + ":" + path;
    }

    @Contract("null -> null")
    default String cleanPath(@Nullable String path) {
        if (path == null) {
            return null;
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - 5);
        }
        return path;
    }
}
