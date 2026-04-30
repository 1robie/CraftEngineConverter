package fr.robie.craftengineconverter.api.configuration.bedrock.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TextureData {
    private final String bedrockIdentifier;

    private final List<String> textures = new ArrayList<>();

    public TextureData(@NotNull String bedrockIdentifier) {
        this.bedrockIdentifier = bedrockIdentifier;
    }

    public TextureData addTexture(@NotNull String texture) {
        this.textures.add(texture);
        return this;
    }

    public String getBedrockIdentifier() {
        return this.bedrockIdentifier;
    }

    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();

        if (!this.textures.isEmpty()) {
            if (this.textures.size() == 1) {
                jsonObject.addProperty("textures", this.textures.getFirst());
            } else {
                JsonArray texturesArray = new JsonArray();
                for (String texture : this.textures) {
                    texturesArray.add(texture);
                }
                jsonObject.add("textures", texturesArray);
            }
        }

        return jsonObject;
    }
}
