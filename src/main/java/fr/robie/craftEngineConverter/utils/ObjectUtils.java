package fr.robie.craftEngineConverter.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ObjectUtils {
    protected String cleanPath(String path) {
        if (path == null || path.isEmpty()) return null;
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - 5);
        }
        return path;
    }

    @Contract("null -> false")
    public boolean isValidString(String str){
        return str != null && !str.isBlank();
    }

    @Contract("null -> false; !null -> true")
    public boolean isNotNull(Object obj){
        return obj != null;
    }

    @Contract("null -> true")
    public boolean isNull(Object obj){
        return obj == null;
    }

    protected @Nullable String namespaced(String path) {
        path = cleanPath(path);
        if (path == null || path.isEmpty()) return null;
        return path.contains(":") ? path : "minecraft:" + path;
    }

    public String removeEndWith(@NotNull String str, List<String> ends, String defaultValue) {
        for  (String end : ends) {
            if (str.endsWith(end)) {
                return str.substring(0, str.length() - end.length());
            }
        }
        return defaultValue;
    }
}
