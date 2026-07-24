package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.component.BedrockComponent;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.option.BedrockOptions;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.BedrockPredicate;
import fr.robie.craftengineconverter.api.configuration.bedrock.texture.TextureData;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class ItemMapping {
    private final Material javaMaterial;

    private final String bedrockIdentifier;

    private String displayName;
    private BedrockOptions bedrockOptions;
    private final List<BedrockPredicate> bedrockPredicates = new ArrayList<>();

    private final List<BedrockComponent> bedrockComponents = new ArrayList<>(0);

    private final List<TextureData> texturesData = new ArrayList<>();

    public ItemMapping(@NotNull Material javaMaterial, @NotNull String bedrockIdentifier) {
        this.javaMaterial = javaMaterial;
        this.bedrockIdentifier = bedrockIdentifier;
    }

    public ItemMapping setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public ItemMapping setBedrockOptions(BedrockOptions bedrockOptions) {
        this.bedrockOptions = bedrockOptions;
        return this;
    }

    public BedrockOptions getBedrockOptions() {
        return this.bedrockOptions;
    }

    public ItemMapping addBedrockComponent(@NotNull BedrockComponent component) {
        this.bedrockComponents.add(component);
        return this;
    }

    public ItemMapping addBedrockPredicate(BedrockPredicate bedrockPredicate) {
        this.bedrockPredicates.add(bedrockPredicate);
        return this;
    }

    public ItemMapping addTextureData(TextureData textureData) {
        this.texturesData.add(textureData);
        return this;
    }

    @NotNull
    public List<TextureData> getTexturesData() {
        return this.texturesData;
    }

    public Material getJavaMaterial() {
        return this.javaMaterial;
    }

    public String getBedrockIdentifier() {
        return this.bedrockIdentifier;
    }

    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("bedrock_identifier", this.bedrockIdentifier);

        if (this.displayName != null) {
            jsonObject.addProperty("display_name", this.displayName);
        }

        if (this.bedrockOptions != null) {
            JsonObject serialize = this.bedrockOptions.serialize();
            if (!serialize.isEmpty()) {
                jsonObject.add("bedrock_options", serialize);
            }
        }

        if (!this.bedrockComponents.isEmpty()) {
            JsonObject componentsObject = new JsonObject();
            for (BedrockComponent component : this.bedrockComponents) {
                component.applyTo(componentsObject);
            }
            jsonObject.add("components", componentsObject);
        }

        if (!this.bedrockPredicates.isEmpty()) {
            if (this.bedrockPredicates.size() == 1) {
                jsonObject.add("predicate", this.bedrockPredicates.getFirst().serialize());
            } else {
                JsonArray predicateArray = new JsonArray();
                for (BedrockPredicate predicate : this.bedrockPredicates) {
                    predicateArray.add(predicate.serialize());
                }
                jsonObject.add("predicate", predicateArray);
            }
        }

        return jsonObject;
    }
}
