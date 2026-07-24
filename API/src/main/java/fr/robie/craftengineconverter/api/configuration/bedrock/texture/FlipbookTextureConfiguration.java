package fr.robie.craftengineconverter.api.configuration.bedrock.texture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FlipbookTextureConfiguration {
    private final List<FlipbookTextureData> flipbookTextures = new ArrayList<>();

    public FlipbookTextureConfiguration addFlipbookTexture(@NotNull FlipbookTextureData data) {
        this.flipbookTextures.add(data);
        return this;
    }

    public boolean isEmpty() {
        return this.flipbookTextures.isEmpty();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public void save(@NotNull Path directory) {
        JsonArray array = new JsonArray();
        for (FlipbookTextureData data : this.flipbookTextures) {
            array.add(data.serialize());
        }
        try (var writer = Files.newBufferedWriter(directory.resolve("flipbook_textures.json"))) {
            GSON.toJson(array, writer);
        } catch (Exception e) {
            Logger.error("Failed to write flipbook_textures.json", e);
        }
    }
}
