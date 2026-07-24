package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GroupDefinitionMapping extends ItemMapping {
    private String model;

    private final List<ItemMapping> definitions = new ArrayList<>();

    public GroupDefinitionMapping(@NotNull Material javaMaterial, @NotNull String bedrockIdentifier) {
        super(javaMaterial, bedrockIdentifier);
    }

    public String getModel() {
        return this.model;
    }

    public GroupDefinitionMapping setModel(@Nullable String model) {
        this.model = model;
        return this;
    }

    public List<ItemMapping> getDefinitions() {
        return this.definitions;
    }

    public boolean hasDefinitions() {
        return !this.definitions.isEmpty();
    }

    public GroupDefinitionMapping addDefinition(@NotNull ItemMapping definition) {
        this.definitions.add(definition);
        return this;
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = super.serialize();

        if (this.model != null) {
            jsonObject.addProperty("model", this.model);
        }

        jsonObject.addProperty("type", "group");

        if (!this.definitions.isEmpty()) {
            jsonObject.add("definitions", this.definitions.stream().map(ItemMapping::serialize).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        }

        return jsonObject;
    }
}
