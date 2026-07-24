package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class GenericBedrockComponent implements BedrockComponent {
    private final String key;
    private final JsonElement value;

    public GenericBedrockComponent(String key, Object yamlValue) {
        this.key = key;
        this.value = toJsonElement(yamlValue);
    }

    @Override
    public void applyTo(@NotNull JsonObject componentObject) {
        componentObject.add(this.key, this.value);
    }

    static JsonElement toJsonElement(Object yamlValue) {
        if (yamlValue == null) {
            return new JsonObject();
        }
        if (yamlValue instanceof Map<?, ?> map) {
            JsonObject obj = new JsonObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                obj.add(String.valueOf(entry.getKey()), toJsonElement(entry.getValue()));
            }
            return obj;
        }
        if (yamlValue instanceof List<?> list) {
            JsonArray arr = new JsonArray();
            for (Object item : list) {
                arr.add(toJsonElement(item));
            }
            return arr;
        }
        if (yamlValue instanceof Number n) {
            if (n.doubleValue() == n.longValue()) {
                return new JsonPrimitive(n.longValue());
            }
            return new JsonPrimitive(n.doubleValue());
        }
        if (yamlValue instanceof Boolean b) {
            return new JsonPrimitive(b);
        }
        return new JsonPrimitive(String.valueOf(yamlValue));
    }

    public String getKey() {
        return this.key;
    }
}
