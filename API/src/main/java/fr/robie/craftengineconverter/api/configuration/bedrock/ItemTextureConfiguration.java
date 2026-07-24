package fr.robie.craftengineconverter.api.configuration.bedrock;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.texture.TextureData;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ItemTextureConfiguration {
    private String resourcePackName;
    private String textureName;
    private String fileName = "item_texture.json";

    private final List<TextureData> textures = new ArrayList<>();

    public ItemTextureConfiguration setResourcePackName(String resourcePackName) {
        this.resourcePackName = resourcePackName;
        return this;
    }

    public ItemTextureConfiguration setTextureName(String textureName) {
        this.textureName = textureName;
        return this;
    }

    public ItemTextureConfiguration setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public boolean isEmpty() {
        return this.textures.isEmpty();
    }

    public ItemTextureConfiguration addTextureData(@NotNull TextureData textureData) {
        this.textures.add(textureData);
        return this;
    }

    public void save(@NotNull Path directory) {
        JsonObject jsonObject = new JsonObject();

        if (this.resourcePackName != null) {
            jsonObject.addProperty("resource_pack_name", this.resourcePackName);
        }

        if (this.textureName != null) {
            jsonObject.addProperty("texture_name", this.textureName);
        }

        JsonObject texturesObject = new JsonObject();
        for (TextureData textureData : this.textures) {
            if (!textureData.getTextures().isEmpty()) {
                texturesObject.add(textureData.getBedrockIdentifier(), textureData.serialize());
            }
        }
        jsonObject.add("texture_data", texturesObject);

        FileCacheManager.saveJsonToFile(directory.resolve(this.fileName), jsonObject);
    }

}
