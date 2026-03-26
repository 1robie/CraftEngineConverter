package fr.robie.craftengineconverter.api.configuration.recipe.postprocessor;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public abstract class PostProcessor {
    private final String type;

    protected PostProcessor(@NotNull String type) {
        this.type = type;
    }

    public @NotNull String getType() {
        return this.type;
    }

    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", this.type);
        return map;
    }
}
