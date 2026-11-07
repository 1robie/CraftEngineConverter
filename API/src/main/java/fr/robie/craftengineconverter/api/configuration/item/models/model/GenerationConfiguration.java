package fr.robie.craftengineconverter.api.configuration.item.models.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class GenerationConfiguration {
    private final String parent;
    private final Map<String, String> textures = new LinkedHashMap<>();

    public GenerationConfiguration(@Nullable String parent) {
        this.parent = parent;
    }

    public GenerationConfiguration(@Nullable String parent, @NotNull String ... textures) {
        assert textures.length % 2 == 0 : "Textures should be provided in pairs of key and value.";
        this.parent = parent;
        for (int i = 0; i < textures.length; i += 2) {
            this.addTexture(textures[i], textures[i + 1]);
        }
    }

    public void addTexture(@NotNull String key, @NotNull String value) {
        this.textures.put(key, value);
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (this.parent != null) {
            map.put("parent", this.parent);
        }
        if (!this.textures.isEmpty()) {
            map.put("textures", this.textures);
        }
        return map;
    }
}
