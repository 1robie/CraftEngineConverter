package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class LegacyItemMapping extends ItemMapping {
    private final float customModelData;

    public LegacyItemMapping(@NotNull Material javaMaterial, @NotNull String bedrockIdentifier, float customModelData) {
        super(javaMaterial, bedrockIdentifier);
        this.customModelData = customModelData;
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("type", "legacy");
        jsonObject.addProperty("custom_model_data", this.customModelData);
        return jsonObject;
    }
}
