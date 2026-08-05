package fr.robie.craftengineconverter.api.configuration.bedrock.texture;

import com.google.gson.JsonArray;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class FlipbookTextureConfiguration {
    private final Map<String, FlipbookTextureData> flipbookTextures = new LinkedHashMap<>();

    public FlipbookTextureConfiguration addFlipbookTexture(@NotNull FlipbookTextureData data) {
        this.flipbookTextures.put(data.getAtlasTile(), data);
        return this;
    }

    public boolean isEmpty() {
        return this.flipbookTextures.isEmpty();
    }

    public void save(@NotNull Path directory) {
        JsonArray array = new JsonArray();
        for (FlipbookTextureData data : this.flipbookTextures.values()) {
            array.add(data.serialize());
        }

        FileCacheManager.saveJsonToFile(directory.resolve("flipbook_textures.json"), array);
    }
}
