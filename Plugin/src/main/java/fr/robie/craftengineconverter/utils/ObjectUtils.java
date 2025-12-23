package fr.robie.craftengineconverter.utils;

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

    @Contract("!null -> true; null -> false")
    public boolean isValidString(@Nullable String str){
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

    public boolean parseBoolean(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        String str = value.toString().toLowerCase();
        if ("true".equals(str) || "1".equals(str)) return true;
        if ("false".equals(str) || "0".equals(str)) return false;
        return defaultValue;
    }

    public boolean parseBoolean(Object value) {
        return parseBoolean(value, false);
    }

    public double parseDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            String str = value.toString().replace("f", "").replace("F", "");
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double parseDouble(Object value) {
        return parseDouble(value, 0.0);
    }

    public int parseInt(Object value, int defaultValue){
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int parseInt(Object value) {
        return parseInt(value, 0);
    }


}
