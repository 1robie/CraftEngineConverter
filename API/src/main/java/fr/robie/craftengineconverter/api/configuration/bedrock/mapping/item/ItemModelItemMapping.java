package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemModelItemMapping extends ItemMapping {
    private final String bedrockModelPath;

    public ItemModelItemMapping(@NotNull Material javaMaterial, @NotNull String bedrockIdentifier, @Nullable String bedrockModelPath) {
        super(javaMaterial, bedrockIdentifier);
        this.bedrockModelPath = bedrockModelPath;
    }


    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("type", "definition");
        if (this.bedrockModelPath != null) {
            jsonObject.addProperty("model", this.bedrockModelPath);
        }
        return jsonObject;
    }

}
